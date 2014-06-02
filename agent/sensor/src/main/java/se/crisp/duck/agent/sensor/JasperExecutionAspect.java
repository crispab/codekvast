package se.crisp.duck.agent.sensor;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * This is an AspectJ aspect that captures execution of a JSP page compiled by Apache Jasper.
 * <p/>
 * It is weaved into the target app by the AspectJ load-time weaver.
 *
 * @author Olle Hallin
 * @see DuckSensor
 */
@Aspect
public class JasperExecutionAspect extends AbstractDuckAspect {

    @Pointcut("execution(public void org.apache.jsp.._jspService(..))")
    public void jasperPageExecution() {
    }

    /**
     * Register that this JSP page has been invoked.
     *
     * @param jp The join point
     */
    @Before("jasperPageExecution() && !withinDuck()")
    public void recordJspInvocation(JoinPoint jp) {
        UsageRegistry.instance.registerJspPageExecution(getJspPageName(jp));
    }

    private String getJspPageName(JoinPoint jp) {
        // TODO: implement getJspPageName(JoinPoint)
        return "foobar.jsp";
    }
}
