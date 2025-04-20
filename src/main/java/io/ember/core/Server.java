package io.ember.core;

import com.sun.net.httpserver.HttpServer;
import io.ember.enums.HttpStatusCode;
import io.ember.enums.MediaType;
import io.ember.enums.RequestHeader;
import io.ember.exceptions.HttpException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class Server {

    private final Router router;
    private final List<Middleware> middleware;

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
     * The server uses a cached thread pool executor to handle requests asynchronously.
     *
     * @param port The port number on which the server will listen for incoming requests.
     */
    public void start(int port) {

        try{
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            // Setting the custom executor for handling requests asynchronously
            server.setExecutor(Executors.newCachedThreadPool());

            // Setting up the context for handling requests
            server.createContext("/", exchange -> {
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
                    exchange.sendResponseHeaders(e.getStatus().getCode(), 0);
                    exchange.getResponseBody().write(e.getMessage().getBytes());
                    exchange.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    HttpStatusCode internalServerError = HttpStatusCode.INTERNAL_SERVER_ERROR;
                    exchange.sendResponseHeaders(internalServerError.getCode(), 0);
                    exchange.getResponseBody().write(HttpStatusCode.INTERNAL_SERVER_ERROR.getMessage().getBytes());
                    exchange.close();
                }

            });

            server.start();
            System.out.println("==== Server started on port " + port + " ====");
        }catch (IOException e){
            e.printStackTrace();
            System.out.println("==== Failed to start server on port " + port + " ====");
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
        RouteMatchResult match = router.getRoute(context.getMethod(), context.getPath());

        List<Middleware> fullChain = new ArrayList<>(middleware);
        if(match != null) {
            context.pathParams().setPathParams(match.parameters());
            fullChain.addAll(match.middlewareChain().middleware());
            fullChain.add(c -> match.middlewareChain().handler().accept(c));
        } else {
            fullChain.add(c -> {
                c.response().send(HttpStatusCode.NOT_FOUND.getMessage(), HttpStatusCode.NOT_FOUND.getCode());
            });
        }

        return fullChain;
    }

}
