package se.crisp.codekvast.agent.collector.aspects;

/**
 * Abstract base aspect providing some reusable pointcuts.
 *
 * @author olle.hallin@crisp.se
 */
abstract aspect AbstractCodekvastAspect {

    pointcut withinCodekvast(): within(se.crisp.codekvast.agent..*);

}
