package io.github.renatompf.ember.enums;


/**
 * Enum representing common media types used in HTTP communication.
 * <p>
 * This enum provides a set of standard media types that can be used to specify
 * the format of the data being sent or received in HTTP requests and responses.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * MediaType mediaType = MediaType.APPLICATION_JSON;
 * System.out.println(mediaType.getType()); // Output: application/json
 * }
 * </pre>
 */
public enum MediaType {

    /**
     * Media type for JSON data.
     */
    APPLICATION_JSON("application/json"),

    /**
     * Media type for XML data.
     */
    APPLICATION_XML("application/xml"),

    /**
     * Media type for form data encoded as key-value pairs.
     */
    APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded"),

    /**
     * Media type for plain text data.
     */
    TEXT_PLAIN("text/plain"),

    /**
     * Media type for HTML data.
     */
    TEXT_HTML("text/html"),

    /**
     * Media type for multipart form data, often used for file uploads.
     */
    MULTIPART_FORM_DATA("multipart/form-data"),

    /**
     * Media type for binary data.
     */
    OCTET_STREAM("application/octet-stream"),

    ;

    /**
     * The string representation of the media type.
     */
    private final String type;

    /**
     * Constructor to initialize the media type.
     *
     * @param type The string representation of the media type.
     */
    MediaType(String type) {
        this.type = type;
    }

    /**
     * Retrieves the string representation of the media type.
     *
     * @return The string representation of the media type.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the string representation of the media type.
     *
     * @return The string representation of the media type.
     */
    @Override
    public String toString() {
        return type;
    }
}