package sample.aspect;

import io.codekvast.javaagent.AbstractMethodExecutionAspect;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/** Example that shows how to make methodExecution() concrete */
@Aspect
public class MethodExecutionAspect extends AbstractMethodExecutionAspect {

  @Pointcut("execution(* *..*(..)) && within(sample..*)")
  @Override
  public void methodExecution() {}
}
