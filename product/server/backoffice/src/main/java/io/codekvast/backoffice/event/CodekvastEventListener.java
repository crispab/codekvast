/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.backoffice.event;

import io.codekvast.backoffice.rules.RuleEngine;
import io.codekvast.common.messaging.AbstractCodekvastEventListener;
import io.codekvast.common.messaging.impl.MessageIdRepository;
import io.codekvast.common.messaging.model.CodekvastEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** @author olle.hallin@crisp.se */
@Component
@Slf4j
public class CodekvastEventListener extends AbstractCodekvastEventListener {

  private final RuleEngine ruleEngine;

  public CodekvastEventListener(MessageIdRepository messageIdRepository, RuleEngine ruleEngine) {
    super(messageIdRepository);
    this.ruleEngine = ruleEngine;
  }

  @Override
  public void onCodekvastEvent(CodekvastEvent event) {
    logger.debug("Received {}", event);
    ruleEngine.handle(event);
  }
}
