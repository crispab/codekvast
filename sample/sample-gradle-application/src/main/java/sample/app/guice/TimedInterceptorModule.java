package sample.app.guice;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

import com.google.inject.AbstractModule;

/** @author olle.hallin@crisp.se */
class TimedInterceptorModule extends AbstractModule {

  @Override
  protected void configure() {
    bindInterceptor(any(), annotatedWith(Timed.class), new TimedInterceptor());
  }
}
