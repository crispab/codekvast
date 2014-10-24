package se.crisp.codekvast.agent.collector.aspects;

/**
 * Abstract base aspect providing some reusable pointcuts.
 *
 * @author Olle Hallin
 */
abstract aspect AbstractCodekvastAspect {

    pointcut withinCodekvast(): within(se.crisp.codekvast.agent..*);

}
