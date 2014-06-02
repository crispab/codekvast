package se.crisp.duck.agent.sensor;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * This is an AspectJ aspect that captures execution of public methods in the scope of interest.
 * <p/>
 * It is weaved into the target app by the AspectJ load-time weaver.
 *
 * @author Olle Hallin
 * @see DuckSensor
 */
@Aspect
public abstract class AbstractMethodExecutionAspect extends AbstractDuckAspect {

    /**
     * This abstract pointcut specifies the scope for what method executions to detect.
     * <p/>
     * It is made concrete by an XML file that is created on-the-fly by {@link DuckSensor} before loading
     * the AspectJ load-time weaving agent.
     */
    @Pointcut
    public abstract void scope();

    @Pointcut("execution(public * *..*(..))")
    private void publicMethodExecution() {
    }

    /**
     * Register that this method has been invoked.
     *
     * @param jp The join point
     */
    @Before("scope() && publicMethodExecution() && !withinDuck()")
    public void recordMethodCall(JoinPoint jp) {
        UsageRegistry.instance.registerMethodExecution(jp.getSignature());
    }

}
