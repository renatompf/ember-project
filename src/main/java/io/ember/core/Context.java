package io.ember.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sun.net.httpserver.HttpExchange;
import io.ember.enums.HttpMethod;
import io.ember.enums.HttpStatusCode;
import io.ember.enums.MediaType;
import io.ember.enums.RequestHeader;
import io.ember.exceptions.HttpException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The `Context` class provides an abstraction over the `HttpExchange` object,
 * offering utility methods for handling HTTP requests and responses.
 * It supports parsing query parameters, path parameters, request bodies,
 * and sending responses in various formats.
 */
public class Context {

    private final HttpExchange exchange;

    private String cachedBody = null;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Map<String, String> pathParams = Map.of();
    private List<Middleware> middlewareChain;
    private int middlewareIndex = -1;

    private static final Map<String, Map<String, Object>> sessions = new HashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final XmlMapper xmlMapper = new XmlMapper();

    public Context(final HttpExchange exchange) {
        this.exchange = exchange;
    }

    /**
     * Retrieves the HTTP method of the current request.
     *
     * @return The HTTP method as an `HttpMethod` enum.
     */
    public HttpMethod getMethod() {
        return HttpMethod.fromString(exchange.getRequestMethod().toUpperCase());
    }

    /**
     * Retrieves the path of the current request.
     *
     * @return The request path as a `String`.
     */
    public String getPath() {
        return exchange.getRequestURI().getPath();
    }

    /**
     * Retrieves the value of a request header by its key.
     *
     * @param key The key of the request header.
     * @return The value of the request header, or `null` if not present.
     */
    public String header(String key) {
        if (headers == null) {
            headers = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
                headers.put(entry.getKey(), entry.getValue().getFirst());
            }
        }
        return headers.get(key);
    }

    /**
     * Retrieves the value of a cookie by its name.
     *
     * @param name The name of the cookie.
     * @return The value of the cookie, or `null` if not present.
     */
    public String cookie(String name) {
        String cookies = header("Cookie");
        if (cookies != null) {
            for (String cookie : cookies.split(";")) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts[0].equals(name)) {
                    return parts.length > 1 ? parts[1] : "";
                }
            }
        }
        return null;
    }

    /**
     * Sets a cookie in the response headers.
     *
     * @param name    The name of the cookie.
     * @param value   The value of the cookie.
     * @param maxAge  The maximum age of the cookie in seconds.
     */
    public void setCookie(String name, String value, int maxAge) {
        String cookie = name + "=" + value + "; Max-Age=" + maxAge + "; Path=/";
        exchange.getResponseHeaders().add("Set-Cookie", cookie);
    }

    /**
     * Deletes a cookie by setting its value to an empty string and max age to 0.
     *
     * @param name The name of the cookie to delete.
     */
    public void deleteCookie(String name) {
        setCookie(name, "", 0);
    }

    /**
     * Retrieves the session data for a given session ID.
     * If the session does not exist, a new session is created.
     *
     * @param sessionId The ID of the session.
     * @return A map representing the session data.
     */
    public Map<String, Object> session(String sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> new HashMap<>());
    }

    /**
     * Sets an attribute in the session data for a given session ID.
     *
     * @param sessionId The ID of the session.
     * @param key       The key of the attribute.
     * @param value     The value of the attribute.
     */
    public void setSessionAttribute(String sessionId, String key, Object value) {
        session(sessionId).put(key, value);
    }

    /**
     * Retrieves an attribute from the session data for a given session ID.
     *
     * @param sessionId The ID of the session.
     * @param key       The key of the attribute.
     * @return The value of the attribute, or `null` if not present.
     */
    public Object sessionAttribute(String sessionId, String key) {
        return session(sessionId).get(key);
    }

    /**
     * Invalidates the session for a given session ID.
     * This removes all session data associated with the session ID.
     *
     * @param sessionId The ID of the session to invalidate.
     */
    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }


    /**
     * Retrieves the body of the current request as a `String`.
     * The body is cached after the first read.
     *
     * @return The request body as a `String`.
     * @throws IOException If an I/O error occurs while reading the body.
     */
    public String body() throws IOException {
        if(cachedBody == null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                cachedBody = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
        return cachedBody;
    }

    public <T> T bodyAs(Class<T> clazz) {
        String contentType = exchange.getRequestHeaders().getFirst(RequestHeader.CONTENT_TYPE.getHeaderName());

        try {
            if (contentType == null || contentType.equals(MediaType.APPLICATION_JSON.getType())) {
                return mapper.readValue(body(), clazz);
            } else if (contentType.contains(MediaType.APPLICATION_XML.getType())) {
                return xmlMapper.readValue(body(), clazz);
            } else if (contentType.equals(MediaType.TEXT_PLAIN.getType())) {
                return clazz.getConstructor(String.class).newInstance(body());
            }

            throw new HttpException(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE, "Unsupported Content-Type: " + contentType);

        } catch (Exception e) {
            throw new HttpException(HttpStatusCode.BAD_REQUEST, "Failed to parse request body: " + e.getMessage());
        }
    }

    /**
     * Retrieves the value of a query parameter by its key.
     *
     * @param key The key of the query parameter.
     * @return The value of the query parameter, or `null` if not present.
     */
    public String queryParam(String key) {
        parseQueryParams();
        return queryParams.get(key);
    }

    /**
     * Retrieves the value of a query parameter by its key, or a default value if the parameter is not present.
     *
     * @param key          The key of the query parameter.
     * @param defaultValue The default value to return if the parameter is not present.
     * @return The value of the query parameter, or the default value if not present.
     */
    public String queryParam(String key, String defaultValue) {
        String value = queryParam(key);
        return value == null ? defaultValue : value;
    }

    /**
     * Parses the query parameters from the request URI and stores them in a map.
     * This method is called lazily and caches the parsed parameters.
     */
    private void parseQueryParams() {
        if (queryParams != null) return;

        queryParams = new HashMap<>();
        String query = exchange.getRequestURI().getRawQuery();

        if (query == null || query.isEmpty()) return;

        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = decode(keyValue[0]);
                String value = decode(keyValue[1]);
                queryParams.put(key, value);
            } else if (keyValue.length == 1) {
                String key = decode(keyValue[0]);
                queryParams.put(key, "");
            }
        }
    }

    /**
     * Decodes a URL-encoded string using UTF-8.
     *
     * @param s The string to decode.
     * @return The decoded string.
     * @throws RuntimeException If the string cannot be decoded.
     */
    private String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode string: " + s, e);
        }
    }

    /**
     * Sets the middleware chain for the current context.
     *
     * @param middlewareChain The list of middleware to execute.
     */
    void setMiddlewareChain(List<Middleware> middlewareChain) {
        this.middlewareChain = middlewareChain;
        this.middlewareIndex = -1;
    }

    /**
     * Executes the next middleware in the chain.
     *
     * @throws Exception If an error occurs during middleware execution.
     */
    public void next() throws Exception {
        middlewareIndex++;
        if (middlewareIndex < middlewareChain.size()) {
            middlewareChain.get(middlewareIndex).handle(this);
        }
    }

    /**
     * Sets the path parameters for the current context.
     *
     * @param pathParams A map of path parameter names to their values.
     */
    public void setPathParams(Map<String, String> pathParams) {
        this.pathParams = pathParams;
    }

    /**
     * Retrieves the value of a path parameter by its key.
     *
     * @param key The key of the path parameter.
     * @return The value of the path parameter, or `null` if not present.
     */
    public String pathParam(String key) {
        return pathParams.get(key);
    }

    /**
     * Retrieves a path parameter by its key and parses it using the provided parser function.
     *
     * @param <T>    The type to which the path parameter should be parsed.
     * @param key    The key of the path parameter.
     * @param parser A function to parse the path parameter value.
     * @return The parsed value of the path parameter, or `null` if the parameter is not present.
     * @throws RuntimeException If the parser function throws an exception during parsing.
     */
    public <T> T pathParamAs(String key, Function<String, T> parser) {
        String value = pathParam(key);
        if (value == null) {
            return null;
        }

        if (parser == null) {
            return (T) value; // Fallback to raw String
        }

        try {
            return parser.apply(value);
        } catch (Exception e) {
            if (parser.equals(Function.identity())) {
                return (T) value; // Fallback to raw String
            }
        }
        return (T) value;
    }

    public Integer pathParamAsInt(String key) {
        return pathParamAs(key, Integer::parseInt);
    }

    public Double pathParamAsDouble(String key) {
        return pathParamAs(key, Double::parseDouble);
    }

    public Long pathParamAsLong(String key) {
        return pathParamAs(key, Long::parseLong);
    }

    public Boolean pathParamAsBoolean(String key) {
        return pathParamAs(key, Boolean::parseBoolean);
    }

    public String pathParamAsString(String key) {
        return pathParamAs(key, null);
    }


    /**
     * Sends a JSON response with an HTTP 200 OK status.
     *
     * @param body The response body to send.
     */
    public void ok(Object body) {
        sendJson(body, HttpStatusCode.OK.getCode());
    }

    /**
     * Sends a JSON response with an HTTP 201 Created status.
     *
     * @param body The response body to send.
     */
    public void created(Object body) {
        sendJson(body, HttpStatusCode.CREATED.getCode());
    }

    /**
     * Sends a JSON response with an HTTP 404 Not Found status.
     *
     * @param body The response body to send.
     */
    public void notFound(Object body) {
        sendJson(body, HttpStatusCode.NOT_FOUND.getCode());
    }

    /**
     * Sends a JSON response with an HTTP 400 Bad Request status.
     *
     * @param body The response body to send.
     */
    public void badRequest(Object body) {
        sendJson(body, HttpStatusCode.BAD_REQUEST.getCode());
    }


    /**
     * Sends a JSON response with an HTTP 401 Unauthorized status.
     *
     * @param body The response body to send.
     */
    public void unauthorized(Object body) {
        sendJson(body, HttpStatusCode.UNAUTHORIZED.getCode());
    }

    /**
     * Sends a JSON response with a custom HTTP status code.
     *
     * @param body   The response body to send.
     * @param status The HTTP status code to use.
     */
    public void error(Object body, int status) {
        sendJson(body, status);
    }

    /**
     * Throws an HTTP exception with a custom status code and response body.
     *
     * @param status The HTTP status code to throw.
     * @param body   The response body to include in the exception.
     */
    public void throwStatus(int status, Object body) {
        throw new HttpException(HttpStatusCode.fromCode(status), body);
    }

    /**
     * Throws an HTTP exception with a predefined status code and response body.
     *
     * @param status The HTTP status code to throw.
     * @param body   The response body to include in the exception.
     */
    public void throwStatus(HttpStatusCode status, Object body) {
        throw new HttpException(status, body);
    }

    /**
     * Throws an HTTP 403 Forbidden exception with a custom error message.
     *
     * @param message The error message to include in the exception.
     */
    public void forbidden(String message) {
        sendJson(Map.of("error", message), HttpStatusCode.FORBIDDEN.getCode());
    }


    /**
     * Throws an HTTP 500 Internal Server Error exception with a custom error message.
     *
     * @param message The error message to include in the exception.
     */
    public void internalServerError(String message) {
        throwStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode(), Map.of("error", message));
    }

    /**
     * Sends a JSON response with an HTTP 200 OK status.
     *
     * @param response The object to serialize as JSON and send in the response.
     * @throws RuntimeException If the JSON serialization fails.
     */
    public void sendJson(Object response) {
        sendJson(response, HttpStatusCode.OK.getCode());
    }

    /**
     * Sends a JSON response with the specified HTTP status code.
     *
     * @param response The object to serialize as JSON and send in the response.
     * @param status   The HTTP status code to send.
     * @throws RuntimeException If the JSON serialization fails.
     */
    public void sendJson(Object response, int status)  {
        try {
            String jsonResponse = mapper.writeValueAsString(response);
            exchange.getResponseHeaders().set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.APPLICATION_JSON.getType());
            send(jsonResponse, status);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert response to JSON", e);
        }
    }

    /**
     * Sends a plain text response with an HTTP 200 OK status.
     *
     * @param response The response body as a `String`.
     */
    public void send(String response) {
        send(response, HttpStatusCode.OK.getCode());
    }

    /**
     * Sends a plain text response with the specified HTTP status code.
     *
     * @param body       The response body as a `String`.
     * @param statusCode The HTTP status code to send.
     */
    public void send(String body, HttpStatusCode statusCode) {
        send(body, statusCode.getCode());
    }

    /**
     * Sends a plain text response with the specified HTTP status code.
     *
     * @param body       The response body as a `String`.
     * @param statusCode The HTTP status code to send.
     * @throws RuntimeException If an I/O error occurs while sending the response.
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

}
