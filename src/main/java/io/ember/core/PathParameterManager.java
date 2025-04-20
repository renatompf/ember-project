package io.ember.core;

import java.util.Map;
import java.util.function.Function;

public class PathParameterManager {
    private Map<String, String> pathParams;

    public PathParameterManager(Map<String, String> pathParams) {
        this.pathParams = pathParams;
    }

    public String pathParam(String key) {
        return pathParams.get(key);
    }

    public <T> T pathParamAs(String key, Function<String, T> parser) {
        String value = pathParams.get(key);
        if (value == null) {
            return null;
        }
        return parser.apply(value);
    }

    public void setPathParams(Map<String, String> pathParams) {
        this.pathParams = pathParams;
    }

}