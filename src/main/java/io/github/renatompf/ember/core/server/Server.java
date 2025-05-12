package io.github.renatompf.ember.core.server;

import com.sun.net.httpserver.HttpServer;
import io.github.renatompf.ember.core.http.Response;
import io.github.renatompf.ember.core.routing.RouteMatchResult;
import io.github.renatompf.ember.core.routing.Router;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;
import io.github.renatompf.ember.enums.RequestHeader;
import io.github.renatompf.ember.exceptions.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Represents an HTTP server that handles incoming requests and routes them to the appropriate handlers.
 * <p>
 * The server uses a router to determine the correct route for each request and applies middleware
 * to process requests and responses. It can be started on a specified port and can be stopped when no longer needed.
 */
public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private final Router router;
    private final List<Middleware> middleware;
    private HttpServer server;

    /**
     * Constructs a new Server instance with the specified router and middleware.
     * <p>
     * This constructor initializes the server with a router for handling routes
     * and a list of middleware to be applied to incoming requests.
     * If no context middleware is provided, it adds a default ContextMiddleware.
     *
     * @param router    The router instance for managing routes.
     * @param middleware A list of middleware to be applied to requests.
     */
    public Server(Router router, List<Middleware> middleware) {
        this.router = router;
        this.middleware = new ArrayList<>(middleware);
        boolean containsContextMiddleware = middleware
                .stream()
                .anyMatch(m -> m instanceof ContextMiddleware);
        if (!containsContextMiddleware) {
            this.middleware.add(new ContextMiddleware());
        }
    }

    /**
     * Starts the HTTP server on the specified port.
     * <p>
     * This method initializes an `HttpServer` instance, sets up a request handler, and starts the server.
     * It processes incoming HTTP requests by creating a `Context` object, building the middleware chain,
     * and executing the chain. If an exception occurs during request processing, appropriate HTTP error
     * responses are sent back to the client.
     * <p>
     * The server uses a virtual thread pool executor to handle requests asynchronously.
     *
     * @param port The port number on which the server will listen for incoming requests.
     */
    public void start(int port) {

        try{
            server = HttpServer.create(new InetSocketAddress(port), 0);
            // Setting the custom executor for handling requests asynchronously
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            logger.info("HTTP server created and executor set.");

            // Setting up the context for handling requests
            server.createContext("/", exchange -> {
                logger.info("Received request: {} {}", exchange.getRequestMethod(), exchange.getRequestURI());
                Context context = new Context(
                        exchange,
                        exchange.getRequestURI().getQuery(),
                        new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                                .lines()
                                .reduce("", (acc, line) -> acc + line),
                        exchange.getRequestHeaders().getFirst(RequestHeader.CONTENT_TYPE.getHeaderName()) != null
                                ? exchange.getRequestHeaders().getFirst(RequestHeader.CONTENT_TYPE.getHeaderName())
                                : MediaType.OCTET_STREAM.getType(),
                        Map.of()
                );

                context.setMiddlewareChain(buildMiddlewareChain(context));
                try {
                    context.next();
                } catch (HttpException e) {
                    logger.error("HTTP exception occurred: {}", e.getMessage());
                    exchange.sendResponseHeaders(e.getStatus().getCode(), 0);
                    exchange.getResponseBody().write(e.getMessage().getBytes());
                    exchange.close();
                } catch (Exception e) {
                    logger.error("Unexpected error occurred while processing request.", e);
                    e.printStackTrace();
                    HttpStatusCode internalServerError = HttpStatusCode.INTERNAL_SERVER_ERROR;
                    exchange.sendResponseHeaders(internalServerError.getCode(), 0);
                    exchange.getResponseBody().write(HttpStatusCode.INTERNAL_SERVER_ERROR.getMessage().getBytes());
                    exchange.close();
                }

            });

            server.start();
            logger.info("============ HTTP server started on port {} ============", port);
        }catch (IOException e){
            e.printStackTrace();
            logger.error("Failed to start HTTP server on port {}: {}", port, e.getMessage());
        }

    }

    /**
     * Builds the middleware chain for the given context.
     *<p>
     * This method determines the appropriate middleware chain to execute for a given request.
     * It first retrieves the route match result based on the HTTP method and path of the request.
     * If a matching route is found, it adds the route-specific middleware and handler to the chain.
     * If no match is found, it adds a middleware that sends a 404 Not Found response.
     *<p>
     * @param context The context for the current request, containing request details and state.
     * @return A list of middleware to be executed for the request.
     */
    private List<Middleware> buildMiddlewareChain(Context context) {
        logger.debug("Building middleware chain for request: {} {}", context.getMethod(), context.getPath());
        List<Middleware> fullChain = new ArrayList<>(middleware);

        try {
            RouteMatchResult match = router.getRoute(context.getMethod(), context.getPath());
            if(match != null) {
                logger.debug("Route match found: {}", match);
                context.pathParams().setPathParams(match.parameters());
                fullChain.addAll(match.middlewareChain().middleware());
                fullChain.add(c -> match.middlewareChain().handler().accept(c));
            } else {
                logger.warn("No route match found for path: {}", context.getPath());
                fullChain.add(c -> c.response().handleResponse(
                        Response
                                .status(HttpStatusCode.NOT_FOUND)
                                .body(HttpStatusCode.NOT_FOUND.getMessage())
                                .build()));
            }
        } catch (HttpException e) {
            logger.error("HTTP exception occurred while building middleware chain: {}", e.getMessage());
            fullChain.add(c -> c.response().handleResponse(
                    Response
                            .status(e.getStatus())
                            .body(e.getMessage())
                            .build())
            );
        } catch (Exception e) {
            logger.error("Unexpected error occurred while building middleware chain.", e);
            fullChain.add(c -> c.response().handleResponse(
                    Response
                            .status(HttpStatusCode.INTERNAL_SERVER_ERROR)
                            .body(HttpStatusCode.INTERNAL_SERVER_ERROR.getMessage())
                            .build())
            );
        }

        return fullChain;
    }

    /**
     * Stops the HTTP server.
     * <p>
     * This method stops the server and releases any resources associated with it.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("HTTP server stopped");
        }
    }

    /**
     * Returns the middleware list used by the server.
     *
     * @return The list of middleware.
     */
    public List<Middleware> getMiddleware() {
        return middleware;
    }
}
