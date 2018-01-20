/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
package io.codekvast.dashboard.webapp.model.status;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("ClassWithTooManyFields")
@Value
@Builder
public class AgentDescriptor {

    @NonNull
    private final Long id;

    @NonNull
    private final String appName;

    @NonNull
    private final String appVersion;

    @NonNull
    private final String agentVersion;

    @NonNull
    private final String packages;

    private final String excludePackages;

    private final String environment;

    @NonNull
    private final String tags;

    /**
     * public, protected, package-private or private
     */
    @NonNull
    private final String methodVisibility;

    @NonNull
    private final Long startedAtMillis;

    @NonNull
    private final Long publishedAtMillis;

    /**
     * When did we hear from this agent?
     */
    @NonNull
    private final Long pollReceivedAtMillis;

    /**
     * When will we hear again from this agent?
     */
    @NonNull
    private final Long nextPollExpectedAtMillis;

    /**
     * When will we get data again from this agent?
     */
    @NonNull
    private final Long nextPublicationExpectedAtMillis;

    /**
     * Is this agent alive?
     */
    private final boolean agentAlive;

    /**
     * Is this agent live and enabled?
     */
    private final boolean agentLiveAndEnabled;
}
