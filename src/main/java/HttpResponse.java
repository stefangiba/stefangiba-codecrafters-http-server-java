import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

final class HttpResponse {
    private static final HttpResponse NOT_FOUND_HTTP_1_1 = new HttpResponse(HttpVersion.HTTP_1_1,
            HttpStatusCode.NOT_FOUND);
    private static final HttpResponse NOT_FOUND_HTTP_1_0 = new HttpResponse(HttpVersion.HTTP_1_0,
            HttpStatusCode.NOT_FOUND);
    private static final HttpResponse OK_HTTP_1_1 = new HttpResponse(HttpVersion.HTTP_1_1, HttpStatusCode.OK);
    private static final HttpResponse OK_HTTP_1_0 = new HttpResponse(HttpVersion.HTTP_1_0, HttpStatusCode.OK);
    private static final HttpResponse CREATED_HTTP_1_1 = new HttpResponse(HttpVersion.HTTP_1_1, HttpStatusCode.CREATED);
    private static final HttpResponse CREATED_HTTP_1_0 = new HttpResponse(HttpVersion.HTTP_1_0, HttpStatusCode.CREATED);

    private static final String NEW_LINE = "\r\n";

    private final HttpVersion httpVersion;
    private final HttpStatusCode statusCode;
    private final Map<String, String> headers;
    private final Optional<byte[]> responseBody;

    HttpResponse(HttpVersion httpVersion, HttpStatusCode statusCode) {
        this(httpVersion, statusCode, Collections.emptyMap(), null);
    }

    HttpResponse(HttpVersion httpVersion, HttpStatusCode statusCode, Map<String, String> headers) {
        this(httpVersion, statusCode, headers, null);
    }

    HttpResponse(HttpVersion httpVersion, HttpStatusCode statusCode, Map<String, String> headers, byte[] responseBody) {
        this.httpVersion = httpVersion;
        this.statusCode = statusCode;
        this.headers = Collections.unmodifiableMap(headers);
        this.responseBody = Optional.ofNullable(responseBody);
    }

    void send(OutputStream outputStream) throws IOException {
        var sb = new StringBuilder();
        writeRequestLine(sb);
        writeHeaders(sb);

        outputStream.write(sb.toString().getBytes());

        if (responseBody.isPresent()) {
            outputStream.write(responseBody.get());
        }
    }

    private void writeRequestLine(StringBuilder sb) {
        sb.append(httpVersion.toString());
        sb.append(' ');
        sb.append(statusCode.getCode());
        sb.append(' ');
        sb.append(statusCode.toString());
        sb.append(NEW_LINE);
    }

    private void writeHeaders(StringBuilder sb) {
        headers.forEach((headerName, value) -> {
            sb.append(headerName);
            sb.append(':');
            sb.append(' ');
            sb.append(value);
            sb.append(NEW_LINE);
        });

        sb.append(NEW_LINE);
    }

    static HttpResponse notFound() {
        return NOT_FOUND_HTTP_1_1;
    }

    static HttpResponse notFound(HttpVersion httpVersion) {
        return switch (httpVersion) {
            case HTTP_1_0 -> NOT_FOUND_HTTP_1_0;
            case HTTP_1_1 -> NOT_FOUND_HTTP_1_1;
        };
    }

    static HttpResponse created() {
        return CREATED_HTTP_1_1;
    }

    static HttpResponse created(HttpVersion httpVersion) {
        return switch (httpVersion) {
            case HTTP_1_0 -> CREATED_HTTP_1_0;
            case HTTP_1_1 -> CREATED_HTTP_1_1;
        };
    }

    static HttpResponse ok() {
        return OK_HTTP_1_1;
    }

    static HttpResponse ok(HttpVersion httpVersion) {
        return switch (httpVersion) {
            case HTTP_1_0 -> OK_HTTP_1_0;
            case HTTP_1_1 -> OK_HTTP_1_1;
        };
    }
}
