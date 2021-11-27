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
package io.codekvast.intake.file_import.impl

import io.codekvast.common.logging.LoggerDelegate
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.stereotype.Component
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import java.util.stream.Collectors

/** @author olle.hallin@crisp.se
 */
@Component
@RequiredArgsConstructor
@Slf4j
class SyntheticSignatureService(
        private val syntheticSignatureDAO: SyntheticSignatureDAO
) {

    private val fallbackSyntheticSignaturePattern = Pattern.compile(
            ".*(\\$\\$.*" +
                    "|\\$\\w+\\$.*" +
                    "|\\.[A-Z0-9_]+\\(.*\\)$" +
                    "|\\$[a-z]+\\(\\)$" +
                    "|\\.\\.anonfun\\..*" +
                    "|\\.\\.(Enhancer|FastClass)BySpringCGLIB\\.\\..*" +
                    "|\\.canEqual\\(java\\.lang\\.Object\\))"
    )

    val logger by LoggerDelegate()

    private var compiledPatterns: Pattern? = null
    private var patternHash = 0

    fun isSyntheticMethod(signature: String): Boolean {
        return compiledPattern.matcher(signature).matches()
    }

    private val compiledPattern: Pattern
        get() {
            val dbPatterns = syntheticSignatureDAO.syntheticPatterns()
            if (dbPatterns.hashCode() != patternHash) {
                val validPatterns = validatePatterns(dbPatterns)
                compiledPatterns = if (validPatterns.isEmpty()) {
                    logger.error("Found no valid synthetic signature patterns, using fallback")
                    fallbackSyntheticSignaturePattern
                } else {
                    logger.debug(
                            "Combining {} patterns retrieved from the database",
                            validPatterns.size
                    )
                    val regexp = validPatterns.stream()
                            .map(SyntheticSignaturePattern::pattern)
                            .collect(Collectors.joining("|", "(", ")"))
                    Pattern.compile(regexp)
                }
                patternHash = dbPatterns.hashCode()
            }
            return compiledPatterns!!
        }

    private fun validatePatterns(
            patterns: List<SyntheticSignaturePattern>
    ): List<SyntheticSignaturePattern> {
        val result: MutableList<SyntheticSignaturePattern> = ArrayList(patterns)
        val iterator = result.iterator()
        while (iterator.hasNext()) {
            val pattern = iterator.next()
            try {
                Pattern.compile(pattern.pattern)
            } catch (e: PatternSyntaxException) {
                logger.error(
                        "Invalid pattern '{}': {}",
                        pattern,
                        e.message
                )
                syntheticSignatureDAO.rejectPattern(pattern, e.message!!)
                iterator.remove()
            }
        }
        return result
    }
}