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
package io.codekvast.agent.api.model.v1;

import io.codekvast.agent.api.util.PublishingUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.AssertTrue;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Output of the CodeBasePublisher implementations.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods", "OverlyComplexClass"})
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Slf4j
public class CodeBasePublication implements Serializable {
    private static final long serialVersionUID = 1L;

    @NonNull
    private CommonPublicationData commonData;

    @NonNull
    private Collection<CodeBaseEntry> entries;

    @NonNull
    private Map<String, String> overriddenSignatures;

    /**
     * "strange" signatures, i.e., signatures with unnatural names that indicate that they are synthesized at runtime by some
     * byte-code library.
     * key: strangeSignature
     * value: normalized strange signature
     */
    @NonNull
    private Map<String, String> strangeSignatures;

    /**
     * Trap for strange signatures. It checks that left and right parenthesis are either both present or missing and if present in correct
     * order.
     *
     * @return false if some bad signature is found in entries.
     */
    @SuppressWarnings("unused")
    @AssertTrue
    public boolean isValid() {
        boolean result = true;
        for (CodeBaseEntry entry : entries) {
            if (!PublishingUtils.isValid(entry.getSignature())) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("CodeBasePublication{commonData=%s, entries.size()=%d}", commonData, entries.size());
    }
}
