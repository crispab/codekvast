package se.crisp.duck.agent.sensor;

import org.aspectj.lang.annotation.Pointcut;

/**
 * Abstract base aspect providing some reusable pointcuts.
 *
 * @author Olle Hallin
 */
abstract class AbstractDuckAspect {

    @Pointcut("within(se.crisp.duck.agent..*)")
    void withinDuck() {
    }

}
