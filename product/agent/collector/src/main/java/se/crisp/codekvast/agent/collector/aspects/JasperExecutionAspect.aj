package se.crisp.codekvast.agent.collector.aspects;

import org.aspectj.lang.JoinPoint;
import se.crisp.codekvast.agent.collector.UsageRegistry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is an AspectJ aspect that captures execution of a JSP page compiled by Apache Jasper.
 * <p/>
 * It is weaved into the target app by the AspectJ load-time weaver.
 *
 * @author Olle Hallin
 * @see se.crisp.codekvast.agent.collector.CodekvastCollector
 */
public aspect JasperExecutionAspect extends AbstractCodekvastAspect {

    public static final String JASPER_BASE_PACKAGE = "org.apache.jsp";

    private static final Pattern JSP_NAME_PATTERN = Pattern.compile(JASPER_BASE_PACKAGE.replace(".", "\\.") + "(\\..*)_jsp");

    pointcut jasperPageExecution(): execution(public void org.apache.jsp.._jspService(*, *));

    /**
     * Register that this JSP page has been invoked.
     */
    before(): jasperPageExecution() && !withinCodekvast() {
        UsageRegistry.instance.registerJspPageExecution(getJspPageName(thisJoinPoint));
    }

    private String getJspPageName(JoinPoint jp) {
        String declaringTypeName = jp.getSignature().getDeclaringTypeName();
        Matcher m = JSP_NAME_PATTERN.matcher(declaringTypeName);
        String result = m.matches() ? m.group(1) : declaringTypeName;
        return result.replace(".", "/") + ".jsp";
    }
}
