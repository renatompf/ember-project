package io.github.renatompf.ember.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

public class TypeConverter {

    /**
     * A map of supported types and their corresponding conversion functions.
     * The keys are the target types, and the values are functions that take a string
     * and return an object of the target type.
     */
    private static final Map<Class<?>, Function<String, Object>> CONVERTERS = Map.ofEntries(
            Map.entry(Integer.class, Integer::parseInt),
            Map.entry(int.class, Integer::parseInt),
            Map.entry(Long.class, Long::parseLong),
            Map.entry(long.class, Long::parseLong),
            Map.entry(Double.class, Double::parseDouble),
            Map.entry(double.class, Double::parseDouble),
            Map.entry(Boolean.class, Boolean::parseBoolean),
            Map.entry(boolean.class, Boolean::parseBoolean),
            Map.entry(String.class, str -> str),
            Map.entry(char.class, str -> str.charAt(0)),
            Map.entry(Character.class, str -> str.charAt(0)),
            Map.entry(Byte.class, Byte::parseByte),
            Map.entry(byte.class, Byte::parseByte),
            Map.entry(Short.class, Short::parseShort),
            Map.entry(short.class, Short::parseShort),
            Map.entry(Float.class, Float::parseFloat),
            Map.entry(float.class, Float::parseFloat),
            Map.entry(UUID.class, UUID::fromString),
            Map.entry(LocalDate.class, LocalDate::parse),
            Map.entry(LocalDateTime.class, LocalDateTime::parse),
            Map.entry(Instant.class, Instant::parse),
            Map.entry(Date.class, TypeConverter::parseDate)
    );

    /**
     * Converts a string value to the specified target type.
     *
     * @param value      the string value to convert
     * @param targetType the target type to convert to
     * @param <T>        the type of the target type
     * @return the converted value
     * @throws IllegalArgumentException if the conversion is not supported
     */
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

    /**
     * Parses a date string into a Date object using multiple formats.
     *
     * @param str the date string to parse
     * @return the parsed Date object
     * @throws IllegalArgumentException if the date string cannot be parsed
     */
    private static Date parseDate(String str) {
        List<String> formats = List.of(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "dd/MM/yyyy",
                "MM-dd-yyyy"
        );

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));  // This is the key change
                return sdf.parse(str);
            } catch (ParseException ignored) {
            }
        }

        throw new IllegalArgumentException("Unsupported date format: " + str);
    }

}