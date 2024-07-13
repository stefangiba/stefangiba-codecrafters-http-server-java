import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

  public static void main(String[] args) throws InterruptedException, IOException {
    Optional<Path> directoryPath = getDirectoryPath(args);

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
                try {
                  if (directoryPath.isEmpty()) {
                    yield HTTP_NOT_FOUND;
                  }

                  var filePath = directoryPath.get().resolve(path.substring(7));
                  byte[] fileContent = Files.readAllBytes(filePath);

                  yield buildResponse(new String(fileContent, StandardCharsets.UTF_8), "application/octet-stream",
                      fileContent.length);
                } catch (IOException e) {
                  System.err.println(e.getMessage());

                  yield HTTP_NOT_FOUND;
                }
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
