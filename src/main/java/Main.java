import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
  private static final String PATH_ECHO = "/echo";
  private static final String PATH_USER_AGENT = "/user-agent";
  private static final String PATH_FILES = "/files";

  private static final String HTTP_OK = "HTTP/1.1 200 OK\r\n\r\n";
  private static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found\r\n\r\n";
  private static final String HTTP_RESPONSE = "HTTP/1.1 200 OK\r\nContent-type: %s\r\nContent-Length: %d\r\n\r\n%s";
  private static final String HTTP_CREATED = "HTTP/1.1 201 Created\r\n\r\n";

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

            String response = switch (request.getPath()) {
              case "/" -> HTTP_OK;
              case String path when path.startsWith(PATH_ECHO) -> buildTextResponse(path.split("/")[2]);
              case String path when path.startsWith(PATH_USER_AGENT) ->
                buildTextResponse(request.getHeader("User-Agent"));
              case String path when path.startsWith(PATH_FILES) -> {
                if (directoryPathOpt.isEmpty()) {
                  yield HTTP_NOT_FOUND;
                }

                var directoryPath = directoryPathOpt.get();

                yield switch (request.getHttpMethod()) {
                  case GET -> retrieveFile(directoryPath, path.substring(7));
                  case POST -> writeToFileString(directoryPath, path.substring(7), request.getBody());
                  default ->
                    HTTP_NOT_FOUND;
                };
              }
              default -> HTTP_NOT_FOUND;
            };

            outputStream.write(response.getBytes());
          } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
          }
        });
      }
    }
  }

  private static String retrieveFile(Path directoryPath, String filePathStr) {
    try {
      var filePath = directoryPath.resolve(filePathStr);
      byte[] fileContent = Files.readAllBytes(filePath);

      return buildResponse(new String(fileContent, StandardCharsets.UTF_8), "application/octet-stream",
          fileContent.length);
    } catch (IOException e) {
      System.err.println(e.getMessage());

      return HTTP_NOT_FOUND;
    }
  }

  private static String writeToFileString(Path directoryPath, String filePathStr, String content) {
    try {
      var filePath = directoryPath.resolve(filePathStr);
      Files.write(filePath, content.getBytes());

      return HTTP_CREATED;
    } catch (IOException e) {
      System.err.println(e.getMessage());

      return HTTP_NOT_FOUND;
    }
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

  private static String buildTextResponse(String content) {
    return buildResponse(content, "text/plain", -1);
  }

  private static String buildResponse(String content, String contentType, int contentLength) {
    return String.format(HTTP_RESPONSE, contentType, contentLength == -1 ? content.length() : contentLength, content);
  }
}
