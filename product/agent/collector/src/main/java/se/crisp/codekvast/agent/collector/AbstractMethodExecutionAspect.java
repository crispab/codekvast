package se.crisp.codekvast.agent.collector;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * This is an AspectJ aspect that captures execution of methods in the scope of interest.
 * <p>
 * It is weaved into the target app by the AspectJ load-time weaver.
 *
 * @author olle.hallin@crisp.se
 * @see CodekvastCollector
 */
@Aspect
public abstract class AbstractMethodExecutionAspect {

    /**
     * This abstract pointcut specifies what method executions to detect.
     * <p>
     * It is made concrete by an XML file that is created on-the-fly by {@link CodekvastCollector} before
     * loading the AspectJ load-time weaving agent.
     */
    @Pointcut
    public abstract void methodExecution();

    @Pointcut("execution(int compareTo(Object)) " +
            "|| execution(boolean equals(Object)) " +
            "|| execution(* get*()) " +
            "|| execution(int hashCode()) " +
            "|| execution(void set*(*)) " +
            "|| execution(String toString()) ")
    public void trivialMethodExecution() {
    }

    @Before("methodExecution() && !trivialMethodExecution()")
    public void registerInvokation(JoinPoint thisJointPoint) {
        if (InvocationRegistry.instance != null) {
            InvocationRegistry.instance.registerMethodInvocation(thisJointPoint.getSignature());
        }
    }

}
