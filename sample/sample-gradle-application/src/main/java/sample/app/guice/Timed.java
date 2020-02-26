package sample.app.guice;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a method for collection of time metrics by guice aop.
 *
 * @author olle.hallin@crisp.se
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Timed {}
