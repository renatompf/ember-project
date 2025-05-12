package core.parameter;

import io.github.renatompf.ember.core.parameter.QueryParameterManager;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryParameterManagerTest {

    @Test
    void constructor_WithNormalQueryString_ShouldParseParameters() {
        // Arrange & Act
        QueryParameterManager manager = new QueryParameterManager("key1=value1&key2=value2");
        
        // Assert
        assertEquals("value1", manager.queryParam("key1"));
        assertEquals("value2", manager.queryParam("key2"));
    }
    
    @Test
    void constructor_WithEmptyQueryString_ShouldReturnEmptyMap() {
        // Arrange & Act
        QueryParameterManager manager = new QueryParameterManager("");
        
        // Assert
        assertTrue(manager.queryParams().isEmpty());
    }
    
    @Test
    void constructor_WithNullQueryString_ShouldReturnEmptyMap() {
        // Arrange & Act
        QueryParameterManager manager = new QueryParameterManager(null);
        
        // Assert
        assertTrue(manager.queryParams().isEmpty());
    }
    
    @Test
    void queryParam_WithExistingKey_ShouldReturnValue() {
        // Arrange
        QueryParameterManager manager = new QueryParameterManager("key=value");
        
        // Act
        String value = manager.queryParam("key");
        
        // Assert
        assertEquals("value", value);
    }
    
    @Test
    void queryParam_WithNonExistingKey_ShouldReturnNull() {
        // Arrange
        QueryParameterManager manager = new QueryParameterManager("key=value");
        
        // Act
        String value = manager.queryParam("nonexistent");
        
        // Assert
        assertNull(value);
    }
    
    @Test
    void queryParams_ShouldReturnAllParameters() {
        // Arrange
        QueryParameterManager manager = new QueryParameterManager("key1=value1&key2=value2");
        
        // Act
        Map<String, String> params = manager.queryParams();
        
        // Assert
        assertEquals(2, params.size());
        assertEquals("value1", params.get("key1"));
        assertEquals("value2", params.get("key2"));
    }
    
    @Test
    void parseQueryParams_WithUrlEncodedValues_ShouldDecode() {
        // Arrange & Act
        QueryParameterManager manager = new QueryParameterManager("key=Hello%20World");
        
        // Assert
        assertEquals("Hello World", manager.queryParam("key"));
    }
    
    @Test
    void parseQueryParams_WithUrlEncodedKeys_ShouldDecode() {
        // Arrange & Act
        QueryParameterManager manager = new QueryParameterManager("hello%20world=value");
        
        // Assert
        assertEquals("value", manager.queryParam("hello world"));
    }
    
    @Test
    void parseQueryParams_WithSpecialCharacters_ShouldDecode() {
        // Arrange & Act
        QueryParameterManager manager = new QueryParameterManager("key=%21%40%23%24%25%5E%26");
        
        // Assert
        assertEquals("!@#$%^&", manager.queryParam("key"));
    }
    
    @Test
    void parseQueryParams_WithParameterWithoutValue_ShouldUseEmptyString() {
        // Arrange & Act
        QueryParameterManager manager = new QueryParameterManager("key=");
        
        // Assert
        assertEquals("", manager.queryParam("key"));
    }
    
    @Test
    void parseQueryParams_WithParameterWithoutEquals_ShouldUseEmptyString() {
        // Arrange & Act
        QueryParameterManager manager = new QueryParameterManager("key");
        
        // Assert
        assertEquals("", manager.queryParam("key"));
    }
    
    @Test
    void parseQueryParams_WithDuplicateParameters_ShouldUseLastValue() {
        // Arrange & Act
        QueryParameterManager manager = new QueryParameterManager("key=value1&key=value2");
        
        // Assert
        assertEquals("value2", manager.queryParam("key"));
    }
    
    @Test
    void parseQueryParams_WithMultipleParameters_ShouldParseAll() {
        // Arrange & Act
        QueryParameterManager manager = new QueryParameterManager("a=1&b=2&c=3&d=4&e=5");
        
        // Assert
        assertEquals(5, manager.queryParams().size());
        assertEquals("1", manager.queryParam("a"));
        assertEquals("5", manager.queryParam("e"));
    }
    
    @Test
    void parseQueryParams_WithPlusSign_ShouldDecodeAsSpace() {
        // Arrange & Act
        QueryParameterManager manager = new QueryParameterManager("key=hello+world");
        
        // Assert
        assertEquals("hello world", manager.queryParam("key"));
    }
}