package sample.app.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import se.crisp.codekvast.collector.AbstractMethodExecutionAspect;

@Aspect
public class MethodExecutionAspect extends AbstractMethodExecutionAspect {

    @Pointcut("execution(* *..*(..)) && within(sample..*)")
    @Override
    public void methodExecution() {
    }
}
