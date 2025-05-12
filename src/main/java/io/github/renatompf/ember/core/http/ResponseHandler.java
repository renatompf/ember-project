package io.github.renatompf.ember.core.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;
import io.github.renatompf.ember.enums.RequestHeader;
import io.github.renatompf.ember.exceptions.HttpException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles HTTP responses for the Ember framework.
 * <p>
 * This class is responsible for sending JSON and plain text responses to the client.
 * It uses Jackson for JSON serialization and manages HTTP headers appropriately.
 * </p>
 */
public class ResponseHandler {
    private final HttpExchange exchange;
    private final Map<MediaType, ResponseSerializer> serializers;

    private static final ObjectMapper jsonMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // XML mapper for XML serialization
    private static final XmlMapper xmlMapper = new XmlMapper();

    /**
     * Constructor for ResponseHandler.
     *
     * @param exchange The HttpExchange object representing the HTTP request and response.
     */
    public ResponseHandler(HttpExchange exchange) {
        this.exchange = exchange;
        this.serializers = initializeSerializers();
    }

    /**
     * Initializes the default serializers for various media types.
     *
     * @return A map of media types to their corresponding serializers.
     */
    private Map<MediaType, ResponseSerializer> initializeSerializers() {
        Map<MediaType, ResponseSerializer> map = new HashMap<>();

        // JSON Serializer
        map.put(MediaType.APPLICATION_JSON, jsonMapper::writeValueAsString);

        // XML Serializer
        map.put(MediaType.APPLICATION_XML, xmlMapper::writeValueAsString);

        // Plain Text Serializer
        map.put(MediaType.TEXT_PLAIN, Object::toString);

        return map;
    }

    /**
     * Registers a custom serializer for a specific media type.
     *
     * @param mediaType The media type to register the serializer for.
     * @param serializer The serializer to use for the specified media type.
     */
    private void registerSerializer(MediaType mediaType, ResponseSerializer serializer) {
        serializers.put(mediaType, serializer);
    }

    /**
     * Registers a custom serializer for a specific media type.
     *
     * @param mediaType The media type to register the serializer for.
     * @param serializer The serializer to use for the specified media type.
     */
    public void registerCustomSerializer(String mediaType, ResponseSerializer serializer) {
        MediaType type = MediaType.fromString(mediaType);
        if (type == null) {
            throw new IllegalArgumentException("Invalid media type: " + mediaType);
        }

        if (serializers.containsKey(type)) {
            throw new IllegalArgumentException("Media type already registered: " + mediaType);
        }

        registerSerializer(type, serializer);
    }

    /**
     * Handles the HTTP response by serializing the response body and sending it to the client.
     *
     * @param response The Response object containing the status code, headers, and body.
     * @param <T>      The type of the response body.
     */
    public <T> void handleResponse(Response<T> response) {
        try {

            if (response.getBody() == null) {
                exchange.sendResponseHeaders(response.getStatusCode().getCode(), -1);
                exchange.getResponseBody().close();
                return;
            }

            String contentType = response.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                String first = exchange.getResponseHeaders().getFirst(RequestHeader.ACCEPT.getHeaderName());
                contentType = first != null ? first : MediaType.APPLICATION_JSON.getType();
            }

            MediaType mediaType = MediaType.fromString(contentType);
            if (mediaType == null) {
                mediaType = MediaType.APPLICATION_JSON; // default
            }

            ResponseSerializer serializer = serializers.get(mediaType);
            if (serializer == null) {
                throw new HttpException(
                        HttpStatusCode.UNSUPPORTED_MEDIA_TYPE,
                        "Unsupported media type: " + mediaType
                );
            }

            String serializedResponse = serializer.serialize(response.getBody());
            exchange.getResponseHeaders().set(
                    RequestHeader.CONTENT_TYPE.getHeaderName(),
                    mediaType.getType()
            );
            send(serializedResponse, response.getStatusCode().getCode());

        } catch (Exception e) {
            throw new RuntimeException("Failed to send response", e);
        }
    }


    /**
     * Sends a response to the client with the specified body and status code.
     *
     * @param body       The response body as a string.
     * @param statusCode The HTTP status code to send.
     */
    private void send(String body, int statusCode) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to send response", e);
        }
    }


}