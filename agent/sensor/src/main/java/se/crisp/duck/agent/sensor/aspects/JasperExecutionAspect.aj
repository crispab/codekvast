package se.crisp.duck.agent.sensor.aspects;

import org.aspectj.lang.JoinPoint;
import se.crisp.duck.agent.sensor.UsageRegistry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is an AspectJ aspect that captures execution of a JSP page compiled by Apache Jasper.
 * <p/>
 * It is weaved into the target app by the AspectJ load-time weaver.
 *
 * @author Olle Hallin
 * @see se.crisp.duck.agent.sensor.DuckSensor
 */
public aspect JasperExecutionAspect extends AbstractDuckAspect {

    public static final String JASPER_BASE_PACKAGE = "org.apache.jsp";

    private static final Pattern JSP_NAME_PATTERN = Pattern.compile(JASPER_BASE_PACKAGE.replace(".", "\\.") + "\\.(.*)_jsp");

    pointcut jasperPageExecution(): execution(public void org.apache.jsp.._jspService(javax.servlet.http.HttpServletRequest, javax .servlet.http.HttpServletResponse));

    /**
     * Register that this JSP page has been invoked.
     */
    before(): jasperPageExecution() && !withinDuck() {
        UsageRegistry.instance.registerJspPageExecution(getJspPageName(thisJoinPoint));
    }

    private String getJspPageName(JoinPoint jp) {
        Matcher m = JSP_NAME_PATTERN.matcher(jp.getSignature().getDeclaringTypeName());
        String result = m.matches() ? m.group(1) : jp.getSignature().getDeclaringTypeName();
        return result + ".jsp";
    }
}
