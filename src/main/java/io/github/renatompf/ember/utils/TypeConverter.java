package io.github.renatompf.ember.utils;

import java.util.Map;
import java.util.function.Function;

public class TypeConverter {
    private static final Map<Class<?>, Function<String, Object>> CONVERTERS = Map.of(
            Integer.class, Integer::parseInt,
            int.class, Integer::parseInt,
            Long.class, Long::parseLong,
            long.class, Long::parseLong,
            Double.class, Double::parseDouble,
            double.class, Double::parseDouble,
            Boolean.class, Boolean::parseBoolean,
            boolean.class, Boolean::parseBoolean,
            String.class, str -> str
    );

    @SuppressWarnings("unchecked")
    public static <T> T convert(String value, Class<T> targetType) {
        Function<String, Object> converter = CONVERTERS.get(targetType);
        if (converter == null) {
            throw new IllegalArgumentException(
                    String.format("Cannot convert value '%s' to unsupported type: %s",
                            value, targetType.getName())
            );
        }
        return (T) converter.apply(value);
    }
}