package io.github.renatompf.ember.core;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages query parameters extracted from a URL query string.
 * Provides methods to retrieve query parameter values.
 */
public class QueryParameterManager {
    // Stores the parsed query parameters as key-value pairs
    private final Map<String, String> queryParams;

    /**
     * Constructs a QueryParameterManager with the given query string.
     *
     * @param query The query string to parse (e.g., "key1=value1&amp;key2=value2").
     */
    public QueryParameterManager(String query) {
        this.queryParams = parseQueryParams(query);
    }

    /**
     * Parses the query string into a map of key-value pairs.
     *
     * @param query The query string to parse.
     * @return A map containing the parsed query parameters.
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;

        // Split the query string into key-value pairs
        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=", 2);
            String key = decode(keyValue[0]); // Decode the key
            String value = keyValue.length > 1 ? decode(keyValue[1]) : ""; // Decode the value or use an empty string
            params.put(key, value);
        }
        return params;
    }

    /**
     * Decodes a URL-encoded string using UTF-8.
     *
     * @param s The string to decode.
     * @return The decoded string.
     */
    private String decode(String s) {
        return URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Retrieves the value of a query parameter by its key.
     *
     * @param key The name of the query parameter to retrieve.
     * @return The value of the query parameter, or null if not present.
     */
    public String queryParam(String key) {
        return queryParams.get(key);
    }

    /**
     * Retrieves all query parameters as a map.
     *
     * @return A map containing all query parameters.
     */
    public Map<String, String> queryParams() {
        return queryParams;
    }

}