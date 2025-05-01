package io.github.renatompf.ember.enums;

public enum HttpStatusCode {
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NO_CONTENT(204, "No Content"),
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),
    NOT_MODIFIED(304, "Not Modified"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private final int code;
    private final String message;

    HttpStatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static HttpStatusCode fromCode(int code) {
        for (HttpStatusCode responseCode : values()) {
            if (responseCode.getCode() == code) {
                return responseCode;
            }
        }
        return null;
    }

    public static HttpStatusCode fromMessage(String message) {
        for (HttpStatusCode responseCode : values()) {
            if (responseCode.getMessage().equalsIgnoreCase(message)) {
                return responseCode;
            }
        }
        return null;
    }
}
