package io.ember.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.ember.enums.HttpStatusCode;
import io.ember.enums.MediaType;
import io.ember.enums.RequestHeader;
import io.ember.exceptions.HttpException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class BodyManager {
    private final String body;
    private final String contentType;
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final XmlMapper xmlMapper = new XmlMapper();

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