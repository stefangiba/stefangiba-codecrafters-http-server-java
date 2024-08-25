package com.stefangiba.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

import com.stefangiba.http.model.HttpRequest;
import com.stefangiba.http.model.HttpResponse;
import com.stefangiba.http.model.HttpStatusCode;
import com.stefangiba.http.model.HttpVersion;

public class Main {
    private static final String[] SUPPORTED_ENCODINGS = { "gzip" };

    private static final String PATH_ECHO = "/echo";
    private static final String PATH_USER_AGENT = "/user-agent";
    private static final String PATH_FILES = "/files";

    public static void main(String[] args) throws InterruptedException, IOException {
        Optional<Path> directoryPathOpt = getDirectoryPath(args);

        try (ServerSocket serverSocket = new ServerSocket(4221);
                ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.

                executorService.submit(() -> {
                    try (
                            InputStream inputStream = clientSocket.getInputStream();
                            OutputStream outputStream = clientSocket.getOutputStream()) {
                        serverSocket.setReuseAddress(true);

                        // handle incoming connection
                        HttpRequest request = HttpRequest.readFrom(inputStream);

                        HttpResponse response = switch (request.getPath()) {
                            case "/" -> HttpResponse.ok();
                            case String path when path.startsWith(PATH_ECHO) ->
                                buildTextResponse(path.split("/")[2], request);
                            case String path when path.startsWith(PATH_USER_AGENT) ->
                                buildTextResponse(request.getHeader("User-Agent"), request);
                            case String path when path.startsWith(PATH_FILES) -> {
                                if (directoryPathOpt.isEmpty()) {
                                    yield HttpResponse.notFound();
                                }

                                var directoryPath = directoryPathOpt.get();

                                yield switch (request.getHttpMethod()) {
                                    case GET -> retrieveFile(directoryPath, path.substring(7), request);
                                    case POST ->
                                        writeRequestBodyToFile(directoryPath, path.substring(7), request.getBody());
                                    default -> HttpResponse.notFound();
                                };
                            }
                            default -> HttpResponse.notFound();
                        };

                        response.send(outputStream);
                        outputStream.flush();
                    } catch (IOException | IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    private static HttpResponse retrieveFile(Path directoryPath, String filePathStr, HttpRequest request) {
        try {
            var filePath = directoryPath.resolve(filePathStr);
            byte[] fileContent = Files.readAllBytes(filePath);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/octet-stream");

            var supportedEncoding = getSupportedEncoding(request);
            if (supportedEncoding.isPresent()) {
                headers.put("Content-Encoding", supportedEncoding.get());
                fileContent = zip(fileContent);
            }

            headers.put("Content-Length", Integer.toString(fileContent.length));

            return new HttpResponse(HttpVersion.HTTP_1_1, HttpStatusCode.OK, headers, fileContent);
        } catch (IOException e) {
            System.err.println(e);

            return HttpResponse.notFound();
        }
    }

    private static HttpResponse writeRequestBodyToFile(Path directoryPath, String filePathStr, String content) {
        try {
            var filePath = directoryPath.resolve(filePathStr);
            Files.write(filePath, content.getBytes());

            return HttpResponse.created();
        } catch (IOException e) {
            System.err.println(e.getMessage());

            return HttpResponse.notFound();
        }
    }

    private static HttpResponse buildTextResponse(String responseContent, HttpRequest request) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");

        var supportedEncoding = getSupportedEncoding(request);
        var contentBytes = responseContent.getBytes(StandardCharsets.UTF_8);
        if (supportedEncoding.isPresent()) {
            headers.put("Content-Encoding", supportedEncoding.get());
            contentBytes = zip(contentBytes);
        }

        headers.put("Content-Length", Integer.toString(contentBytes.length));

        return new HttpResponse(HttpVersion.HTTP_1_1, HttpStatusCode.OK, headers, contentBytes);
    }

    private static final byte[] zip(byte[] bytes) throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();

        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(bytes);
        }

        return byteArrayOutputStream.toByteArray();
    }

    private static Optional<String> getSupportedEncoding(HttpRequest request) {
        var acceptedEncodings = request.getHeader("Accept-Encoding");

        if (acceptedEncodings == null) {
            return Optional.empty();
        }

        for (String encoding : acceptedEncodings.split(", ")) {
            if (isSupportedEncoding(encoding.toLowerCase(Locale.ROOT))) {
                return Optional.of(encoding);
            }
        }

        return Optional.empty();
    }

    private static boolean isSupportedEncoding(String encoding) {
        for (String supportedEncoding : SUPPORTED_ENCODINGS) {
            if (supportedEncoding.equals(encoding)) {
                return true;
            }
        }

        return false;
    }

    private static Optional<Path> getDirectoryPath(String[] args) {
        if (args.length != 2 || !"--directory".equals(args[0])) {
            return Optional.empty();
        }

        var path = Paths.get(args[1]);

        if (!path.isAbsolute()) {
            return Optional.empty();
        }

        return Optional.of(path);
    }
}
