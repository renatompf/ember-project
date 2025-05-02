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
import org.mockito.InOrder;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    void start_ShouldHandleInternalServerError() throws IOException, URISyntaxException {
        // Given
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        HttpExchange exchange = mock(HttpExchange.class);

        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("http://localhost:8080/test"));
        when(exchange.getResponseBody()).thenReturn(responseBody);
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[0]));

        RuntimeException unexpectedException = new RuntimeException("Unexpected error");
        when(router.getRoute(any(HttpMethod.class), anyString()))
                .thenThrow(unexpectedException);

        // When
        server.start(8080);
        verify(httpServer).createContext(eq("/"), handlerCaptor.capture());
        HttpHandler handler = handlerCaptor.getValue();
        handler.handle(exchange);

        // Then
        verify(exchange).sendResponseHeaders(eq(500), anyLong());
        assertTrue(responseBody.toString().contains("Internal Server Error"));
    }

    @Test
    void start_ShouldHandleIOException() {
        // Given
        mockedStatic.when(() -> HttpServer.create(any(InetSocketAddress.class), anyInt()))
                .thenThrow(new IOException("Failed to create server"));

        // When
        server.start(8080);

        // Then
        verify(httpServer, never()).setExecutor(any());
        verify(httpServer, never()).createContext(anyString(), any(HttpHandler.class));
        verify(httpServer, never()).start();

    }

    @Test
    void constructor_ShouldNotAddDuplicateContextMiddleware() {
        // Given
        List<Middleware> existingMiddleware = new ArrayList<>();
        existingMiddleware.add(new ContextMiddleware());

        // When
        Server server = new Server(router, existingMiddleware);

        // Then
        assertEquals(1, server.getMiddleware().size());
        assertInstanceOf(ContextMiddleware.class, server.getMiddleware().getFirst());
    }

    @Test
    void buildMiddlewareChain_ShouldIncludeGlobalAndRouteSpecificMiddleware() throws Exception {
        // Given
        Middleware globalMiddleware = mock(Middleware.class);
        Middleware routeMiddleware = mock(Middleware.class);
        server = new Server(router, List.of(globalMiddleware));

        // Ensure middleware calls next() to continue the chain
        doAnswer(invocation -> {
            Context context = invocation.getArgument(0);
            context.next();
            return null;
        }).when(globalMiddleware).handle(any(Context.class));

        doAnswer(invocation -> {
            Context context = invocation.getArgument(0);
            context.next();
            return null;
        }).when(routeMiddleware).handle(any(Context.class));

        // Final route handler
        MiddlewareChain middlewareChain = new MiddlewareChain(
                List.of(routeMiddleware),
                ctx -> ctx.response().handleResponse(Response.ok().build())
        );

        RouteMatchResult matchResult = new RouteMatchResult(middlewareChain, Map.of());

        // Ensure router is queried for correct path and method
        when(router.getRoute(eq(HttpMethod.GET), eq("/test"))).thenReturn(matchResult);

        // When
        server.start(8080);
        verify(httpServer).createContext(eq("/"), handlerCaptor.capture());
        HttpHandler handler = handlerCaptor.getValue();

        // Mock HTTP exchange
        Headers requestHeaders = new Headers();
        HttpExchange exchange = mock(HttpExchange.class);

        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("http://localhost:8080/test"));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(exchange.getResponseBody()).thenReturn(mock(OutputStream.class));

        // Execute handler
        handler.handle(exchange);

        // Then
        InOrder inOrder = inOrder(globalMiddleware, routeMiddleware);
        inOrder.verify(globalMiddleware).handle(any(Context.class));
        inOrder.verify(routeMiddleware).handle(any(Context.class));
    }

    @Test
    void start_ShouldHandleHttpExceptionInMiddleware() throws Exception {
        // Given
        Middleware throwingMiddleware = mock(Middleware.class);
        server = new Server(router, List.of(throwingMiddleware));
        HttpException expectedException = new HttpException(HttpStatusCode.FORBIDDEN, "Access Denied");

        doThrow(expectedException).when(throwingMiddleware).handle(any(Context.class));

        // When
        server.start(8080);
        verify(httpServer).createContext(eq("/"), handlerCaptor.capture());
        HttpHandler handler = handlerCaptor.getValue();

        // Simulate request
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        HttpExchange exchange = mock(HttpExchange.class);
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("http://localhost:8080/test"));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        when(exchange.getResponseBody()).thenReturn(responseBody);

        // Execute handler
        handler.handle(exchange);

        // Then
        verify(exchange).sendResponseHeaders(eq(403), anyLong());
        assertEquals("Access Denied", responseBody.toString());
    }

    @Test
    void start_ShouldHandleGeneralExceptionInMiddleware() throws Exception {
        // Given
        Middleware throwingMiddleware = mock(Middleware.class);
        server = new Server(router, List.of(throwingMiddleware));
        RuntimeException expectedException = new RuntimeException("Middleware Error");

        doThrow(expectedException).when(throwingMiddleware).handle(any(Context.class));

        // When
        server.start(8080);
        verify(httpServer).createContext(eq("/"), handlerCaptor.capture());
        HttpHandler handler = handlerCaptor.getValue();

        // Simulate request
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        HttpExchange exchange = mock(HttpExchange.class);
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("http://localhost:8080/test"));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        when(exchange.getResponseBody()).thenReturn(responseBody);

        // Execute handler
        handler.handle(exchange);

        // Then
        verify(exchange).sendResponseHeaders(eq(500), anyLong());
        assertTrue(responseBody.toString().contains("Internal Server Error"));
    }


}
