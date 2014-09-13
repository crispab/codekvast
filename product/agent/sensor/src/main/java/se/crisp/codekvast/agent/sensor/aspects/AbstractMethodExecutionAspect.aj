package se.crisp.codekvast.agent.sensor.aspects;

import se.crisp.codekvast.agent.sensor.UsageRegistry;

/**
 * This is an AspectJ aspect that captures execution of public methods in the scope of interest.
 * <p/>
 * It is weaved into the target app by the AspectJ load-time weaver.
 *
 * @author Olle Hallin
 * @see se.crisp.codekvast.agent.sensor.CodeKvastSensor
 */
public abstract aspect AbstractMethodExecutionAspect extends AbstractCodeKvastAspect {

    /**
     * This abstract pointcut specifies the scope for what method executions to detect.
     * <p/>
     * It is made concrete by an XML file that is created on-the-fly by {@link se.crisp.codekvast.agent.sensor.CodeKvastSensor} before
     * loading
     * the AspectJ load-time weaving agent.
     */
    public abstract pointcut scope();

    private pointcut publicMethodExecution(): execution(public * *..*(..));

    /**
     * Register that this method has been invoked.
     */
    before(): scope() && publicMethodExecution() && !withinCodeKvast() {
        UsageRegistry.instance.registerMethodExecution(thisJoinPoint.getSignature());
    }

}
