import java.util.Collections;
import java.util.Map;
import java.util.Optional;

final class HttpResponse {
    private static final String NEW_LINE = "\r\n";

    private final HttpVersion httpVersion;
    private final HttpStatusCode statusCode;
    private final Map<String, String> headers;
    private final Optional<String> responseBody;

    HttpResponse(HttpVersion httpVersion, HttpStatusCode statusCode) {
        this(httpVersion, statusCode, Collections.emptyMap(), null);
    }

    HttpResponse(HttpVersion httpVersion, HttpStatusCode statusCode, Map<String, String> headers) {
        this(httpVersion, statusCode, headers, null);
    }

    HttpResponse(HttpVersion httpVersion, HttpStatusCode statusCode, Map<String, String> headers, String responseBody) {
        this.httpVersion = httpVersion;
        this.statusCode = statusCode;
        this.headers = Collections.unmodifiableMap(headers);
        this.responseBody = Optional.ofNullable(responseBody);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(httpVersion.toString());
        sb.append(' ');
        sb.append(statusCode.getCode());
        sb.append(' ');
        sb.append(statusCode.toString());
        sb.append(NEW_LINE);

        headers.forEach((headerName, value) -> {
            sb.append(headerName);
            sb.append(':');
            sb.append(' ');
            sb.append(value);
            sb.append(NEW_LINE);
        });

        sb.append(NEW_LINE);

        if (responseBody.isPresent()) {
            sb.append(responseBody.get());
        }

        return sb.toString();
    }

    static HttpResponse notFound() {
        return notFound(HttpVersion.HTTP_1_1);
    }

    static HttpResponse notFound(HttpVersion httpVersion) {
        return new HttpResponse(httpVersion, HttpStatusCode.NOT_FOUND);
    }

    static HttpResponse created() {
        return created(HttpVersion.HTTP_1_1);
    }

    static HttpResponse created(HttpVersion httpVersion) {
        return new HttpResponse(httpVersion, HttpStatusCode.CREATED);
    }

    static HttpResponse ok() {
        return ok(HttpVersion.HTTP_1_1);
    }

    static HttpResponse ok(HttpVersion httpVersion) {
        return new HttpResponse(httpVersion, HttpStatusCode.OK);
    }
}
