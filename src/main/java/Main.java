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

      System.out.println("accepted new connection");
      // handle incoming connection
      HttpRequest request = HttpRequest.readFrom(inputStream);

      String response = switch (request.getPath()) {
        case "/" -> HTTP_OK;
        case String s when s.startsWith("/echo") -> buildEchoResponse(s);
        default -> HTTP_NOT_FOUND;
      };

      System.out.println("sending response...");
      outputStream.write(response.getBytes());

      System.out.println("wrote bytes to socket");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String buildEchoResponse(String path) {
    String[] parts = path.split("/");
    String param = parts[2];
    return String.format(HTTP_RESPONSE, "text/plain", param.length(), param);
  }
}
