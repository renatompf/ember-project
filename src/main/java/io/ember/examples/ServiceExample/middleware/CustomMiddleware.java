package io.ember.examples.ServiceExample.middleware;

import io.ember.core.Context;
import io.ember.core.Middleware;

public class CustomMiddleware implements Middleware {

    @Override
    public void handle(Context context) throws Exception {
        // Custom logic before the request is processed
        System.out.println("CustomMiddleware: Before processing request");
        System.out.println("==============================================");
        // Call the next middleware in the chain
        context.next();

    }
}
