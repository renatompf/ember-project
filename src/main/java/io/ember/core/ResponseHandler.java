package io.ember.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import io.ember.enums.HttpStatusCode;
import io.ember.enums.MediaType;
import io.ember.enums.RequestHeader;
import io.ember.exceptions.HttpException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ResponseHandler {
    private final HttpExchange exchange;
    private static final ObjectMapper mapper = new ObjectMapper();

    public ResponseHandler(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public void sendJson(Object response, int status) {
        try {
            String jsonResponse = mapper.writeValueAsString(response);
            exchange.getResponseHeaders().set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.APPLICATION_JSON.getType());
            send(jsonResponse, status);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert response to JSON", e);
        }
    }

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

    public void ok(Object body) {
        sendJson(body, HttpStatusCode.OK.getCode());
    }

    public void created(Object body) {
        sendJson(body, HttpStatusCode.CREATED.getCode());
    }

    public void notFound(Object body) {
        sendJson(body, HttpStatusCode.NOT_FOUND.getCode());
    }

    public void badRequest(Object body) {
        sendJson(body, HttpStatusCode.BAD_REQUEST.getCode());
    }

    public void unauthorized(Object body) {
        sendJson(body, HttpStatusCode.UNAUTHORIZED.getCode());
    }

    public void forbidden(String message) {
        sendJson(Map.of("error", message), HttpStatusCode.FORBIDDEN.getCode());
    }

    public void internalServerError(String message) {
        sendJson(Map.of("error", message), HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
    }

    public void throwStatus(int status, Object body) {
        throw new HttpException(HttpStatusCode.fromCode(status), body);
    }

    public void throwStatus(HttpStatusCode status, Object body) {
        throw new HttpException(status, body);
    }
}