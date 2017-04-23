package sample.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import io.codekvast.agent.collector.AbstractMethodExecutionAspect;

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
