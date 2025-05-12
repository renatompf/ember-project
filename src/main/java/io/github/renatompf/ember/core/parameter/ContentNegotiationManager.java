package io.github.renatompf.ember.core.parameter;

import io.github.renatompf.ember.annotations.content.Consumes;
import io.github.renatompf.ember.annotations.content.Produces;
import io.github.renatompf.ember.core.server.Context;
import io.github.renatompf.ember.enums.HttpStatusCode;
import io.github.renatompf.ember.enums.MediaType;
import io.github.renatompf.ember.exceptions.HttpException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * ContentNegotiationManager is responsible for handling content negotiation
 * in HTTP requests and responses.
 * <p>
 * It validates the Content-Type of incoming requests and negotiates the
 * appropriate response type based on the Accept header.
 * </p>
 */
public class ContentNegotiationManager {

    /**
     * Validates the Content-Type of the incoming request against the
     * supported types specified in the method's @Consumes annotation.
     *
     * @param context The context of the HTTP request.
     * @param method  The method being invoked.
     * @throws HttpException if the Content-Type is not supported.
     */
    public void validateContentType(Context context, Method method) {
        String contentType = context.headers().header("Content-Type");
        Consumes consumesAnnotation = method.getAnnotation(Consumes.class);
        
        if (consumesAnnotation != null && contentType != null) {
            MediaType requestContentType = MediaType.fromString(contentType);
            if (requestContentType == null || 
                !Arrays.asList(consumesAnnotation.value()).contains(requestContentType)) {
                throw new HttpException(
                    HttpStatusCode.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported Media Type: " + contentType
                );
            }
        }
    }

    /**
     * Negotiates the response type based on the Accept header in the request
     * and the supported types specified in the method's @Produces annotation.
     *
     * @param context The context of the HTTP request.
     * @param method  The method being invoked.
     * @return The negotiated MediaType for the response.
     * @throws HttpException if none of the supported media types are acceptable.
     */
    public MediaType negotiateResponseType(Context context, Method method) {
        Produces producesAnnotation = method.getAnnotation(Produces.class);
        if (producesAnnotation == null) {
            return MediaType.APPLICATION_JSON; // default
        }

        String accept = context.headers().header("Accept");
        if (accept == null || accept.isEmpty() || accept.equals("*/*")) {
            return producesAnnotation.value()[0];
        }

        List<MediaType> supported = Arrays.asList(producesAnnotation.value());
        for (String acceptType : accept.split(",")) {
            MediaType mediaType = MediaType.fromString(acceptType.trim());
            if (mediaType != null && supported.contains(mediaType)) {
                return mediaType;
            }
        }

        throw new HttpException(
            HttpStatusCode.NOT_ACCEPTABLE,
            "None of the supported media types are acceptable"
        );
    }
}
