public enum HttpMethod {
  GET("GET"),
  POST("POST"),
  PUT("PUT"),
  DELETE("DELETE"),
  UPDATE("UPDATE");

  private final String name;

  HttpMethod(String name) {
    this.name = name;
  }

  public String toString() {
    return name;
  }
}
