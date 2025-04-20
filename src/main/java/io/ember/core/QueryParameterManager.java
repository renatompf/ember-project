package io.ember.core;

import java.util.HashMap;
import java.util.Map;

public class QueryParameterManager {
    private final Map<String, String> queryParams;

    public QueryParameterManager(String query) {
        this.queryParams = parseQueryParams(query);
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;

        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=", 2);
            String key = decode(keyValue[0]);
            String value = keyValue.length > 1 ? decode(keyValue[1]) : "";
            params.put(key, value);
        }
        return params;
    }

    private String decode(String s) {
        return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    public String queryParam(String key) {
        return queryParams.get(key);
    }
}