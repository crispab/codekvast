/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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
package io.codekvast.javaagent.model.v2;

import io.codekvast.javaagent.model.v1.CodeBaseEntry1;
import io.codekvast.javaagent.model.v1.CodeBasePublication1;
import io.codekvast.javaagent.model.v1.CommonPublicationData1;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Output of the CodeBasePublisher implementations.
 *
 * @author olle.hallin@crisp.se
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class CodeBasePublication2 implements Serializable {
    private static final long serialVersionUID = 1L;

    @NonNull
    private CommonPublicationData1 commonData;

    @NonNull
    private Collection<CodeBaseEntry2> entries;

    @Override
    public String toString() {
        return String.format("%s(commonData=%s, entries.size()=%d)",
                             this.getClass().getSimpleName(), commonData, entries.size());
    }

    public static CodeBasePublication2 fromV1Format(CodeBasePublication1 publication1) {

        List<CodeBaseEntry2> entries2 = new ArrayList<>();
        for (CodeBaseEntry1 entry1 : publication1.getEntries()) {
            entries2.add(CodeBaseEntry2.fromV1Format(entry1));
        }

        return CodeBasePublication2.builder()
                                   .commonData(publication1.getCommonData())
                                   .entries(entries2)
                                   .build();
    }
}
