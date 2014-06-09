package sample.app.guice;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method for collection of time metrics by guice aop.
 *
 * @author Olle Hallin
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Timed {
}
