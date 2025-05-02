package utils;

import io.github.renatompf.ember.utils.TypeConverter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TypeConverterTest {

    /**
     * This class tests the `convert` method of the `TypeConverter` class.
     * The `convert` method takes a string value and a target type,
     * and converts the string to the requested type using predefined converters.
     */

    @Test
    void testConvertToInteger() {
        String value = "123";
        Integer result = TypeConverter.convert(value, Integer.class);
        assertEquals(123, result);
    }

    @Test
    void testConvertToPrimitiveInt() {
        String value = "456";
        int result = TypeConverter.convert(value, int.class);
        assertEquals(456, result);
    }

    @Test
    void testConvertToLong() {
        String value = "789123";
        Long result = TypeConverter.convert(value, Long.class);
        assertEquals(789123L, result);
    }

    @Test
    void testConvertToPrimitiveLong() {
        String value = "890123";
        long result = TypeConverter.convert(value, long.class);
        assertEquals(890123L, result);
    }

    @Test
    void testConvertToDouble() {
        String value = "123.45";
        Double result = TypeConverter.convert(value, Double.class);
        assertEquals(123.45, result);
    }

    @Test
    void testConvertToPrimitiveDouble() {
        String value = "678.90";
        double result = TypeConverter.convert(value, double.class);
        assertEquals(678.90, result);
    }

    @Test
    void testConvertToBoolean() {
        String value = "true";
        Boolean result = TypeConverter.convert(value, Boolean.class);
        assertTrue(result);
    }

    @Test
    void testConvertToPrimitiveBoolean() {
        String value = "false";
        boolean result = TypeConverter.convert(value, boolean.class);
        assertFalse(result);
    }

    @Test
    void testConvertToString() {
        String value = "hello";
        String result = TypeConverter.convert(value, String.class);
        assertEquals("hello", result);
    }

    @Test
    void testConvertToCharacter() {
        String value = "A";
        Character result = TypeConverter.convert(value, Character.class);
        assertEquals('A', result);
    }

    @Test
    void testConvertToPrimitiveChar() {
        String value = "B";
        char result = TypeConverter.convert(value, char.class);
        assertEquals('B', result);
    }

    @Test
    void testConvertToByte() {
        String value = "123";
        Byte result = TypeConverter.convert(value, Byte.class);
        assertEquals((byte) 123, result);
    }

    @Test
    void testConvertToPrimitiveByte() {
        String value = "45";
        byte result = TypeConverter.convert(value, byte.class);
        assertEquals((byte) 45, result);
    }

    @Test
    void testConvertToShort() {
        String value = "4567";
        Short result = TypeConverter.convert(value, Short.class);
        assertEquals((short) 4567, result);
    }

    @Test
    void testConvertToPrimitiveShort() {
        String value = "1234";
        short result = TypeConverter.convert(value, short.class);
        assertEquals((short) 1234, result);
    }

    @Test
    void testConvertToFloat() {
        String value = "123.45";
        Float result = TypeConverter.convert(value, Float.class);
        assertEquals(123.45f, result);
    }

    @Test
    void testConvertToPrimitiveFloat() {
        String value = "678.90";
        float result = TypeConverter.convert(value, float.class);
        assertEquals(678.90f, result);
    }

    @Test
    void testConvertToUUID() {
        String value = "123e4567-e89b-12d3-a456-426614174000";
        UUID result = TypeConverter.convert(value, UUID.class);
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), result);
    }

    @Test
    void testConvertToLocalDate() {
        String value = "2025-05-02";
        LocalDate result = TypeConverter.convert(value, LocalDate.class);
        assertEquals(LocalDate.of(2025, 05, 02), result);
    }

    @Test
    void testConvertToLocalDateTime() {
        String value = "2025-05-02T10:15:30";
        LocalDateTime result = TypeConverter.convert(value, LocalDateTime.class);
        assertEquals(LocalDateTime.of(2025, 05, 02, 10, 15, 30), result);
    }

    @Test
    void testConvertToInstant() {
        String value = "2025-05-02T10:15:30Z";
        Instant result = TypeConverter.convert(value, Instant.class);
        assertEquals(Instant.parse("2025-05-02T10:15:30Z"), result);
    }

    @Test
    void testConvertUnsupportedTypeThrowsException() {
        String value = "test";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                TypeConverter.convert(value, Void.class));
        assertTrue(exception.getMessage().contains("Cannot convert value"));
    }

    @Test
    void testConvertToDate() {
        String value = "2025-05-02T10:15:30Z";
        Date result = TypeConverter.convert(value, Date.class);
        Instant expected = Instant.parse("2025-05-02T10:15:30Z");
        assertEquals(expected, result.toInstant());
    }


    @Test
    void testConvertToDateWithDifferentFormats() {
        String value1 = "02/05/2025";
        Date result1 = TypeConverter.convert(value1, Date.class);

        String value2 = "02-05-2025";
        Date result2 = TypeConverter.convert(value2, Date.class);

        assertNotNull(result1);
        assertNotNull(result2);
    }

    @Test
    void testConvertToDateWithUnsupportedFormatThrowsException() {
        String value = "02.05.2025";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                TypeConverter.convert(value, Date.class));
        assertTrue(exception.getMessage().contains("Unsupported date format"));
    }
}