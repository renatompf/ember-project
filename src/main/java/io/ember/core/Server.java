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
        this.middleware = middleware;
    }

    public void start(int port) {

        try{
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Setting the custom executor for handling requests asynchronously
            server.setExecutor(Executors.newCachedThreadPool());

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
