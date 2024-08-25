package com.stefangiba.http.model;

public enum HttpVersion {
    HTTP_1_0("HTTP/1.0"),
    HTTP_1_1("HTTP/1.1");

    private final String name;

    HttpVersion(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static HttpVersion parse(String name) {
        if (HTTP_1_0.name.equals(name)) {
            return HTTP_1_0;
        } else if (HTTP_1_1.name.equals(name)) {
            return HTTP_1_1;
        } else {
            throw new IllegalArgumentException("Unknown HTTP version: " + name);
        }
    }
}
