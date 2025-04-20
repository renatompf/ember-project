package io.ember.core;

import com.sun.net.httpserver.HttpExchange;

public class CookieManager {
    private final HttpExchange exchange;

    public CookieManager(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public String cookie(String name) {
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookies != null) {
            for (String cookie : cookies.split(";")) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts[0].equals(name)) {
                    return parts.length > 1 ? parts[1] : "";
                }
            }
        }
        return null;
    }

    public void setCookie(String name, String value, int maxAge) {
        String cookie = name + "=" + value + "; Max-Age=" + maxAge + "; Path=/";
        exchange.getResponseHeaders().add("Set-Cookie", cookie);
    }

    public void deleteCookie(String name) {
        setCookie(name, "", 0);
    }
}