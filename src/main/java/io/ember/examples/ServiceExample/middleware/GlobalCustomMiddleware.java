package io.ember.examples.ServiceExample.middleware;

import io.ember.core.Context;
import io.ember.core.Middleware;

public class GlobalCustomMiddleware implements Middleware {

    @Override
    public void handle(Context context) throws Exception {
        // Custom logic before the request is processed
        System.out.println("GlobalCustomMiddleware: Before processing request");
        System.out.println("==============================================");
        // You can access request parameters, headers, etc. from the context
        System.out.println("Request Method: " + context.getMethod());
        System.out.println("Request Path: " + context.getPath());
        System.out.println("Request Headers: " + context.headers().headers());
        // Call the next middleware in the chain
        context.next();

    }
}
