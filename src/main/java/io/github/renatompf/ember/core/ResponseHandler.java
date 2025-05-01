package io.github.renatompf.ember.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;
import io.github.renatompf.ember.enums.RequestHeader;
import io.github.renatompf.ember.exceptions.HttpException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
    public void sendJson(Object response, int status) {
        try {
            String jsonResponse = mapper.writeValueAsString(response);
            exchange.getResponseHeaders().set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.APPLICATION_JSON.getType());
            send(jsonResponse, status);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert response to JSON", e);
        }
    }

    /**
     * Sends a plain text response with the specified status code.
     *
     * @param body       The response body as a string.
     * @param statusCode The HTTP status code to send.
     */
    public void send(String body, int statusCode) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.TEXT_PLAIN.getType());
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to send response", e);
        }
    }

    /**
     * Sends a 200 OK response with a JSON body.
     *
     * @param body The response body to serialize to JSON.
     */
    public void ok(Object body) {
        sendJson(body, HttpStatusCode.OK.getCode());
    }

    /**
     * Sends a 200 OK response with a plain text body.
     *
     * @param body The response body as a string.
     */
    public void ok(String body) {
        sendJson(body, HttpStatusCode.OK.getCode());
    }

    /**
     * Sends a 204 No Content response.
     */
    public void noContent() {
        send("", HttpStatusCode.NO_CONTENT.getCode());
    }

    /**
     * Sends a 201 Created response with a JSON body.
     *
     * @param body The response body to serialize to JSON.
     */
    public void created(Object body) {
        sendJson(body, HttpStatusCode.CREATED.getCode());
    }

    /**
     * Sends a 404 Not Found response with a JSON body.
     *
     * @param body The response body to serialize to JSON.
     */
    public void notFound(Object body) {
        sendJson(body, HttpStatusCode.NOT_FOUND.getCode());
    }

    /**
     * Sends a 400 Bad Request response with a JSON body.
     *
     * @param body The response body to serialize to JSON.
     */
    public void badRequest(Object body) {
        sendJson(body, HttpStatusCode.BAD_REQUEST.getCode());
    }

    /**
     * Sends a 401 Unauthorized response with a JSON body.
     *
     * @param body The response body to serialize to JSON.
     */
    public void unauthorized(Object body) {
        sendJson(body, HttpStatusCode.UNAUTHORIZED.getCode());
    }

    /**
     * Sends a 403 Forbidden response with an error message.
     *
     * @param message The error message to include in the response.
     */
    public void forbidden(String message) {
        sendJson(Map.of("error", message), HttpStatusCode.FORBIDDEN.getCode());
    }

    /**
     * Sends a 500 Internal Server Error response with an error message.
     *
     * @param message The error message to include in the response.
     */
    public void internalServerError(String message) {
        sendJson(Map.of("error", message), HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
    }

    /**
     * Throws an HTTP exception with the specified status code and body.
     *
     * @param status The HTTP status code to throw.
     * @param body   The response body to include in the exception.
     */
    public void throwStatus(int status, Object body) {
        throw new HttpException(HttpStatusCode.fromCode(status), body);
    }

    /**
     * Throws an HTTP exception with the specified status and body.
     *
     * @param status The HttpStatusCode enum representing the status to throw.
     * @param body   The response body to include in the exception.
     */
    public void throwStatus(HttpStatusCode status, Object body) {
        throw new HttpException(status, body);
    }
}