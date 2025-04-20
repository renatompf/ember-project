package io.ember.core;

import com.sun.net.httpserver.HttpExchange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages HTTP headers for a given HTTP exchange.
 * Provides methods to retrieve request headers and set response headers.
 */
public class HeadersManager {
    private final HttpExchange exchange;
    private Map<String, String> headers;

    /**
     * Constructs a HeadersManager for the given HTTP exchange.
     *
     * @param exchange The HTTP exchange to manage headers for.
     */
    public HeadersManager(HttpExchange exchange) {
        this.exchange = exchange;
    }

    /**
     * Retrieves the value of a request header by its key.
     * If the headers are not yet initialized, they are loaded from the exchange.
     *
     * @param key The name of the header to retrieve.
     * @return The value of the header, or null if the header is not present.
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
     * Sets a response header with the given key and value.
     *
     * @param key   The name of the header to set.
     * @param value The value of the header to set.
     */
    public void setHeader(String key, String value) {
        exchange.getResponseHeaders().set(key, value);
    }
}