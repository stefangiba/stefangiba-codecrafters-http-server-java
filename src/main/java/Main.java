import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
  private static final String HTTP_OK = "HTTP/1.1 200 OK\r\n\r\n";

  public static void main(String[] args) {
    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      clientSocket = serverSocket.accept(); // Wait for connection from client.

      System.out.println("accepted new connection");
      // handle incoming connection
      // var inputStream = clientSocket.getInputStream();
      // var request = readRequest(inputStream);
      // System.out.println(request);

      System.out.println("sending response...");
      try (var outputStream = clientSocket.getOutputStream()) {
        System.out.println("writing bytes to socket...");
        outputStream.write(HTTP_OK.getBytes());
        System.out.println("wrote bytes to socket");
        outputStream.flush();
        outputStream.close();
      }

      serverSocket.close();
      clientSocket.close();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  private static String readRequest(InputStream is) throws IOException {
    int bufferSize = 1024;
    char[] buffer = new char[bufferSize];
    StringBuilder out = new StringBuilder();

    System.out.println("Reading input stream...");

    try (Reader in = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      for (int numRead = 0; (numRead = in.read(buffer)) > 0;) {
        System.out.println(numRead);
        out.append(buffer, 0, numRead);
      }
    }

    return out.toString();
  }
}
