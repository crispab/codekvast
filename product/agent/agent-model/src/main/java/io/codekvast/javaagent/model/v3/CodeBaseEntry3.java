/*
 * Copyright (c) 2015-2022 Hallin Information Technology AB
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

import io.codekvast.javaagent.model.v2.CodeBaseEntry2;
import java.io.Serializable;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Representation of a code base entry.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder(toBuilder = true)
public class CodeBaseEntry3 implements Serializable {
  private static final long serialVersionUID = 1L;

  /** The low-level description of the signature. */
  private final MethodSignature3 methodSignature;

  /** The visibility of the signature. Package private is coded as 'package-private'. */
  @NonNull private final String visibility;

  /** The signature. */
  @NonNull private final String signature;

  public static CodeBaseEntry3 fromFormat2(CodeBaseEntry2 codeBaseEntry2) {
    return CodeBaseEntry3.builder()
        .methodSignature(MethodSignature3.fromFormat2(codeBaseEntry2.getMethodSignature()))
        .signature(codeBaseEntry2.getSignature())
        .visibility(codeBaseEntry2.getVisibility())
        .build();
  }

  public static CodeBaseEntry3 sampleCodeBaseEntry() {
    return builder()
        .methodSignature(MethodSignature3.createSampleMethodSignature())
        .signature("signature1()")
        .visibility("public")
        .build();
  }
}
