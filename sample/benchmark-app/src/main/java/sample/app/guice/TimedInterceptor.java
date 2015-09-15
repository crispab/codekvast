package sample.app.guice;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author Per Huss, Diabol (qpehu)
 */
@Slf4j
public class TimedInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        long startedAt = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            log.debug("Elapsed: {}, method: {}", System.currentTimeMillis() - startedAt, invocation.getMethod());
        }
    }
}
