package core.parameter;


import core.parameter.mock.TestController;
import core.parameter.mock.TestData;
import io.github.renatompf.ember.core.parameter.BodyManager;
import io.github.renatompf.ember.core.parameter.ParameterResolver;
import io.github.renatompf.ember.core.parameter.PathParameterManager;
import io.github.renatompf.ember.core.parameter.QueryParameterManager;
import io.github.renatompf.ember.core.server.Context;
import io.github.renatompf.ember.utils.TypeConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParameterResolverTest {

    private ParameterResolver resolver;
    
    @Mock
    private Context context;
    
    @Mock
    private PathParameterManager pathParamManager;
    
    @Mock
    private QueryParameterManager queryParamManager;
    
    @Mock
    private BodyManager bodyManager;
    
    private Map<String, String> pathParams;
    private Map<String, String> queryParams;
    
    @BeforeEach
    void setUp() {
        resolver = new ParameterResolver();
        
        // Setup path parameters
        pathParams = new HashMap<>();
        pathParams.put("id", "123");
        lenient().when(pathParamManager.pathParams()).thenReturn(pathParams);
        lenient().when(context.pathParams()).thenReturn(pathParamManager);
        
        // Setup query parameters
        queryParams = new HashMap<>();
        queryParams.put("name", "testName");
        lenient().when(queryParamManager.queryParams()).thenReturn(queryParams);
        lenient().when(context.queryParams()).thenReturn(queryParamManager);
        
        // Setup body manager
        lenient().when(context.body()).thenReturn(bodyManager);
    }
    
    @Test
    void resolveParameter_WhenContextParameter_ShouldReturnContext() throws NoSuchMethodException {
        // Arrange
        Parameter parameter = TestController.class.getMethod("contextMethod", Context.class)
                .getParameters()[0];
                
        // Act
        Object result = resolver.resolveParameter(parameter, context);
        
        // Assert
        assertSame(context, result);
    }
    
    @Test
    void resolveParameter_WhenPathParameter_ShouldResolveParameter() throws NoSuchMethodException {
        // Arrange
        Parameter parameter = TestController.class.getMethod("pathParamMethod", Integer.class)
                .getParameters()[0];
        try (MockedStatic<TypeConverter> mockedConverter = mockStatic(TypeConverter.class)) {
            mockedConverter.when(() -> TypeConverter.convert("123", Integer.class)).thenReturn(123);
            
            // Act
            Object result = resolver.resolveParameter(parameter, context);
            
            // Assert
            assertEquals(123, result);
        }
    }
    
    @Test
    void resolveParameter_WhenQueryParameter_ShouldResolveParameter() throws NoSuchMethodException {
        // Arrange
        Parameter parameter = TestController.class.getMethod("queryParamMethod", String.class)
                .getParameters()[0];
        try (MockedStatic<TypeConverter> mockedConverter = mockStatic(TypeConverter.class)) {
            mockedConverter.when(() -> TypeConverter.convert("testName", String.class)).thenReturn("testName");
            
            // Act
            Object result = resolver.resolveParameter(parameter, context);
            
            // Assert
            assertEquals("testName", result);
        }
    }
    
    @Test
    void resolveParameter_WhenRequestBodyParameter_ShouldResolveParameter() throws NoSuchMethodException {
        // Arrange
        Parameter parameter = TestController.class.getMethod("bodyMethod", TestData.class)
                .getParameters()[0];
        TestData testModel = new TestData();
        when(bodyManager.parseBodyAs(TestData.class)).thenReturn(testModel);
        
        // Act
        Object result = resolver.resolveParameter(parameter, context);
        
        // Assert
        assertSame(testModel, result);
    }
    
    @Test
    void resolveParameter_WhenUnsupportedParameter_ShouldReturnNull() throws NoSuchMethodException {
        // Arrange
        Parameter parameter = TestController.class.getMethod("unsupportedMethod", String.class)
                .getParameters()[0];
        
        // Act
        Object result = resolver.resolveParameter(parameter, context);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void resolvePathParameter_WhenPathParamNotFound_ShouldReturnNull() throws NoSuchMethodException {
        // Arrange
        Parameter parameter = TestController.class.getMethod("missingPathParamMethod", Integer.class)
                .getParameters()[0];
        
        // Act
        Object result = resolver.resolveParameter(parameter, context);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void resolveQueryParameter_WhenQueryParamNotFound_ShouldThrowException() throws NoSuchMethodException {
        // Arrange
        Parameter parameter = TestController.class.getMethod("missingQueryParamMethod", String.class)
                .getParameters()[0];
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            resolver.resolveParameter(parameter, context)
        );
    }

}