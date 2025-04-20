package io.ember.core;

import com.sun.net.httpserver.HttpExchange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeadersManager {
    private final HttpExchange exchange;
    private Map<String, String> headers;

    public HeadersManager(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public String header(String key) {
        if (headers == null) {
            headers = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
                headers.put(entry.getKey(), entry.getValue().getFirst());
            }
        }
        return headers.get(key);
    }

    public void setHeader(String key, String value) {
        exchange.getResponseHeaders().set(key, value);
    }
}