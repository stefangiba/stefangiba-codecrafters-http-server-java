import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class HttpRequest {
  private final HttpMethod httpMethod;
  private final HttpVersion httpVersion;
  private final String path;
  private final Map<String, String> headers;
  private final String body;

  private HttpRequest(
      HttpMethod httpMethod,
      HttpVersion httpVersion,
      String path,
      Map<String, String> headers,
      String body) {
    this.httpMethod = httpMethod;
    this.httpVersion = httpVersion;
    this.path = path;
    this.headers = headers;
    this.body = body;
  }

  public HttpMethod getHttpMethod() {
    return httpMethod;
  }

  public HttpVersion getHttpVersion() {
    return httpVersion;
  }

  public String getPath() {
    return path;
  }

  public String getHeader(String headerName) {
    return headers.get(headerName);
  }

  public String getBody() {
    return body;
  }

  public static HttpRequest readFrom(InputStream is) throws IllegalArgumentException, IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

    String requestLine = reader.readLine();

    if (requestLine == null) {
      throw new IllegalArgumentException("Request line is null");
    }

    String[] parts = requestLine.split(" ");

    if (parts.length != 3) {
      throw new IllegalArgumentException("Request line is malformed");
    }

    HttpMethod httpMethod = HttpMethod.valueOf(parts[0]);
    HttpVersion httpVersion = HttpVersion.parse(parts[2]);
    String path = parts[1];

    Map<String, String> headers = readHeaders(reader);

    int contentLength = 0;
    if (headers.containsKey("Content-Length")) {
      contentLength = Integer.parseInt(headers.get("Content-Length"));
    }

    return new HttpRequest(httpMethod, httpVersion, path, headers, readBody(reader, contentLength));
  }

  private static final Map<String, String> readHeaders(BufferedReader reader) throws IOException {
    Map<String, String> headers = new HashMap<>();
    String headerLine;
    while (!(headerLine = reader.readLine()).isEmpty()) {
      String[] headerParts = headerLine.split(": ");

      if (headerParts.length == 2) {
        headers.put(headerParts[0], headerParts[1]);
      }
    }

    return Collections.unmodifiableMap(headers);
  }

  private static final String readBody(BufferedReader reader, int contentLength) throws IOException {
    StringBuilder body = new StringBuilder();
    if (contentLength > 0) {
      char[] bodyChars = new char[contentLength];
      reader.read(bodyChars, 0, contentLength);
      body.append(bodyChars);
    }

    return body.toString();
  }
}
