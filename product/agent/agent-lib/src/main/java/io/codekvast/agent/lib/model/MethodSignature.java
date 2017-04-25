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
package io.codekvast.agent.lib.model;

import lombok.*;

/**
 * Immutable representation of a method signature.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString(of = "aspectjString")
@EqualsAndHashCode(of = "aspectjString")
public class MethodSignature {
    @NonNull
    private final String aspectjString;
    @NonNull
    private final String declaringType;
    @NonNull
    private final String exceptionTypes;
    @NonNull
    private final String methodName;
    @NonNull
    private final String modifiers;
    @NonNull
    private final String packageName;
    @NonNull
    private final String parameterTypes;
    @NonNull
    private final String returnType;

    public static MethodSignature createSampleMethodSignature() {
        return builder()
                .aspectjString("aspectjString")
                .declaringType("declaringType")
                .exceptionTypes("exceptionTypes")
                .methodName("methodName")
                .modifiers("modifiers")
                .packageName("packageName")
                .parameterTypes("parameterTypes")
                .returnType("returnType")
                .build();
    }
}