package core.http;

import io.github.renatompf.ember.core.http.ErrorResponse;
import io.github.renatompf.ember.enums.HttpStatusCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ErrorResponseTest {

    @Test
    void shouldCreateErrorResponseWithAllFields() {
        HttpStatusCode status = HttpStatusCode.BAD_REQUEST;
        String message = "Test error";
        String path = "/test";
        String exception = "TestException";

        ErrorResponse response = new ErrorResponse(status, message, path, exception);

        assertNotNull(response.getTimestamp());
        assertEquals(status, response.getStatus());
        assertEquals(message, response.getMessage());
        assertEquals(path, response.getPath());
        assertEquals(exception, response.getException());
    }

    @Test
    void shouldGenerateCorrectToString() {
        ErrorResponse response = new ErrorResponse(
                HttpStatusCode.BAD_REQUEST,
                "Test error",
                "/test",
                "TestException"
        );

        String toString = response.toString();

        assertTrue(toString.contains("BAD_REQUEST"));
        assertTrue(toString.contains("Test error"));
        assertTrue(toString.contains("/test"));
        assertTrue(toString.contains("TestException"));
    }


}
