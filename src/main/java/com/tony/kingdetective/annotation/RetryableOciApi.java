package com.tony.kingdetective.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  OCI API 
 * 
 * 
 * @author Tony Wang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryableOciApi {
    /**
     * 
     */
    int maxAttempts() default 3;
    
    /**
     * 
     */
    long delayMs() default 1000;
}
