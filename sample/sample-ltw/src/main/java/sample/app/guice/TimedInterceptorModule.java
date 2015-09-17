package sample.app.guice;

import com.google.inject.AbstractModule;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

/**
 * @author olle.hallin@crisp.se
 */
public class TimedInterceptorModule extends AbstractModule {

    @Override
    protected void configure() {
        bindInterceptor(any(), annotatedWith(Timed.class), new TimedInterceptor());
    }
}
