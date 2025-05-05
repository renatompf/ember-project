package io.github.renatompf.ember.enums;

/**
 * Enum representing common HTTP request headers.
 * <p>
 * This enum provides a set of standard HTTP headers that can be used in HTTP requests.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * RequestHeader header = RequestHeader.ACCEPT;
 * System.out.println(header.getHeaderName()); // Output: Accept
 * }
 * </pre>
 */
public enum RequestHeader {

    /**
     * The Accept request header.
     */
    ACCEPT("Accept"),

    /**
     * The Accept-Charset request header.
     */
    ACCEPT_CHARSET("Accept-Charset"),

    /**
     * The Accept-Encoding request header.
     */
    ACCEPT_ENCODING("Accept-Encoding"),

    /**
     * The Accept-Language request header.
     */
    ACCEPT_LANGUAGE("Accept-Language"),

    /**
     * The Authorization request header.
     */
    AUTHORIZATION("Authorization"),

    /**
     * The Cache-Control request header.
     */
    CACHE_CONTROL("Cache-Control"),

    /**
     * The Connection request header.
     */
    CONNECTION("Connection"),

    /**
     * The Content-Length request header.
     */
    CONTENT_LENGTH("Content-Length"),

    /**
     * The Content-Type request header.
     */
    CONTENT_TYPE("Content-Type"),

    /**
     * The Cookie request header.
     */
    COOKIE("Cookie"),

    /**
     * The Host request header.
     */
    HOST("Host"),

    /**
     * The Referer request header.
     */
    REFERER("Referer"),

    /**
     * The User-Agent request header.
     */
    USER_AGENT("User-Agent");

    /**
     * The name of the HTTP request header.
     */
    private final String headerName;

    /**
     * Constructor to initialize the HTTP request header name.
     *
     * @param headerName The name of the HTTP request header.
     */
    RequestHeader(String headerName) {
        this.headerName = headerName;
    }

    /**
     * Retrieves the name of the HTTP request header.
     *
     * @return The name of the HTTP request header.
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * Returns the string representation of the HTTP request header.
     *
     * @return The string representation of the HTTP request header.
     */
    @Override
    public String toString() {
        return headerName;
    }
}