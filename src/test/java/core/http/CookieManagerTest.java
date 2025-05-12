package core.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.github.renatompf.ember.core.http.CookieManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CookieManagerTest {

    @Mock
    private HttpExchange exchange;
    
    @Mock
    private Headers requestHeaders;
    
    @Mock
    private Headers responseHeaders;
    
    private CookieManager cookieManager;

    @BeforeEach
    void setUp() {
        lenient().when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        lenient().when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        cookieManager = new CookieManager(exchange);
    }

    @Test
    void cookie_WhenNoCookiesExist_ShouldReturnNull() {
        // Arrange
        when(requestHeaders.getFirst("Cookie")).thenReturn(null);
        
        // Act
        String result = cookieManager.cookie("testCookie");
        
        // Assert
        assertNull(result);
    }

    @Test
    void cookie_WhenCookieExists_ShouldReturnValue() {
        // Arrange
        when(requestHeaders.getFirst("Cookie")).thenReturn("testCookie=testValue");
        
        // Act
        String result = cookieManager.cookie("testCookie");
        
        // Assert
        assertEquals("testValue", result);
    }

    @Test
    void cookie_WhenMultipleCookiesExist_ShouldReturnCorrectValue() {
        // Arrange
        when(requestHeaders.getFirst("Cookie")).thenReturn("cookie1=value1; testCookie=testValue; cookie3=value3");
        
        // Act
        String result = cookieManager.cookie("testCookie");
        
        // Assert
        assertEquals("testValue", result);
    }

    @Test
    void cookie_WhenCookieExistsWithoutValue_ShouldReturnEmptyString() {
        // Arrange
        when(requestHeaders.getFirst("Cookie")).thenReturn("testCookie=");
        
        // Act
        String result = cookieManager.cookie("testCookie");
        
        // Assert
        assertEquals("", result);
    }

    @Test
    void cookie_WhenCookieHasSpaces_ShouldTrimAndReturnValue() {
        // Arrange
        when(requestHeaders.getFirst("Cookie")).thenReturn(" testCookie = testValue ");
        
        // Act
        String result = cookieManager.cookie("testCookie");
        
        // Assert
        assertEquals("testValue", result);
    }

    @Test
    void setCookie_ShouldAddCorrectHeader() {
        // Act
        cookieManager.setCookie("testCookie", "testValue", 3600);
        
        // Assert
        verify(responseHeaders).add("Set-Cookie", "testCookie=testValue; Max-Age=3600; Path=/");
    }

    @Test
    void deleteCookie_ShouldSetEmptyValueAndZeroMaxAge() {
        // Act
        cookieManager.deleteCookie("testCookie");
        
        // Assert
        verify(responseHeaders).add("Set-Cookie", "testCookie=; Max-Age=0; Path=/");
    }
}