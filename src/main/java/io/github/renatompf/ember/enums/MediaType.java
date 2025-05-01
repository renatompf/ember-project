package io.github.renatompf.ember.enums;

public enum MediaType {
    APPLICATION_JSON("application/json"),
    APPLICATION_XML("application/xml"),
    APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded"),
    TEXT_PLAIN("text/plain"),
    TEXT_HTML("text/html"),
    MULTIPART_FORM_DATA("multipart/form-data"),
    OCTET_STREAM("application/octet-stream"),

    ;

    private final String type;

    MediaType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }
}