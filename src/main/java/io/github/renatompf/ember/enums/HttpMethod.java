package io.github.renatompf.ember.enums;

public enum HttpMethod {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    PATCH("PATCH"),
    OPTIONS("OPTIONS"),
    HEAD("HEAD");

    private final String method;

    private static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"};

    HttpMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public static boolean isAllowedMethod(String method) {
        for (String allowedMethod : ALLOWED_METHODS) {
            if (allowedMethod.equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }

    public static HttpMethod fromString(String method) {
        for (HttpMethod httpMethod : HttpMethod.values()) {
            if (httpMethod.method.equalsIgnoreCase(method)) {
                return httpMethod;
            }
        }
        throw new IllegalArgumentException("Invalid HTTP method: " + method);
    }

}
