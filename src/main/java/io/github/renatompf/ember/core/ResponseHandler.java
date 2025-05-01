package io.github.renatompf.ember.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import io.github.renatompf.ember.enums.MediaType;
import io.github.renatompf.ember.enums.RequestHeader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResponseHandler {
    // The HTTP exchange object used to send and receive HTTP data
    private final HttpExchange exchange;

    // A static ObjectMapper instance for JSON serialization
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructs a ResponseHandler with the given HttpExchange.
     *
     * @param exchange The HttpExchange object for handling HTTP requests and responses.
     */
    public ResponseHandler(HttpExchange exchange) {
        this.exchange = exchange;
    }

    /**
     * Sends a JSON response with the specified status code.
     *
     * @param response The response object to serialize to JSON.
     * @param status   The HTTP status code to send.
     */
    private void sendJson(Object response, int status) {
        try {
            String jsonResponse = mapper.writeValueAsString(response);
            exchange.getResponseHeaders().set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.APPLICATION_JSON.getType());
            send(jsonResponse, status);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert response to JSON", e);
        }
    }

    private void send(String body, int statusCode) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            if (!exchange.getResponseHeaders().containsKey(RequestHeader.CONTENT_TYPE.getHeaderName())) {
                exchange.getResponseHeaders().set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.TEXT_PLAIN.getType());
            }
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to send response", e);
        }
    }

    /**
     * Handles a generic Response object by sending its contents with appropriate headers and status code.
     *
     * @param response The Response object containing the body, headers, and status code
     * @param <T> The type of the response body
     */
    public <T> void handleResponse(Response<T> response) {
        try {
            // Handle the response based on body presence
            if (response.getBody() != null) {
                String contentType = response.getContentType() != null ? response.getContentType() : MediaType.TEXT_PLAIN.getType();
                if (MediaType.APPLICATION_JSON.getType().equals(contentType)) {
                    sendJson(response.getBody(), response.getStatusCode().getCode());
                } else {
                    send(response.getBody().toString(), response.getStatusCode().getCode());
                }
            } else {
                exchange.sendResponseHeaders(response.getStatusCode().getCode(), -1);
                exchange.getResponseBody().close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to send response", e);
        }
    }

}