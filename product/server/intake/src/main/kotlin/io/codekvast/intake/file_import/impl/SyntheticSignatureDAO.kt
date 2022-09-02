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
package io.codekvast.intake.file_import.impl

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/** @author olle.hallin@crisp.se
 */
@Repository
class SyntheticSignatureDAO(private val jdbcTemplate: JdbcTemplate) {

    @Transactional(readOnly = true)
    @Cacheable(SYNTHETIC_SIGNATURE_PATTERNS_CACHE)
    fun syntheticPatterns(): List<SyntheticSignaturePattern> =
            jdbcTemplate.query(
                    "SELECT id, pattern FROM synthetic_signature_patterns WHERE errorMessage IS NULL"
            ) { rs, index ->
                SyntheticSignaturePattern(
                        id = rs.getLong("id"),
                        pattern = rs.getString("pattern")
                )
            }

    @Transactional(rollbackFor = [Exception::class])
    @CacheEvict(SYNTHETIC_SIGNATURE_PATTERNS_CACHE)
    fun rejectPattern(pattern: SyntheticSignaturePattern, errorMessage: String) {
        jdbcTemplate.update(
                "UPDATE synthetic_signature_patterns SET errorMessage = ? WHERE id = ? ",
                errorMessage,
                pattern.id
        )
    }

    companion object {
        const val SYNTHETIC_SIGNATURE_PATTERNS_CACHE = "synthetic_signature_patterns"
    }
}