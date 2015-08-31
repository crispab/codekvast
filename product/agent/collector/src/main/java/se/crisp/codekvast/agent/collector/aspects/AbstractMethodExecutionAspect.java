package se.crisp.codekvast.agent.collector.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import se.crisp.codekvast.agent.collector.InvocationRegistry;

/**
 * This is an AspectJ aspect that captures execution of methods in the scope of interest.
 * <p>
 * It is weaved into the target app by the AspectJ load-time weaver.
 *
 * @author olle.hallin@crisp.se
 * @see se.crisp.codekvast.agent.collector.CodekvastCollector
 */
@Aspect
public abstract class AbstractMethodExecutionAspect {

    /**
     * This abstract pointcut specifies what method executions to detect.
     * <p>
     * It is made concrete by an XML file that is created on-the-fly by {@link se.crisp.codekvast.agent.collector.CodekvastCollector} before
     * loading the AspectJ load-time weaving agent.
     */
    @Pointcut
    abstract void methodExecution();

    @Pointcut("execution(int compareTo(Object)) " +
            "|| execution(boolean equals(Object)) " +
            "|| execution(* get*()) " +
            "|| execution(int hashCode()) " +
            "|| execution(void set*(*)) " +
            "|| execution(String toString()) ")
    public void trivialMethodExecution() {
    }

    ;
    ;

    @Before("methodExecution() && !trivialMethodExecution()")
    public void registerInvokation(JoinPoint thisJointPoint) {
        InvocationRegistry.instance.registerMethodInvocation(thisJointPoint.getSignature());
    }

}
