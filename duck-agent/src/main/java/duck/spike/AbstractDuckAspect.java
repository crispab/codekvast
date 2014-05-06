package duck.spike;

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
 * @see duck.spike.DuckAgent
 */
@Aspect
public abstract class AbstractDuckAspect {

    /**
     * This abstract pointcut specifies the scope for what dead code to detect.
     * <p/>
     * It is concreted in an XML file, which is created on-the-fly by {@link duck.spike.DuckAgent} before delegating
     * to the AspectJ load-time weaving agent.
     */
    @Pointcut
    public abstract void scope();

    @Pointcut("execution(* *..*(..))")
    private void methodExecution() {
    }

    /**
     * Register that this method has been invoked.
     *
     * @param jp The join point
     */
    @Before("scope() && methodExecution()")
    public void recordMethodCall(JoinPoint jp) {
        UsageRegistry.registerMethodExecution(jp.getSignature());
    }

}
