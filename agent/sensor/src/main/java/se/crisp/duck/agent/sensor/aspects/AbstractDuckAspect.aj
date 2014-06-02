package se.crisp.duck.agent.sensor.aspects;

/**
 * Abstract base aspect providing some reusable pointcuts.
 *
 * @author Olle Hallin
 */
abstract aspect AbstractDuckAspect {

    pointcut withinDuck(): within(se.crisp.duck.agent..*);

}
