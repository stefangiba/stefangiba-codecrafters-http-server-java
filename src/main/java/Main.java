import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

public class Main {
  private static final String HTTP_OK = "HTTP/1.1 200 OK\r\n\r\n";
  private static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found\r\n\r\n";

  public static void main(String[] args) throws InterruptedException {
    try (
        ServerSocket serverSocket = new ServerSocket(4221);
        Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
        InputStream inputStream = clientSocket.getInputStream();
        OutputStream outputStream = clientSocket.getOutputStream()) {
      serverSocket.setReuseAddress(true);

      System.out.println("accepted new connection");
      // handle incoming connection
      Request request = Request.readFrom(inputStream);
      System.out.println(request.getPath());
      System.out.println(request.getHttpMethod());
      System.out.println(request.getHttpVersion());

      System.out.println("sending response...");
      if ("/".equals(request.getPath())) {
        outputStream.write(HTTP_OK.getBytes());
      } else {
        outputStream.write(HTTP_NOT_FOUND.getBytes());
      }

      System.out.println("wrote bytes to socket");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
