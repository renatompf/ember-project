package core.parameter;

import core.parameter.mock.TestController;
import io.github.renatompf.ember.core.http.HeadersManager;
import io.github.renatompf.ember.core.parameter.ContentNegotiationManager;
import io.github.renatompf.ember.core.server.Context;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;
import io.github.renatompf.ember.exceptions.HttpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentNegotiationManagerTest {

    private ContentNegotiationManager manager;

    @Mock
    private Context context;

    @Mock
    private HeadersManager headerManager;

    @BeforeEach
    void setUp() {
        manager = new ContentNegotiationManager();
        lenient().when(context.headers()).thenReturn(headerManager);
    }

    @Test
    void validateContentType_WithMatchingContentType_ShouldNotThrowException() throws NoSuchMethodException {
        // Arrange
        Method method = TestController.class.getMethod("consumesJson");
        when(headerManager.header("Content-Type")).thenReturn("application/json");

        // Act & Assert - no exception should be thrown
        assertDoesNotThrow(() -> manager.validateContentType(context, method));
    }

    @Test
    void validateContentType_WithUnsupportedContentType_ShouldThrowException() throws NoSuchMethodException {
        // Arrange
        Method method = TestController.class.getMethod("consumesJson");
        when(headerManager.header("Content-Type")).thenReturn("application/xml");

        // Act & Assert
        HttpException exception = assertThrows(HttpException.class,
                () -> manager.validateContentType(context, method));
        assertEquals(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE, exception.getStatus());
        assertTrue(exception.getMessage().contains("application/xml"));
    }

    @Test
    void validateContentType_WithNoContentTypeHeader_ShouldNotThrowException() throws NoSuchMethodException {
        // Arrange
        Method method = TestController.class.getMethod("consumesJson");
        when(headerManager.header("Content-Type")).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> manager.validateContentType(context, method));
    }

    @Test
    void validateContentType_WithNoConsumesAnnotation_ShouldNotThrowException() throws NoSuchMethodException {
        // Arrange
        Method method = TestController.class.getMethod("noAnnotations");
        when(headerManager.header("Content-Type")).thenReturn("application/json");

        // Act & Assert
        assertDoesNotThrow(() -> manager.validateContentType(context, method));
    }

    @Test
    void validateContentType_WithMultipleAllowedContentTypes_ShouldAcceptMatching() throws NoSuchMethodException {
        // Arrange
        Method method = TestController.class.getMethod("consumesMultiple");
        when(headerManager.header("Content-Type")).thenReturn("application/xml");

        // Act & Assert
        assertDoesNotThrow(() -> manager.validateContentType(context, method));
    }

    @Test
    void negotiateResponseType_WithNoProducesAnnotation_ShouldReturnDefaultJson() throws NoSuchMethodException {
        // Arrange
        Method method = TestController.class.getMethod("noAnnotations");

        // Act
        MediaType result = manager.negotiateResponseType(context, method);

        // Assert
        assertEquals(MediaType.APPLICATION_JSON, result);
    }

    @Test
    void negotiateResponseType_WithNoAcceptHeader_ShouldReturnFirstProducesValue() throws NoSuchMethodException {
        // Arrange
        Method method = TestController.class.getMethod("producesMultiple");
        when(headerManager.header("Accept")).thenReturn(null);

        // Act
        MediaType result = manager.negotiateResponseType(context, method);

        // Assert
        assertEquals(MediaType.APPLICATION_JSON, result);
    }

    @Test
    void negotiateResponseType_WithEmptyAcceptHeader_ShouldReturnFirstProducesValue() throws NoSuchMethodException {
        // Arrange
        Method method = TestController.class.getMethod("producesMultiple");
        when(headerManager.header("Accept")).thenReturn("");

        // Act
        MediaType result = manager.negotiateResponseType(context, method);

        // Assert
        assertEquals(MediaType.APPLICATION_JSON, result);
    }

    @Test
    void negotiateResponseType_WithStarAcceptHeader_ShouldReturnFirstProducesValue() throws NoSuchMethodException {
        // Arrange
        Method method = TestController.class.getMethod("producesMultiple");
        when(headerManager.header("Accept")).thenReturn("*/*");

        // Act
        MediaType result = manager.negotiateResponseType(context, method);

        // Assert
        assertEquals(MediaType.APPLICATION_JSON, result);
    }

    @Test
    void negotiateResponseType_WithMatchingAcceptHeader_ShouldReturnMatchingMediaType() throws NoSuchMethodException {
        // Arrange
        Method method = TestController.class.getMethod("producesMultiple");
        when(headerManager.header("Accept")).thenReturn("application/xml");

        // Act
        MediaType result = manager.negotiateResponseType(context, method);

        // Assert
        assertEquals(MediaType.APPLICATION_XML, result);
    }

    @Test
    void negotiateResponseType_WithMultipleAcceptValues_ShouldReturnFirstMatching() throws NoSuchMethodException {
        // Arrange
        Method method = TestController.class.getMethod("producesMultiple");
        when(headerManager.header("Accept")).thenReturn("text/plain, application/xml");

        // Act
        MediaType result = manager.negotiateResponseType(context, method);

        // Assert
        assertEquals(MediaType.APPLICATION_XML, result);
    }

    @Test
    void negotiateResponseType_WithNoMatchingAcceptValues_ShouldThrowException() throws NoSuchMethodException {
        // Arrange
        Method method = TestController.class.getMethod("producesJson");
        when(headerManager.header("Accept")).thenReturn("text/plain");

        // Act & Assert
        HttpException exception = assertThrows(HttpException.class,
                () -> manager.negotiateResponseType(context, method));
        assertEquals(HttpStatusCode.NOT_ACCEPTABLE, exception.getStatus());
    }
}