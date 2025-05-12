package core.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.github.renatompf.ember.core.http.HeadersManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeadersManagerTest {

    @Mock
    private HttpExchange exchange;
    
    @Mock
    private Headers requestHeaders;
    
    @Mock
    private Headers responseHeaders;
    
    private HeadersManager headersManager;

    @BeforeEach
    void setUp() {
        lenient().when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        lenient().when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        headersManager = new HeadersManager(exchange);
    }

    @Test
    void header_WhenHeaderExists_ShouldReturnValue() {
        // Arrange
        Map<String, List<String>> headersMap = new HashMap<>();
        headersMap.put("Content-Type", Arrays.asList("application/json"));
        when(requestHeaders.entrySet()).thenReturn(headersMap.entrySet());
        
        // Act
        String result = headersManager.header("Content-Type");
        
        // Assert
        assertEquals("application/json", result);
    }

    @Test
    void header_WhenHeaderDoesNotExist_ShouldReturnNull() {
        // Arrange
        Map<String, List<String>> headersMap = new HashMap<>();
        headersMap.put("Content-Type", Arrays.asList("application/json"));
        when(requestHeaders.entrySet()).thenReturn(headersMap.entrySet());
        
        // Act
        String result = headersManager.header("Authorization");
        
        // Assert
        assertNull(result);
    }

    @Test
    void header_WhenCalledMultipleTimes_ShouldCacheHeaders() {
        // Arrange
        Map<String, List<String>> headersMap = new HashMap<>();
        headersMap.put("Content-Type", Arrays.asList("application/json"));
        when(requestHeaders.entrySet()).thenReturn(headersMap.entrySet());
        
        // Act
        headersManager.header("Content-Type");
        headersManager.header("Authorization");
        
        // Assert
        // Verify getRequestHeaders and entrySet are called only once
        verify(exchange, times(1)).getRequestHeaders();
        verify(requestHeaders, times(1)).entrySet();
    }

    @Test
    void headers_ShouldReturnAllHeaders() {
        // Arrange
        Map<String, List<String>> headersMap = new HashMap<>();
        headersMap.put("Content-Type", Arrays.asList("application/json"));
        headersMap.put("Authorization", Arrays.asList("Bearer token"));
        when(requestHeaders.entrySet()).thenReturn(headersMap.entrySet());
        
        // Act
        Map<String, String> result = headersManager.headers();
        
        // Assert
        assertEquals(2, result.size());
        assertEquals("application/json", result.get("Content-Type"));
        assertEquals("Bearer token", result.get("Authorization"));
    }

    @Test
    void headers_WhenCalledMultipleTimes_ShouldCacheHeaders() {
        // Arrange
        Map<String, List<String>> headersMap = new HashMap<>();
        headersMap.put("Content-Type", Arrays.asList("application/json"));
        when(requestHeaders.entrySet()).thenReturn(headersMap.entrySet());
        
        // Act
        headersManager.headers();
        headersManager.headers();
        
        // Assert
        verify(exchange, times(1)).getRequestHeaders();
        verify(requestHeaders, times(1)).entrySet();
    }

    @Test
    void setHeader_ShouldSetResponseHeader() {
        // Act
        headersManager.setHeader("Content-Type", "application/json");
        
        // Assert
        verify(responseHeaders).set("Content-Type", "application/json");
    }
}