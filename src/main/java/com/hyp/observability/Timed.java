package com.hyp.observability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically time method execution.
 * Metrics will be recorded with the specified name and tags.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {

    /**
     * Name for the timer metric. Defaults to class.method name if not specified.
     */
    String value() default "";

    /**
     * Description for the metric
     */
    String description() default "";

    /**
     * Whether to record percentiles (p50, p75, p95, p99)
     */
    boolean percentiles() default true;

    /**
     * Extra tags to add to the metric in key=value format
     */
    String[] extraTags() default {};
}
