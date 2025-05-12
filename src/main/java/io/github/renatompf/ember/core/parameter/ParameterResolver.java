package io.github.renatompf.ember.core.parameter;

import io.github.renatompf.ember.annotations.parameters.PathParameter;
import io.github.renatompf.ember.annotations.parameters.QueryParameter;
import io.github.renatompf.ember.annotations.parameters.RequestBody;
import io.github.renatompf.ember.core.server.Context;
import io.github.renatompf.ember.utils.TypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * The `ParameterResolver` class is responsible for resolving method parameters
 * based on their annotations and types.
 * <p>
 * It handles path parameters, query parameters, and request body parsing.
 * </p>
 */
public class ParameterResolver {
    private static final Logger logger = LoggerFactory.getLogger(ParameterResolver.class);

    /**
     * Resolves a method parameter based on its annotations and type.
     *
     * @param parameter The method parameter to resolve.
     * @param context   The HTTP request context.
     * @return The resolved parameter value, or `null` if the parameter type is unsupported.
     */
    public Object resolveParameter(Parameter parameter, Context context) {
        logger.debug("Resolving parameter {} of type {}", parameter.getName(), parameter.getType().getName());

        // Handle Context parameter
        if (parameter.getType().isAssignableFrom(Context.class)) {
            return context;
        }

        Object result = null;

        // Handle path parameters
        PathParameter pathParam = parameter.getAnnotation(PathParameter.class);
        if (pathParam != null) {
            result = resolvePathParameter(pathParam, parameter, context.pathParams().pathParams());
        }

        // Handle query parameters
        QueryParameter queryParam = parameter.getAnnotation(QueryParameter.class);
        if (queryParam != null) {
            result = resolveQueryParameter(queryParam, parameter, context.queryParams().queryParams());
        }

        // Handle request body
        if (parameter.isAnnotationPresent(RequestBody.class)) {
            Class<?> type = parameter.getType();
            result = context.body().parseBodyAs(type);
        }

        if (result == null) {
            logger.debug("Parameter {} of type {} is not supported", parameter.getName(), parameter.getType().getName());
        }

        return result;
    }

    /**
     * Resolves a path parameter based on its annotation and the provided path parameters.
     *
     * @param annotation  The PathParameter annotation.
     * @param parameter   The method parameter.
     * @param pathParams  The map of path parameters.
     * @return The resolved parameter value, or `null` if not found.
     */
    private Object resolvePathParameter(PathParameter annotation, Parameter parameter, Map<String, String> pathParams) {
        String paramName = annotation.value();
        String paramValue = pathParams.get(paramName);

        if (paramValue == null) {
            logger.debug("Optional path parameter {} not provided, returning null.", paramName);
            return null;
        }

        return TypeConverter.convert(paramValue, parameter.getType());
    }

    /**
     * Resolves a query parameter based on its annotation and the provided query parameters.
     *
     * @param annotation   The QueryParameter annotation.
     * @param parameter    The method parameter.
     * @param queryParams  The map of query parameters.
     * @return The resolved parameter value, or `null` if not found.
     */
    private Object resolveQueryParameter(QueryParameter annotation, Parameter parameter, Map<String, String> queryParams) {
        String paramName = annotation.value();
        String paramValue = queryParams.get(paramName);

        if (paramValue == null) {
            logger.debug("Required query parameter {} not provided, returning null.", paramName);
            throw new IllegalArgumentException("Missing query parameter: " + paramName);
        }

        return TypeConverter.convert(paramValue, parameter.getType());
    }
}