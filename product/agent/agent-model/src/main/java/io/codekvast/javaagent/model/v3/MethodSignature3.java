/*
 * Copyright (c) 2015-2021 Hallin Information Technology AB
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
package io.codekvast.javaagent.model.v3;

import io.codekvast.javaagent.model.v2.MethodSignature2;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

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
public class MethodSignature3 implements Serializable {
  private static final long serialVersionUID = 3L;

  @NonNull private final String aspectjString;
  private final Boolean bridge;
  @NonNull private final String declaringType;
  @NonNull private final String exceptionTypes;
  @NonNull private final String methodName;
  @NonNull private final String modifiers;
  @NonNull private final String packageName;
  @NonNull private final String parameterTypes;
  @NonNull private final String returnType;
  private final Boolean synthetic;
  private final String location;

  public static MethodSignature3 createSampleMethodSignature() {
    return builder()
        .aspectjString("aspectjString")
        .bridge(false)
        .declaringType("declaringType")
        .exceptionTypes("exceptionTypes")
        .methodName("methodName")
        .modifiers("modifiers")
        .packageName("packageName")
        .parameterTypes("parameterTypes")
        .returnType("returnType")
        .synthetic(false)
        .location("location")
        .build();
  }

  public static MethodSignature3 fromFormat2(MethodSignature2 methodSignature2) {
    return builder()
        .aspectjString(methodSignature2.getAspectjString())
        .bridge(methodSignature2.getBridge())
        .declaringType(methodSignature2.getDeclaringType())
        .exceptionTypes(methodSignature2.getExceptionTypes())
        .methodName(methodSignature2.getMethodName())
        .modifiers(methodSignature2.getModifiers())
        .packageName(methodSignature2.getPackageName())
        .parameterTypes(methodSignature2.getParameterTypes())
        .returnType(methodSignature2.getReturnType())
        .synthetic(methodSignature2.getSynthetic())
        .location(null)
        .build();
  }
}
