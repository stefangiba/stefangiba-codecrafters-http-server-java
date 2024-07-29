enum ContentType {
    TEXT_PLAIN("text/plain"),
    APPLICATION_OCTET_STREAM("application/octet-stream");

    private final String contentType;

    ContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String toString() {
        return contentType;
    }
}
