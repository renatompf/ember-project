package core.server;

import com.sun.net.httpserver.HttpExchange;
import io.github.renatompf.ember.core.http.CookieManager;
import io.github.renatompf.ember.core.http.HeadersManager;
import io.github.renatompf.ember.core.http.ResponseHandler;
import io.github.renatompf.ember.core.http.SessionManager;
import io.github.renatompf.ember.core.parameter.BodyManager;
import io.github.renatompf.ember.core.parameter.PathParameterManager;
import io.github.renatompf.ember.core.parameter.QueryParameterManager;
import io.github.renatompf.ember.core.server.Context;
import io.github.renatompf.ember.core.server.Middleware;
import io.github.renatompf.ember.enums.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContextTest {

    @Mock
    private HttpExchange exchange;

    private Context context;
    private String query = "param1=value1&param2=value2";
    private String body = "{\"key\":\"value\"}";
    private String contentType = "application/json";
    private Map<String, String> pathParams;

    @BeforeEach
    void setUp() throws Exception {
        pathParams = new HashMap<>();
        pathParams.put("id", "123");
        
        // Setup mock behavior
        lenient().when(exchange.getRequestMethod()).thenReturn("GET");
        lenient().when(exchange.getRequestURI()).thenReturn(new URI("/test/path"));
        
        context = new Context(exchange, query, body, contentType, pathParams);
    }

    @Test
    void constructor_ShouldInitializeAllManagers() {
        // Assert
        assertNotNull(context.headers());
        assertNotNull(context.pathParams());
        assertNotNull(context.queryParams());
        assertNotNull(context.body());
        assertNotNull(context.cookies());
        assertNotNull(context.response());
        assertNotNull(context.session());
    }

    @Test
    void getMethod_ShouldReturnHttpMethod() {
        // Act
        HttpMethod method = context.getMethod();
        
        // Assert
        assertEquals(HttpMethod.GET, method);
        verify(exchange).getRequestMethod();
    }

    @Test
    void getPath_ShouldReturnRequestPath() {
        // Act
        String path = context.getPath();
        
        // Assert
        assertEquals("/test/path", path);
        verify(exchange).getRequestURI();
    }

    @Test
    void headers_ShouldReturnHeadersManager() {
        // Act
        HeadersManager headersManager = context.headers();
        
        // Assert
        assertNotNull(headersManager);
        assertEquals(HeadersManager.class, headersManager.getClass());
    }

    @Test
    void pathParams_ShouldReturnPathParameterManager() {
        // Act
        PathParameterManager pathParamManager = context.pathParams();
        
        // Assert
        assertNotNull(pathParamManager);
        assertEquals(PathParameterManager.class, pathParamManager.getClass());
        assertEquals("123", pathParamManager.pathParam("id"));
    }

    @Test
    void queryParams_ShouldReturnQueryParameterManager() {
        // Act
        QueryParameterManager queryParamManager = context.queryParams();
        
        // Assert
        assertNotNull(queryParamManager);
        assertEquals(QueryParameterManager.class, queryParamManager.getClass());
    }

    @Test
    void body_ShouldReturnBodyManager() {
        // Act
        BodyManager bodyManager = context.body();
        
        // Assert
        assertNotNull(bodyManager);
        assertEquals(BodyManager.class, bodyManager.getClass());
    }

    @Test
    void cookies_ShouldReturnCookieManager() {
        // Act
        CookieManager cookieManager = context.cookies();
        
        // Assert
        assertNotNull(cookieManager);
        assertEquals(CookieManager.class, cookieManager.getClass());
    }

    @Test
    void response_ShouldReturnResponseHandler() {
        // Act
        ResponseHandler responseHandler = context.response();
        
        // Assert
        assertNotNull(responseHandler);
        assertEquals(ResponseHandler.class, responseHandler.getClass());
    }

    @Test
    void session_ShouldReturnSessionManager() {
        // Act
        SessionManager sessionManager = context.session();
        
        // Assert
        assertNotNull(sessionManager);
        assertEquals(SessionManager.class, sessionManager.getClass());
    }

    @Test
    void next_ShouldExecuteNextMiddleware() throws Exception {
        // Arrange
        List<Middleware> middlewareChain = new ArrayList<>();
        Middleware middleware1 = mock(Middleware.class);
        Middleware middleware2 = mock(Middleware.class);
        middlewareChain.add(middleware1);
        middlewareChain.add(middleware2);
        
        context.setMiddlewareChain(middlewareChain);
        
        // Act
        context.next();
        
        // Assert
        verify(middleware1).handle(context);
        verify(middleware2, never()).handle(context);
        
        // Call next again
        context.next();
        
        // Verify second middleware was called
        verify(middleware2).handle(context);
    }

    @Test
    void next_WhenNoMoreMiddleware_ShouldNotThrowException() throws Exception {
        // Arrange
        List<Middleware> middlewareChain = new ArrayList<>();
        Middleware middleware = mock(Middleware.class);
        middlewareChain.add(middleware);
        
        context.setMiddlewareChain(middlewareChain);
        
        // Act
        context.next(); // First middleware
        context.next(); // No more middleware
        
        // Assert
        verify(middleware, times(1)).handle(context);
        // No exception should be thrown
    }

    @Test
    void setMiddlewareChain_ShouldResetMiddlewareIndex() throws Exception {
        // Arrange
        List<Middleware> middlewareChain1 = new ArrayList<>();
        Middleware middleware1 = mock(Middleware.class);
        middlewareChain1.add(middleware1);
        
        List<Middleware> middlewareChain2 = new ArrayList<>();
        Middleware middleware2 = mock(Middleware.class);
        middlewareChain2.add(middleware2);
        
        context.setMiddlewareChain(middlewareChain1);
        context.next(); // Execute first middleware chain
        
        // Act
        context.setMiddlewareChain(middlewareChain2);
        context.next();
        
        // Assert
        verify(middleware1).handle(context);
        verify(middleware2).handle(context);
    }
}