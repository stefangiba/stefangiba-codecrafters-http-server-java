enum HttpStatusCode {
    OK(200),
    CREATED(201),
    NOT_FOUND(404);

    private final int code;

    HttpStatusCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String toString() {
        return switch (this) {
            case CREATED -> "Created";
            case NOT_FOUND -> "Not Found";
            case OK -> "OK";
        };
    }
}
