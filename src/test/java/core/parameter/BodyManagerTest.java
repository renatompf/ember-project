package core.parameter;

import core.parameter.mock.TestData;
import io.github.renatompf.ember.core.parameter.BodyManager;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;
import io.github.renatompf.ember.exceptions.HttpException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BodyManagerTest {

    @Test
    void parseBodyAs_WithJsonContentType_ShouldParseCorrectly() throws Exception {
        // Arrange
        String jsonBody = "{\"name\":\"John\",\"age\":30}";
        BodyManager bodyManager = new BodyManager(jsonBody, MediaType.APPLICATION_JSON.getType());

        // Act
        TestData result = bodyManager.parseBodyAs(TestData.class);

        // Assert
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
    }

    @Test
    void parseBodyAs_WithNullContentType_ShouldDefaultToJson() throws Exception {
        // Arrange
        String jsonBody = "{\"name\":\"Alice\",\"age\":25}";
        BodyManager bodyManager = new BodyManager(jsonBody, null);

        // Act
        TestData result = bodyManager.parseBodyAs(TestData.class);

        // Assert
        assertEquals("Alice", result.getName());
        assertEquals(25, result.getAge());
    }

    @Test
    void parseBodyAs_WithXmlContentType_ShouldParseCorrectly() throws Exception {
        // Arrange
        String xmlBody = "<TestData><name>Bob</name><age>40</age></TestData>";
        BodyManager bodyManager = new BodyManager(xmlBody, MediaType.APPLICATION_XML.getType());

        // Act
        TestData result = bodyManager.parseBodyAs(TestData.class);

        // Assert
        assertEquals("Bob", result.getName());
        assertEquals(40, result.getAge());
    }

    @Test
    void parseBodyAs_WithTextPlainContentType_ShouldUseStringConstructor() {
        // Arrange
        String textBody = "Plain text content";
        BodyManager bodyManager = new BodyManager(textBody, MediaType.TEXT_PLAIN.getType());

        // Act
        TestData result = bodyManager.parseBodyAs(TestData.class);

        // Assert
        assertEquals("Plain text content", result.getName());
        assertEquals(0, result.getAge());
    }

    @Test
    void parseBodyAs_WithUnsupportedContentType_ShouldThrowHttpException() {
        // Arrange
        String body = "Some content";
        BodyManager bodyManager = new BodyManager(body, "application/unsupported");

        // Act & Assert
        HttpException exception = assertThrows(HttpException.class, () ->
                bodyManager.parseBodyAs(TestData.class)
        );
        assertEquals(HttpStatusCode.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Unsupported Content-Type"));
    }

    @Test
    void parseBodyAs_WithMalformedJson_ShouldThrowHttpException() {
        // Arrange
        String malformedJson = "{\"name\":\"John\",\"age\":30,}"; // Note the trailing comma
        BodyManager bodyManager = new BodyManager(malformedJson, MediaType.APPLICATION_JSON.getType());

        // Act & Assert
        HttpException exception = assertThrows(HttpException.class, () ->
                bodyManager.parseBodyAs(TestData.class)
        );
        assertEquals(HttpStatusCode.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Failed to parse request body"));
    }

    @Test
    void parseBodyAs_WithMalformedXml_ShouldThrowHttpException() {
        // Arrange
        String malformedXml = "<TestData><name>Bob</name><age>40</TestData>"; // Missing closing tag
        BodyManager bodyManager = new BodyManager(malformedXml, MediaType.APPLICATION_XML.getType());

        // Act & Assert
        HttpException exception = assertThrows(HttpException.class, () ->
                bodyManager.parseBodyAs(TestData.class)
        );
        assertEquals(HttpStatusCode.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Failed to parse request body"));
    }

    @Test
    void parseBodyAs_WithTextPlainAndNoStringConstructor_ShouldThrowHttpException() {
        // Arrange
        String textBody = "Plain text content";
        BodyManager bodyManager = new BodyManager(textBody, MediaType.TEXT_PLAIN.getType());

        // Act & Assert
        HttpException exception = assertThrows(HttpException.class, () ->
                bodyManager.parseBodyAs(Integer.class)
        );
        assertEquals(HttpStatusCode.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Failed to parse request body"));
    }

}
