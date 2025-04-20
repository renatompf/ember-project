package io.ember.core;

import com.sun.net.httpserver.HttpExchange;
import io.ember.enums.HttpMethod;

import java.util.List;
import java.util.Map;

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


    public HttpMethod getMethod() {
        return HttpMethod.fromString(exchange.getRequestMethod().toUpperCase());
    }

    public String getPath() {
        return exchange.getRequestURI().getPath();
    }

    public CookieManager cookies() {
        return cookieManager;
    }

    public ResponseHandler response() {
        return responseHandler;
    }

    public SessionManager session() {
        return sessionManager;
    }

    public void next() throws Exception {
        middlewareIndex++;
        if (middlewareIndex < middlewareChain.size()) {
            middlewareChain.get(middlewareIndex).handle(this);
        }
    }

    void setMiddlewareChain(List<Middleware> middlewareChain) {
        this.middlewareChain = middlewareChain;
        this.middlewareIndex = -1;
    }

    public HeadersManager headers() {
        return headersManager;
    }

    public PathParameterManager pathParams() {
        return pathParameterManager;
    }

    public QueryParameterManager queryParams() {
        return queryParameterManager;
    }

    public BodyManager body() {
        return bodyManager;
    }
}