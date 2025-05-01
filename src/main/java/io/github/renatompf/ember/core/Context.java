package io.github.renatompf.ember.core;

import com.sun.net.httpserver.HttpExchange;
import io.github.renatompf.ember.enums.HttpMethod;

import java.util.List;
import java.util.Map;

/**
 * Represents the context of an HTTP request, providing access to request data,
 * response handling, and middleware execution.
 */
public class Context {
    private final HttpExchange exchange;
    private final HeadersManager headersManager;
    private final PathParameterManager pathParameterManager;
    private final QueryParameterManager queryParameterManager;
    private final BodyManager bodyManager;
    private final CookieManager cookieManager;
    private final ResponseHandler responseHandler;
    private final SessionManager sessionManager;
    private List<Middleware> middlewareChain;
    private int middlewareIndex = -1;

    /**
     * Constructs a new Context instance.
     *
     * @param exchange    The underlying HTTP exchange object.
     * @param query       The query string of the request.
     * @param body        The body of the request.
     * @param contentType The content type of the request body.
     * @param pathParams  The path parameters extracted from the request URI.
     */
    public Context(HttpExchange exchange, String query, String body, String contentType, Map<String, String> pathParams) {
        this.exchange = exchange;
        this.headersManager = new HeadersManager(exchange);
        this.pathParameterManager = new PathParameterManager(pathParams);
        this.queryParameterManager = new QueryParameterManager(query);
        this.cookieManager = new CookieManager(exchange);
        this.responseHandler = new ResponseHandler(exchange);
        this.sessionManager = new SessionManager();
        this.bodyManager = new BodyManager(body, contentType);
    }

    /**
     * Retrieves the HTTP method of the request.
     *
     * @return The HTTP method as an {@link HttpMethod}.
     */
    public HttpMethod getMethod() {
        return HttpMethod.fromString(exchange.getRequestMethod().toUpperCase());
    }

    /**
     * Retrieves the path of the request URI.
     *
     * @return The request path as a string.
     */
    public String getPath() {
        return exchange.getRequestURI().getPath();
    }

    /**
     * Provides access to the cookie manager for managing cookies.
     *
     * @return The {@link CookieManager} instance.
     */
    public CookieManager cookies() {
        return cookieManager;
    }

    /**
     * Provides access to the response handler for sending responses.
     *
     * @return The {@link ResponseHandler} instance.
     */
    public ResponseHandler response() {
        return responseHandler;
    }

    /**
     * Provides access to the session manager for managing user sessions.
     *
     * @return The {@link SessionManager} instance.
     */
    public SessionManager session() {
        return sessionManager;
    }

    /**
     * Proceeds to the next middleware in the chain.
     *
     * @throws Exception If an error occurs during middleware execution.
     */
    public void next() throws Exception {
        middlewareIndex++;
        if (middlewareIndex < middlewareChain.size()) {
            middlewareChain.get(middlewareIndex).handle(this);
        }
    }

    /**
     * Sets the middleware chain for this context.
     *
     * @param middlewareChain The list of middleware to execute.
     */
    void setMiddlewareChain(List<Middleware> middlewareChain) {
        this.middlewareChain = middlewareChain;
        this.middlewareIndex = -1;
    }

    /**
     * Provides access to the headers manager for managing request headers.
     *
     * @return The {@link HeadersManager} instance.
     */
    public HeadersManager headers() {
        return headersManager;
    }

    /**
     * Provides access to the path parameter manager for retrieving path parameters.
     *
     * @return The {@link PathParameterManager} instance.
     */
    public PathParameterManager pathParams() {
        return pathParameterManager;
    }

    /**
     * Provides access to the query parameter manager for retrieving query parameters.
     *
     * @return The {@link QueryParameterManager} instance.
     */
    public QueryParameterManager queryParams() {
        return queryParameterManager;
    }

    /**
     * Provides access to the body manager for parsing the request body.
     *
     * @return The {@link BodyManager} instance.
     */
    public BodyManager body() {
        return bodyManager;
    }
}