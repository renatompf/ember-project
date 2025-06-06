package io.github.renatompf.ember.core.parameter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;
import io.github.renatompf.ember.exceptions.HttpException;

/**
 * BodyManager is responsible for parsing the request body based on its content type.
 * It supports JSON, XML, and plain text formats.
 */
public class BodyManager {
    private final String body;
    private final String contentType;
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final XmlMapper xmlMapper = new XmlMapper();

    /**
     * Constructs a BodyManager with the given body and content type.
     *
     * @param body        The request body as a string.
     * @param contentType The content type of the request body.
     */
    public BodyManager(String body, String contentType) {
        this.body = body;
        this.contentType = contentType;
    }

    /**
     * Parses the request body into an object of the specified class type.
     *
     * @param <T>   The type of the object to parse the body into.
     * @param clazz The class type to parse the body into.
     * @return An instance of the specified class type populated with data from the request body.
     * @throws HttpException If the content type is unsupported or the body cannot be parsed.
     */
    public <T> T parseBodyAs(Class<T> clazz) {
        try {
            if (contentType == null || contentType.equals(MediaType.APPLICATION_JSON.getType())) {
                return jsonMapper.readValue(body, clazz);
            } else if (contentType.contains(MediaType.APPLICATION_XML.getType())) {
                return xmlMapper.readValue(body, clazz);
            } else if (contentType.equals(MediaType.TEXT_PLAIN.getType())) {
                return clazz.getConstructor(String.class).newInstance(body);
            }
            throw new HttpException(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE, "Unsupported Content-Type: " + contentType);
        } catch (Exception e) {
            throw new HttpException(HttpStatusCode.BAD_REQUEST, "Failed to parse request body: " + e.getMessage());
        }
    }
}