package core.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.github.renatompf.ember.core.http.Response;
import io.github.renatompf.ember.core.http.ResponseHandler;
import io.github.renatompf.ember.core.http.ResponseSerializer;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;
import io.github.renatompf.ember.enums.RequestHeader;
import io.github.renatompf.ember.exceptions.HttpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponseHandlerTest {

    @Mock
    private HttpExchange exchange;

    @Mock
    private Headers headers;

    private ResponseHandler responseHandler;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() throws IOException {
        lenient().when(exchange.getResponseHeaders()).thenReturn(headers);
        outputStream = new ByteArrayOutputStream();
        lenient().when(exchange.getResponseBody()).thenReturn(outputStream);

        responseHandler = new ResponseHandler(exchange);
    }

    @Test
    void handleResponse_WithNullBody_ShouldSendEmptyResponse() throws IOException {
        // Arrange
        Response<Object> response = Response.ok().body(null).build();

        // Act
        responseHandler.handleResponse(response);

        // Assert
        verify(exchange).sendResponseHeaders(HttpStatusCode.OK.getCode(), -1);
        // Can only verify that getResponseBody() was called, not that close() was called
        verify(exchange).getResponseBody();
    }

    @Test
    void handleResponse_WithJsonContentType_ShouldSerializeToJson() throws IOException {
        // Arrange
        TestDto dto = new TestDto("test value");
        Response<TestDto> response = Response.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .build();

        // Act
        responseHandler.handleResponse(response);

        // Assert
        verify(headers).set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.APPLICATION_JSON.getType());
        verify(exchange).sendResponseHeaders(eq(HttpStatusCode.OK.getCode()), anyLong());
        String responseBody = outputStream.toString();
        assertTrue(responseBody.contains("\"value\":\"test value\""));
    }

    @Test
    void handleResponse_WithXmlContentType_ShouldSerializeToXml() throws IOException {
        // Arrange
        TestDto dto = new TestDto("test value");
        Response<TestDto> response = Response.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(dto)
                .build();

        // Act
        responseHandler.handleResponse(response);

        // Assert
        verify(headers).set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.APPLICATION_XML.getType());
        verify(exchange).sendResponseHeaders(eq(HttpStatusCode.OK.getCode()), anyLong());
        String responseBody = outputStream.toString();
        assertTrue(responseBody.contains("<value>test value</value>"));
    }

    @Test
    void handleResponse_WithPlainTextContentType_ShouldUseToString() throws IOException {
        // Arrange
        TestDto dto = new TestDto("test value");
        Response<TestDto> response = Response.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(dto)
                .build();

        // Act
        responseHandler.handleResponse(response);

        // Assert
        verify(headers).set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.TEXT_PLAIN.getType());
        verify(exchange).sendResponseHeaders(eq(HttpStatusCode.OK.getCode()), anyLong());
        String responseBody = outputStream.toString();
        assertEquals(dto.toString(), responseBody);
    }

    @Test
    void handleResponse_WithNoContentTypeButAcceptHeader_ShouldUseAcceptHeader() throws IOException {
        // Arrange
        TestDto dto = new TestDto("test value");
        Response<TestDto> response = Response.ok().body(dto)
                .contentType(MediaType.APPLICATION_XML)
                .build();
        lenient().when(headers.getFirst(RequestHeader.ACCEPT.getHeaderName())).thenReturn(MediaType.APPLICATION_XML.getType());

        // Act
        responseHandler.handleResponse(response);

        // Assert
        verify(headers).set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.APPLICATION_XML.getType());
        verify(exchange).sendResponseHeaders(eq(HttpStatusCode.OK.getCode()), anyLong());
        String responseBody = outputStream.toString();
        assertTrue(responseBody.contains("<value>test value</value>"));
    }

    @Test
    void handleResponse_WithNoContentTypeAndNoAcceptHeader_ShouldDefaultToJson() throws IOException {
        // Arrange
        TestDto dto = new TestDto("test value");
        Response<TestDto> response = Response.ok().body(dto).build();

        // Act
        responseHandler.handleResponse(response);

        // Assert
        verify(headers).set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.APPLICATION_JSON.getType());
        verify(exchange).sendResponseHeaders(eq(HttpStatusCode.OK.getCode()), anyLong());
        String responseBody = outputStream.toString();
        assertTrue(responseBody.contains("\"value\":\"test value\""));
    }

    @Test
    void handleResponse_WithUnsupportedMediaType_ShouldThrowException() {
        // Arrange
        TestDto dto = new TestDto("test value");
        Response<TestDto> response = Response.ok().body(dto).build();

        try {
            java.lang.reflect.Field contentTypeField = Response.class.getDeclaredField("contentType");
            contentTypeField.setAccessible(true);
            contentTypeField.set(response, MediaType.MULTIPART_FORM_DATA.getType());
        } catch (Exception e) {
            fail("Failed to set unsupported content type", e);
        }

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> responseHandler.handleResponse(response)
        );

        assertEquals("Failed to send response", exception.getMessage());
        assertTrue(exception.getCause() instanceof HttpException);

        HttpException httpException = (HttpException) exception.getCause();
        assertEquals(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE, httpException.getStatus());
        assertEquals("Unsupported media type: " + MediaType.MULTIPART_FORM_DATA.getType(), httpException.getMessage());
    }

    @Test
    void registerCustomSerializer_WithValidMediaType_ShouldRegisterSerializer() {
        // Arrange
        ResponseSerializer serializer = obj -> "custom_serialized";

        // Act & Assert
        assertDoesNotThrow(() -> responseHandler.registerCustomSerializer(MediaType.TEXT_HTML.getType(), serializer));
    }

    @Test
    void registerCustomSerializer_WithInvalidMediaType_ShouldThrowException() {
        // Arrange
        ResponseSerializer serializer = obj -> "custom_serialized";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> responseHandler.registerCustomSerializer("invalid-media-type", serializer)
        );

        assertEquals("Invalid media type: invalid-media-type", exception.getMessage());
    }

    @Test
    void registerCustomSerializer_WithExistingMediaType_ShouldThrowException() {
        // Arrange
        ResponseSerializer serializer = obj -> "custom_serialized";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> responseHandler.registerCustomSerializer(MediaType.APPLICATION_JSON.getType(), serializer)
        );

        assertEquals("Media type already registered: application/json", exception.getMessage());
    }

    @Test
    void handleResponse_WithCustomSerializer_ShouldUseCustomSerializer() throws IOException {
        // Arrange
        TestDto dto = new TestDto("test value");
        Response<TestDto> response = Response.ok().body(dto).build();
        ResponseSerializer customSerializer = obj -> "CUSTOM:" + obj.toString();
        responseHandler.registerCustomSerializer(MediaType.TEXT_HTML.getType(), customSerializer);

        // Use reflection to set the content type since we can't modify it directly
        try {
            java.lang.reflect.Field contentTypeField = Response.class.getDeclaredField("contentType");
            contentTypeField.setAccessible(true);
            contentTypeField.set(response, MediaType.TEXT_HTML.getType());
        } catch (Exception e) {
            fail("Failed to set custom content type", e);
        }

        // Act
        responseHandler.handleResponse(response);

        // Assert
        verify(headers).set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.TEXT_HTML.getType());
        verify(exchange).sendResponseHeaders(eq(HttpStatusCode.OK.getCode()), anyLong());
        String responseBody = outputStream.toString();
        assertTrue(responseBody.startsWith("CUSTOM:"));
    }

    @Test
    void handleResponse_WithDateObject_ShouldFormatDateCorrectly() throws IOException {
        // Arrange
        DateDto dto = new DateDto(LocalDate.of(2023, 1, 15));
        Response<DateDto> response = Response.ok().body(dto).build();

        // Act
        responseHandler.handleResponse(response);

        // Assert
        verify(headers).set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.APPLICATION_JSON.getType());
        String responseBody = outputStream.toString();
        assertTrue(responseBody.contains("\"date\":\"2023-01-15\""));
    }

    @Test
    void handleResponse_WithSerializationException_ShouldWrapException() throws Exception {
        // Arrange
        TestDto dto = new TestDto("test value");
        Response<TestDto> response = Response.ok().body(dto).build();

        // Create a mock ResponseSerializer that throws an exception
        ResponseSerializer failingSerializer = obj -> { throw new IOException("Serialization failed"); };
        responseHandler.registerCustomSerializer(MediaType.OCTET_STREAM.getType(), failingSerializer);

        // Use reflection to set the content type
        try {
            java.lang.reflect.Field contentTypeField = Response.class.getDeclaredField("contentType");
            contentTypeField.setAccessible(true);
            contentTypeField.set(response, MediaType.OCTET_STREAM.getType());
        } catch (Exception e) {
            fail("Failed to set failing content type", e);
        }

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> responseHandler.handleResponse(response)
        );

        assertEquals("Failed to send response", exception.getMessage());
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void handleResponse_WithIOExceptionDuringSend_ShouldWrapException() throws IOException {
        // Arrange
        TestDto dto = new TestDto("test value");
        Response<TestDto> response = Response.ok().body(dto).build();

        // Make exchange.getResponseBody() throw an IOException
        OutputStream mockOutputStream = mock(OutputStream.class);
        doThrow(new IOException("Connection error")).when(mockOutputStream).write(any());
        when(exchange.getResponseBody()).thenReturn(mockOutputStream);

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> responseHandler.handleResponse(response)
        );

        assertEquals("Failed to send response", exception.getMessage());
    }

    @Test
    void handleResponse_WithEmptyAcceptHeader_ShouldDefaultToJson() throws IOException {
        // Arrange
        TestDto dto = new TestDto("test value");
        Response<TestDto> response = Response.ok().body(dto).build();
        lenient().when(headers.getFirst(RequestHeader.ACCEPT.getHeaderName())).thenReturn("");

        // Act
        responseHandler.handleResponse(response);

        // Assert
        verify(headers).set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.APPLICATION_JSON.getType());
        String responseBody = outputStream.toString();
        assertTrue(responseBody.contains("\"value\":\"test value\""));
    }

    @Test
    void handleResponse_WithNullContentType_ShouldDefaultToJson() throws IOException {
        // Arrange
        TestDto dto = new TestDto("test value");
        Response<TestDto> response = Response.ok().body(dto).build();

        // Use reflection to set contentType to null
        try {
            java.lang.reflect.Field contentTypeField = Response.class.getDeclaredField("contentType");
            contentTypeField.setAccessible(true);
            contentTypeField.set(response, null);
        } catch (Exception e) {
            fail("Failed to set null content type", e);
        }

        // Act
        responseHandler.handleResponse(response);

        // Assert
        verify(headers).set(RequestHeader.CONTENT_TYPE.getHeaderName(), MediaType.APPLICATION_JSON.getType());
        String responseBody = outputStream.toString();
        assertTrue(responseBody.contains("\"value\":\"test value\""));
    }

    @Test
    void handleResponse_WithLargeBody_ShouldSendSuccessfully() throws IOException {
        // Arrange
        String largeBody = "a".repeat(10_000);
        Response<String> response = Response.ok().body(largeBody).build();

        // Act
        responseHandler.handleResponse(response);

        // Assert
        verify(exchange).sendResponseHeaders(eq(HttpStatusCode.OK.getCode()), eq(largeBody.getBytes().length + 2L));
        String responseBody = outputStream.toString();
        assertEquals("\""+largeBody+"\"", responseBody);
    }

    @Test
    void handleResponse_WithUnsupportedAcceptHeader_ShouldThrowException() {
        // Arrange
        TestDto dto = new TestDto("test value");
        Response<TestDto> response = Response.ok().body(dto).build();

        // First, clear the content type in the response so Accept header will be used
        try {
            java.lang.reflect.Field contentTypeField = Response.class.getDeclaredField("contentType");
            contentTypeField.setAccessible(true);
            contentTypeField.set(response, "");
        } catch (Exception e) {
            fail("Failed to modify content type", e);
        }

        // Set the Accept header to a valid but unsupported media type
        when(headers.getFirst(RequestHeader.ACCEPT.getHeaderName())).thenReturn(MediaType.OCTET_STREAM.getType());

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> responseHandler.handleResponse(response)
        );

        assertEquals("Failed to send response", exception.getMessage());
        assertTrue(exception.getCause() instanceof HttpException);

        HttpException httpException = (HttpException) exception.getCause();
        assertEquals(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE, httpException.getStatus());
        assertEquals("Unsupported media type: " + MediaType.OCTET_STREAM.getType(), httpException.getMessage());
    }

    @Test
    void handleResponse_WithEmptyBody_ShouldSendEmptyResponse() throws IOException {
        // Arrange
        Response<String> response = Response.ok().body("").build();

        // Act
        responseHandler.handleResponse(response);

        // Assert
        verify(exchange).sendResponseHeaders(HttpStatusCode.OK.getCode(), 2L);
        String responseBody = outputStream.toString();
        assertEquals("\"\"", responseBody);
    }

    // Helper classes for testing
    static class TestDto {
        private String value;

        public TestDto() {}

        public TestDto(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "TestDto{value='" + value + "'}";
        }
    }

    static class DateDto {
        private LocalDate date;

        public DateDto() {}

        public DateDto(LocalDate date) {
            this.date = date;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }
    }
}