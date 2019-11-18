/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
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
package io.codekvast.backoffice.facts;

import io.codekvast.common.messaging.model.AgentPolledEvent;
import io.codekvast.common.messaging.model.CollectionStartedEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.time.Instant;

/**
 * @author olle.hallin@crisp.se
 */
@Data
@AllArgsConstructor
public class CollectionStarted implements PersistentFact {
    @NonNull final Instant collectionStartedAt;
    final Instant trialPeriodEndsAt;
    String welcomeMailSentTo;
    Instant welcomeMailSentAt;

    public static CollectionStarted of (CollectionStartedEvent event) {
        return new CollectionStarted(event.getCollectionStartedAt(), event.getTrialPeriodEndsAt(), null, null);
    }

    public static CollectionStarted of (AgentPolledEvent event) {
        return new CollectionStarted(event.getPolledAt(), event.getTrialPeriodEndsAt(), null, null);
    }
}
