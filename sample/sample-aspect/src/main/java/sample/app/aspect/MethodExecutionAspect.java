package sample.app.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import se.crisp.codekvast.collector.AbstractMethodExecutionAspect;

/**
 * Example that shows how to make methodExecution() concrete
 */
@Aspect
public class MethodExecutionAspect extends AbstractMethodExecutionAspect {

    @Pointcut("execution(* *..*(..)) && within(sample..*)")
    @Override
    public void methodExecution() {
    }
}
