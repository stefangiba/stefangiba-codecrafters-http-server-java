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
      InputStream is = clientSocket.getInputStream();
      String request = readRequest(is);
      System.out.println(request);

      OutputStream os = clientSocket.getOutputStream();
      os.write(HTTP_OK.getBytes());
      os.flush();

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
    Reader in = new InputStreamReader(is, StandardCharsets.UTF_8);

    for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0;) {
      out.append(buffer, 0, numRead);
    }

    return out.toString();
  }
}
