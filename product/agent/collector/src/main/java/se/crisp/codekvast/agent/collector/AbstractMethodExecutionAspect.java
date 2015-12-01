/**
 * Copyright (c) 2015 Crisp AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.crisp.codekvast.agent.collector;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * This is an AspectJ aspect that captures execution of methods in the scope of interest.
 * <p>
 * It is weaved into the target app by the AspectJ load-time weaver.
 *
 * @author olle.hallin@crisp.se
 * @see CodekvastCollector
 */
@Aspect
public abstract class AbstractMethodExecutionAspect {

    /**
     * This abstract pointcut specifies what method executions to detect.
     * <p>
     * It is made concrete by an XML file that is created on-the-fly by {@link CodekvastCollector} before
     * loading the AspectJ load-time weaving agent.
     */
    @Pointcut
    public abstract void methodExecution();

    @SuppressWarnings("NoopMethodInAbstractClass")
    @Pointcut("execution(int compareTo(Object)) " +
            "|| execution(boolean equals(Object)) " +
            "|| execution(* get*()) " +
            "|| execution(int hashCode()) " +
            "|| execution(void set*(*)) " +
            "|| execution(String toString()) ")
    public void trivialMethodExecution() {
    }

    @Before("methodExecution() && !trivialMethodExecution()")
    public void registerInvokation(JoinPoint thisJointPoint) {
        if (InvocationRegistry.instance != null) {
            InvocationRegistry.instance.registerMethodInvocation(thisJointPoint.getSignature());
        }
    }

}
