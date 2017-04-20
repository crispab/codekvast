/*
 * Copyright (c) 2015-2017 Crisp AB
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

import org.aspectj.bridge.AbortException;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.IMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.aspectj.bridge.IMessage.*;

/**
 * A bridge from AspectJ's IMessageHandler to SLF4J
 */
public class AspectjMessageHandlerToSLF4JBridge implements IMessageHandler {
    public static final String LOGGER_NAME = "se.crisp.codekvast.aspectjweaver";

    private Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    @Override
    public boolean handleMessage(IMessage message) throws AbortException {
        if (message.isDebug()) {
            logger.debug("{}", message.getMessage());
            return true;
        }
        if (message.isInfo()) {
            logger.info("{}", message.getMessage());
            return true;
        }
        if (message.isWarning()) {
            logger.warn("{}", message.getMessage());
            return true;
        }
        if (message.isError()) {
            logger.error("{}", message.getMessage());
            return true;
        }
        if (message.getKind() == WEAVEINFO) {
            logger.debug("{}", message.getMessage());
            return true;
        }
        return false;
    }

    @Override
    public boolean isIgnoring(Kind kind) {
        return false;
    }

    @Override
    public void dontIgnore(Kind kind) {

    }

    @Override
    public void ignore(Kind kind) {

    }
}
