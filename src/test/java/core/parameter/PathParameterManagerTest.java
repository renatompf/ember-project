package core.parameter;

import io.github.renatompf.ember.core.parameter.PathParameterManager;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PathParameterManagerTest {

    @Test
    void constructor_ShouldInitializePathParameters() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("id", "123");
        
        // Act
        PathParameterManager manager = new PathParameterManager(params);
        
        // Assert
        assertEquals(params, manager.pathParams());
    }

    @Test
    void pathParam_WithExistingKey_ShouldReturnValue() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("id", "123");
        params.put("name", "test");
        PathParameterManager manager = new PathParameterManager(params);
        
        // Act
        String value = manager.pathParam("id");
        
        // Assert
        assertEquals("123", value);
    }

    @Test
    void pathParam_WithNonExistingKey_ShouldReturnNull() {
        // Arrange
        PathParameterManager manager = new PathParameterManager(Collections.emptyMap());
        
        // Act
        String value = manager.pathParam("nonexistent");
        
        // Assert
        assertNull(value);
    }

    @Test
    void pathParamAs_WithExistingKeyAndIntegerParser_ShouldReturnParsedValue() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("id", "123");
        PathParameterManager manager = new PathParameterManager(params);
        
        // Act
        Integer value = manager.pathParamAs("id", Integer::parseInt);
        
        // Assert
        assertEquals(123, value);
    }

    @Test
    void pathParamAs_WithExistingKeyAndBooleanParser_ShouldReturnParsedValue() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("active", "true");
        PathParameterManager manager = new PathParameterManager(params);
        
        // Act
        Boolean value = manager.pathParamAs("active", Boolean::parseBoolean);
        
        // Assert
        assertTrue(value);
    }

    @Test
    void pathParamAs_WithNonExistingKey_ShouldReturnNull() {
        // Arrange
        PathParameterManager manager = new PathParameterManager(Collections.emptyMap());
        
        // Act
        Integer value = manager.pathParamAs("nonexistent", Integer::parseInt);
        
        // Assert
        assertNull(value);
    }

    @Test
    void setPathParams_ShouldUpdatePathParameters() {
        // Arrange
        PathParameterManager manager = new PathParameterManager(Collections.emptyMap());
        Map<String, String> newParams = new HashMap<>();
        newParams.put("id", "456");
        
        // Act
        manager.setPathParams(newParams);
        
        // Assert
        assertEquals(newParams, manager.pathParams());
        assertEquals("456", manager.pathParam("id"));
    }

    @Test
    void pathParams_ShouldReturnPathParameters() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("id", "123");
        params.put("name", "test");
        PathParameterManager manager = new PathParameterManager(params);
        
        // Act
        Map<String, String> result = manager.pathParams();
        
        // Assert
        assertEquals(params, result);
        assertEquals(2, result.size());
        assertEquals("123", result.get("id"));
        assertEquals("test", result.get("name"));
    }

    @Test
    void pathParamAs_WithCustomParser_ShouldReturnParsedValue() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("csv", "1,2,3,4,5");
        PathParameterManager manager = new PathParameterManager(params);
        
        // Act
        String[] values = manager.pathParamAs("csv", s -> s.split(","));
        
        // Assert
        assertEquals(5, values.length);
        assertEquals("1", values[0]);
        assertEquals("5", values[4]);
    }
}