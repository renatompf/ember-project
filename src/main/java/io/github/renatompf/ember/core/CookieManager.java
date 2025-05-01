package io.github.renatompf.ember.core;

import com.sun.net.httpserver.HttpExchange;

/**
 * Manages HTTP cookies for requests and responses.
 * Provides methods to retrieve, set, and delete cookies.
 */
public class CookieManager {
    private final HttpExchange exchange;

    /**
     * Constructs a new CookieManager instance.
     *
     * @param exchange The HTTP exchange object associated with the request and response.
     */
    public CookieManager(HttpExchange exchange) {
        this.exchange = exchange;
    }

    /**
     * Retrieves the value of a cookie by its name.
     *
     * @param name The name of the cookie to retrieve.
     * @return The value of the cookie, or {@code null} if the cookie does not exist.
     */
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

    /**
     * Sets a cookie with the specified name, value, and maximum age.
     *
     * @param name   The name of the cookie.
     * @param value  The value of the cookie.
     * @param maxAge The maximum age of the cookie in seconds.
     */
    public void setCookie(String name, String value, int maxAge) {
        String cookie = name + "=" + value + "; Max-Age=" + maxAge + "; Path=/";
        exchange.getResponseHeaders().add("Set-Cookie", cookie);
    }

    /**
     * Deletes a cookie by setting its value to an empty string and its maximum age to zero.
     *
     * @param name The name of the cookie to delete.
     */
    public void deleteCookie(String name) {
        setCookie(name, "", 0);
    }
}