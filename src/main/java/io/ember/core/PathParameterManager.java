package io.ember.core;

import java.util.Map;
import java.util.function.Function;

/**
 * Manages path parameters extracted from a URL.
 * Provides methods to retrieve and parse path parameters.
 */
public class PathParameterManager {
    private Map<String, String> pathParams;

    /**
     * Constructs a PathParameterManager with the given path parameters.
     *
     * @param pathParams A map of path parameter names to their values.
     */
    public PathParameterManager(Map<String, String> pathParams) {
        this.pathParams = pathParams;
    }

    /**
     * Retrieves the value of a path parameter by its key.
     *
     * @param key The name of the path parameter to retrieve.
     * @return The value of the path parameter, or null if not present.
     */
    public String pathParam(String key) {
        return pathParams.get(key);
    }

    /**
     * Retrieves and parses the value of a path parameter by its key.
     *
     * @param <T>    The type to parse the parameter value into.
     * @param key    The name of the path parameter to retrieve.
     * @param parser A function to parse the parameter value.
     * @return The parsed value of the path parameter, or null if not present.
     */
    public <T> T pathParamAs(String key, Function<String, T> parser) {
        String value = pathParams.get(key);
        if (value == null) {
            return null;
        }
        return parser.apply(value);
    }

    /**
     * Sets the path parameters for this manager.
     *
     * @param pathParams A map of path parameter names to their values.
     */
    public void setPathParams(Map<String, String> pathParams) {
        this.pathParams = pathParams;
    }
}