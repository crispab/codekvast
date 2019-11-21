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
package io.codekvast.dashboard.dashboard.model.methods;

import lombok.*;

/**
 * @author olle.hallin@crisp.se
 */
// Cannot use @Value here, since that will prohibit computed fields.
@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods"})
@Data
@Setter(value = AccessLevel.NONE)
@Builder(toBuilder = true)
public class MethodDescriptor2 {

    @NonNull
    private final Long id;

    @NonNull
    private final String signature;

    @NonNull
    private final Integer trackedPercent;

    @NonNull
    private final Integer collectedDays;

    @NonNull
    private Long lastInvokedAtMillis;

    @NonNull
    private Long collectedToMillis;
}
