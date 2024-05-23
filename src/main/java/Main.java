import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  private static final String HTTP_OK = "HTTP/1.1 200 OK\r\n\r\n";
  private static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found\r\n\r\n";
  private static final String HTTP_RESPONSE = "HTTP/1.1 200 OK\r\nContent-type: %s\r\nContent-Length: %d\r\n\r\n%s";

  public static void main(String[] args) throws InterruptedException {
    try (
        ServerSocket serverSocket = new ServerSocket(4221);
        Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
        InputStream inputStream = clientSocket.getInputStream();
        OutputStream outputStream = clientSocket.getOutputStream()) {
      serverSocket.setReuseAddress(true);

      // handle incoming connection
      HttpRequest request = HttpRequest.readFrom(inputStream);

      String response = switch (request.getPath()) {
        case "/" -> HTTP_OK;
        case String path when path.startsWith("/echo") -> buildTextResponse(path.split("/")[2]);
        case String path when path.startsWith("/user-agent") ->
          buildTextResponse(request.getHeaders().get("User-Agent"));
        default -> HTTP_NOT_FOUND;
      };

      outputStream.write(response.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String buildTextResponse(String content) {
    return String.format(HTTP_RESPONSE, "text/plain", content.length(), content);
  }
}
