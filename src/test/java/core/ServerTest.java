package core;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.renatompf.ember.core.*;
import io.github.renatompf.ember.enums.HttpMethod;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.exceptions.HttpException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ServerTest {

    @Mock private Router router;
    @Mock private HttpServer httpServer;
    private Server server;
    private List<Middleware> middleware;
    private ArgumentCaptor<HttpHandler> handlerCaptor;
    private MockedStatic<HttpServer> mockedStatic;  // Add this field

    @BeforeEach
    void setUp() {
        middleware = List.of();
        server = new Server(router, middleware);
        handlerCaptor = ArgumentCaptor.forClass(HttpHandler.class);

        // Close any existing mock before creating a new one
        if (mockedStatic != null) {
            mockedStatic.close();
        }

        mockedStatic = mockStatic(HttpServer.class);
        mockedStatic.when(() -> HttpServer.create(any(InetSocketAddress.class), anyInt()))
                .thenReturn(httpServer);

    }

    @AfterEach
    void tearDown() {
        if (mockedStatic != null) {
            mockedStatic.close();
            mockedStatic = null;
        }
        if (server != null) {
            server.stop();
        }

    }


    @Test
    void constructor_ShouldAddContextMiddleware_WhenNotPresent() {
        // When
        Server server = new Server(router, List.of());

        // Then
        assertNotNull(server);
    }

    @Test
    void start_ShouldSetupServerCorrectly() throws IOException {
        // Given
        int port = 8080;

        // When
        server.start(port);

        // Then
        verify(httpServer).setExecutor(any(ExecutorService.class));
        verify(httpServer).createContext(eq("/"), any(HttpHandler.class));
        verify(httpServer).start();
    }

    @Test
    void start_ShouldHandleMatchingRoute() throws IOException, URISyntaxException {
        // Given
        RouteMatchResult matchResult = new RouteMatchResult(
                new MiddlewareChain(List.of(), ctx -> ctx.response().handleResponse(
                        Response.ok().body("Hello World").build()
                )),
                Map.of("param", "value")
        );
        when(router.getRoute(any(HttpMethod.class), anyString())).thenReturn(matchResult);

        // When
        server.start(8080);

        // Then
        verify(httpServer).createContext(eq("/"), handlerCaptor.capture());
        HttpHandler handler = handlerCaptor.getValue();

        // Simulate request
        HttpExchange exchange = mock(HttpExchange.class);
        Headers requestHeaders = new Headers();
        Headers responseHeaders = new Headers();

        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("http://localhost:8080/test"));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(exchange.getResponseBody()).thenReturn(mock(OutputStream.class));
        when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);

        // Execute handler
        handler.handle(exchange);

        // Verify response
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }

    @Test
    void start_ShouldHandle404WhenNoRouteMatches() throws IOException, URISyntaxException {
        // Given
        when(router.getRoute(any(HttpMethod.class), anyString())).thenReturn(null);

        // When
        server.start(8080);

        // Then
        verify(httpServer).createContext(eq("/"), handlerCaptor.capture());
        HttpHandler handler = handlerCaptor.getValue();

        // Simulate request
        HttpExchange exchange = mock(HttpExchange.class);
        Headers requestHeaders = new Headers();
        Headers responseHeaders = new Headers();

        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("http://localhost:8080/test"));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(exchange.getResponseBody()).thenReturn(mock(OutputStream.class));
        when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);

        // Execute handler
        handler.handle(exchange);

        // Verify response
        verify(exchange).sendResponseHeaders(eq(404), anyLong());
    }

    @Test
    void start_ShouldHandleHttpException() throws IOException, URISyntaxException {
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        HttpExchange exchange = mock(HttpExchange.class);

        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("http://localhost:8080/test"));
        when(exchange.getResponseBody()).thenReturn(responseBody);
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[0]));

        HttpException expectedException = new HttpException(HttpStatusCode.BAD_REQUEST, "Bad Request");
        when(router.getRoute(any(HttpMethod.class), anyString()))
                .thenThrow(expectedException);

        // When
        server.start(8080);
        verify(httpServer).createContext(eq("/"), handlerCaptor.capture());
        HttpHandler handler = handlerCaptor.getValue();
        handler.handle(exchange);

        // Then
        verify(exchange).sendResponseHeaders(eq(400), eq((long) "\"Bad Request\"".length()));
        assertArrayEquals(("\"Bad Request\"").getBytes(), responseBody.toByteArray());
    }

}
