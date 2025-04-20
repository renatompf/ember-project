package io.ember.core;

import io.ember.EmberApplication;
import io.ember.annotations.*;

import java.lang.reflect.Method;

public class RouteScanner {

    public static void scanRoutes(EmberApplication app, Object... controllers) {
        for (Object controller : controllers) {
            Class<?> controllerClass = controller.getClass();
            Method[] methods = controllerClass.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(Get.class)) {
                    String path = method.getAnnotation(Get.class).value();
                    app.get(path, context -> invokeMethod(controller, method, context));
                } else if (method.isAnnotationPresent(Post.class)) {
                    String path = method.getAnnotation(Post.class).value();
                    app.post(path, context -> invokeMethod(controller, method, context));
                } else if (method.isAnnotationPresent(Put.class)) {
                    String path = method.getAnnotation(Put.class).value();
                    app.put(path, context -> invokeMethod(controller, method, context));
                } else if (method.isAnnotationPresent(Delete.class)) {
                    String path = method.getAnnotation(Delete.class).value();
                    app.delete(path, context -> invokeMethod(controller, method, context));
                } else if (method.isAnnotationPresent(Patch.class)) {
                    String path = method.getAnnotation(Patch.class).value();
                    app.patch(path, context -> invokeMethod(controller, method, context));
                } else if (method.isAnnotationPresent(Options.class)) {
                    String path = method.getAnnotation(Options.class).value();
                    app.options(path, context -> invokeMethod(controller, method, context));
                } else if (method.isAnnotationPresent(Head.class)) {
                    String path = method.getAnnotation(Head.class).value();
                    app.head(path, context -> invokeMethod(controller, method, context));
                }
            }
        }
    }

    private static void invokeMethod(Object controller, Method method, Context context){
        try {
            method.invoke(controller, context);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method: " + method.getName(), e);
        }
    }

}
