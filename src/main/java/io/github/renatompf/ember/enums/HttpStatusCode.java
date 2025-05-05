package io.github.renatompf.ember.enums;

/**
 * Enum representing common HTTP status codes and their corresponding messages.
 * <p>
 * This enum provides a set of standard HTTP status codes that can be used in web applications.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * HttpStatusCode status = HttpStatusCode.OK;
 * System.out.println(status.getCode()); // Output: 200
 * System.out.println(status.getMessage()); // Output: OK
 * }
 * </pre>
 */
public enum HttpStatusCode {

    /**
     * HTTP 100 Continue.
     * <p>
     * The server has received the request headers, and the client should proceed to send the request body.
     * </p>
     */
    CONTINUE(100, "Continue"),

    /**
     * HTTP 101 Switching Protocols.
     * <p>
     * The requester has asked the server to switch protocols, and the server has agreed to do so.
     * </p>
     */
    SWITCHING_PROTOCOLS(101, "Switching Protocols"),

    /**
     * HTTP 102 Processing.
     * <p>
     * The server has received and is processing the request, but no response is available yet.
     * </p>
     */
    PROCESSING(102, "Processing"),

    /**
     * HTTP 103 Early Hints.
     * <p>
     * Used to return some response headers before the final HTTP message.
     * </p>
     */
    EARLY_HINTS(103, "Early Hints"),

    /**
     * HTTP 200 OK.
     * <p>
     * The request has succeeded.
     * </p>
     */
    OK(200, "OK"),

    /**
     * HTTP 201 Created.
     * <p>
     * The request has been fulfilled and resulted in a new resource being created.
     * </p>
     */
    CREATED(201, "Created"),

    /**
     * HTTP 202 Accepted.
     * <p>
     * The request has been accepted for processing, but the processing has not been completed.
     * </p>
     */
    ACCEPTED(202, "Accepted"),

    /**
     * HTTP 203 Non-Authoritative Information.
     * <p>
     * The server successfully processed the request, but is returning information that may be from another source.
     * </p>
     */
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),

    /**
     * HTTP 204 No Content.
     * <p>
     * The server successfully processed the request, but is not returning any content.
     * </p>
     */
    NO_CONTENT(204, "No Content"),

    /**
     * HTTP 205 Reset Content.
     * <p>
     * The server successfully processed the request, but requires the requester to reset the document view.
     * </p>
     */
    RESET_CONTENT(205, "Reset Content"),

    /**
     * HTTP 206 Partial Content.
     * <p>
     * The server is delivering only part of the resource due to a range header sent by the client.
     * </p>
     */
    PARTIAL_CONTENT(206, "Partial Content"),

    /**
     * HTTP 301 Moved Permanently.
     * <p>
     * The requested resource has been assigned a new permanent URI.
     * </p>
     */
    MOVED_PERMANENTLY(301, "Moved Permanently"),

    /**
     * HTTP 302 Found.
     * <p>
     * The requested resource resides temporarily under a different URI.
     * </p>
     */
    FOUND(302, "Found"),

    /**
     * HTTP 304 Not Modified.
     * <p>
     * Indicates that the resource has not been modified since the version specified by the request headers.
     * </p>
     */
    NOT_MODIFIED(304, "Not Modified"),

    /**
     * HTTP 400 Bad Request.
     * <p>
     * The server could not understand the request due to invalid syntax.
     * </p>
     */
    BAD_REQUEST(400, "Bad Request"),

    /**
     * HTTP 401 Unauthorized.
     * <p>
     * The client must authenticate itself to get the requested response.
     * </p>
     */
    UNAUTHORIZED(401, "Unauthorized"),

    /**
     * HTTP 403 Forbidden.
     * <p>
     * The client does not have access rights to the content.
     * </p>
     */
    FORBIDDEN(403, "Forbidden"),

    /**
     * HTTP 404 Not Found.
     * <p>
     * The server can not find the requested resource.
     * </p>
     */
    NOT_FOUND(404, "Not Found"),

    /**
     * HTTP 405 Method Not Allowed.
     * <p>
     * The request method is known by the server but is not supported by the target resource.
     * </p>
     */
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),

    /**
     * HTTP 415 Unsupported Media Type.
     * <p>
     * The media format of the requested data is not supported by the server.
     * </p>
     */
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),

    /**
     * HTTP 500 Internal Server Error.
     * <p>
     * The server has encountered a situation it doesn't know how to handle.
     * </p>
     */
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    /**
     * The numerical HTTP status code.
     */
    private final int code;

    /**
     * The textual description of the HTTP status code.
     */
    private final String message;

    /**
     * Constructor to initialize the HTTP status code and its message.
     *
     * @param code    The numerical HTTP status code.
     * @param message The textual description of the HTTP status code.
     */
    HttpStatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Retrieves the numerical HTTP status code.
     *
     * @return The numerical HTTP status code.
     */
    public int getCode() {
        return code;
    }

    /**
     * Retrieves the textual description of the HTTP status code.
     *
     * @return The textual description of the HTTP status code.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Retrieves the HttpStatusCode enum corresponding to the given numerical code.
     *
     * @param code The numerical HTTP status code.
     * @return The corresponding HttpStatusCode enum, or null if no matching code is found.
     */
    public static HttpStatusCode fromCode(int code) {
        for (HttpStatusCode responseCode : values()) {
            if (responseCode.getCode() == code) {
                return responseCode;
            }
        }
        return null;
    }

    /**
     * Retrieves the HttpStatusCode enum corresponding to the given message.
     *
     * @param message The textual description of the HTTP status code.
     * @return The corresponding HttpStatusCode enum, or null if no matching message is found.
     */
    public static HttpStatusCode fromMessage(String message) {
        for (HttpStatusCode responseCode : values()) {
            if (responseCode.getMessage().equalsIgnoreCase(message)) {
                return responseCode;
            }
        }
        return null;
    }
}
