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
package io.codekvast.agent.model.v1;

import io.codekvast.agent.model.PublishingUtils;
import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representation of a code base entry.
 *
 * @author olle.hallin@crisp.se
 */
@Value
public class CodeBaseEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Pattern VISIBILITY_PATTERN = Pattern.compile("(.*)?(public|protected|private) .*");

    /**
     * The normalized signature in String form.
     */
    @NonNull
    private final String normalizedSignature; // TODO Remove when daemon is dead.

    /**
     * The low-level description of the signature.
     */
    private final MethodSignature methodSignature;

    /**
     * The status of the signature. How it was found, if it has been excluded and so on.
     */
    @NonNull
    private final SignatureStatus signatureStatus;

    /**
     * The visibility of the signature. Package private is coded as 'package-private'.
     */
    @NonNull
    private final String visibility;

    /**
     * The signature.
     */
    @NonNull
    private final String signature;

    public CodeBaseEntry(String normalizedSignature, MethodSignature methodSignature, SignatureStatus signatureStatus) {
        this.normalizedSignature = normalizedSignature;
        this.methodSignature = methodSignature;
        this.signatureStatus = signatureStatus;
        this.signature = PublishingUtils.stripModifiers(normalizedSignature);

        Matcher matcher = VISIBILITY_PATTERN.matcher(normalizedSignature);
        this.visibility = matcher.matches() ? matcher.group(2) : "package-private";

    }
}
