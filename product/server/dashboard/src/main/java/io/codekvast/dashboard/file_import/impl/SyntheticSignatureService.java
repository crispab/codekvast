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
package io.codekvast.dashboard.file_import.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** @author olle.hallin@crisp.se */
@Component
@RequiredArgsConstructor
@Slf4j
public class SyntheticSignatureService {
  private static final Pattern FALLBACK_SYNTHETIC_SIGNATURE_PATTERN;

  static {
    // See io.codekvast.dashboard.dashboard.impl.DashboardServiceImplSyntheticSignatureTest
    FALLBACK_SYNTHETIC_SIGNATURE_PATTERN =
        Pattern.compile(
            ".*(\\$\\$.*|\\$\\w+\\$.*|\\.[A-Z0-9_]+\\(.*\\)$|\\$[a-z]+\\(\\)$|\\.\\.anonfun\\..*|\\.\\.(Enhancer|FastClass)"
                + "BySpringCGLIB\\.\\..*|\\.canEqual\\(java\\.lang\\.Object\\))");
  }

  private final SyntheticSignatureDAO syntheticSignatureDAO;

  private Pattern compiledPatterns;
  private int patternHash = 0;

  public boolean isSyntheticMethod(String signature) {
    return getCompiledPattern().matcher(signature).matches();
  }

  private Pattern getCompiledPattern() {
    List<SyntheticSignaturePattern> dbPatterns = syntheticSignatureDAO.getPatterns();
    if (dbPatterns.hashCode() != patternHash) {

      List<SyntheticSignaturePattern> validPatterns = validatePatterns(dbPatterns);

      if (validPatterns.isEmpty()) {
        logger.error("Found no valid synthetic signature patterns, using fallback");
        compiledPatterns = FALLBACK_SYNTHETIC_SIGNATURE_PATTERN;
      } else {
        logger.debug("Combining {} patterns retrieved from the database", validPatterns.size());
        String regexp =
            validPatterns
                .stream()
                .map(SyntheticSignaturePattern::getPattern)
                .collect(Collectors.joining("|", "(", ")"));
        compiledPatterns = Pattern.compile(regexp);
      }
      patternHash = dbPatterns.hashCode();
    }
    return compiledPatterns;
  }

  private List<SyntheticSignaturePattern> validatePatterns(
      List<SyntheticSignaturePattern> patterns) {
    List<SyntheticSignaturePattern> result = new ArrayList<>(patterns);
    for (var iterator = result.iterator(); iterator.hasNext(); ) {
      SyntheticSignaturePattern pattern = iterator.next();
      try {
        Pattern.compile(pattern.getPattern());
      } catch (PatternSyntaxException e) {
        logger.error("Invalid pattern '{}': {}", pattern, e.getMessage());
        syntheticSignatureDAO.rejectPattern(pattern, e.getMessage());
        iterator.remove();
      }
    }
    return result;
  }
}
