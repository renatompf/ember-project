package io.ember.examples.AuthenticationExample.middleware;

import io.ember.core.Context;
import io.ember.core.Middleware;
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

public class AuthMiddleware implements Middleware {
    private final SecretKey secretKey;

    public AuthMiddleware(String secretKey) {
        // Ensure the key is at least 256 bits (32 bytes)
        if (secretKey.length() < 32) {
            throw new IllegalArgumentException("Secret key must be at least 32 characters long.");
        }
        this.secretKey = null; // Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public void handle(Context ctx) {
        String authHeader = ctx.headers().header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.response().unauthorized("Unauthorized: Missing or invalid token");
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix
        try {
//            Claims claims = Jwts.parser()
//                    .verifyWith(secretKey)
//                    .build()
//                    .parseSignedClaims(token)
//                    .getPayload();

//            System.out.println("Token claims: " + claims);
            ctx.next();
        } catch (Exception e) {
            ctx.response().unauthorized("Unauthorized: Invalid token");
        }
    }
}