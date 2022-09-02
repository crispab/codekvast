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
package io.codekvast.backoffice.rules.impl

import com.google.gson.GsonBuilder
import io.codekvast.backoffice.facts.PersistentFact
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLDataException
import java.sql.Statement.RETURN_GENERATED_KEYS
import java.time.Instant

/** @author olle.hallin@crisp.se
 */
@Repository
class FactDAO(private val jdbcTemplate: JdbcTemplate) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val gson = GsonBuilder().registerTypeAdapter(Instant::class.java, InstantTypeAdapter()).create()

    @Transactional(rollbackFor = [Exception::class], propagation = Propagation.MANDATORY)
    fun getFacts(customerId: Long): List<FactWrapper> {

        val rowMapper = RowMapper { rs, _ ->
            val id = rs.getLong("id")
            val type = rs.getString("type")
            val data = rs.getString("data")

            try {
                val fact = parseJson(data, type)
                FactWrapper(id, fact as PersistentFact)
            } catch (e: ClassCastException) {
                throw SQLDataException("Cannot load fact of type '$type'", e)
            } catch (e: ClassNotFoundException) {
                throw SQLDataException("Cannot load fact of type '$type'", e)
            }
        }

        val wrappers = jdbcTemplate.query(
                "SELECT id, type, data FROM facts WHERE customerId = ?",
                rowMapper, customerId)
        logger.trace("Retrieved the persistent facts {} for customer {}", wrappers, customerId)
        logger.debug("Retrieved {} persistent facts for customer {}", wrappers.size, customerId)
        return wrappers
    }

    @Transactional(rollbackFor = [Exception::class], propagation = Propagation.MANDATORY)
    fun addFact(customerId: Long, fact: PersistentFact): Long {
        val keyHolder: KeyHolder = GeneratedKeyHolder()
        val inserted = jdbcTemplate.update(
                AddFactStatementCreator(customerId, getType(fact), gson.toJson(fact)), keyHolder)
        val factId: Long = keyHolder.key?.toLong() ?: -1L
        if (factId < 0) {
            logger.error("Failed to get generated key")
        }
        if (inserted <= 0) {
            logger.error("Attempt to insert duplicate fact {}:{}", customerId, factId)
        }
        return factId
    }

    @Transactional(rollbackFor = [Exception::class], propagation = Propagation.MANDATORY)
    fun updateFact(customerId: Long, factId: Long, fact: PersistentFact) {
        val updated = jdbcTemplate.update(
                "UPDATE facts SET type = ?, data = ? WHERE id = ? AND customerId = ?",
                getType(fact), gson.toJson(fact), factId, customerId)
        if (updated <= 0) {
            logger.error("Failed to update fact {}:{}", customerId, factId)
        }
    }

    @Transactional(rollbackFor = [Exception::class], propagation = Propagation.MANDATORY)
    fun removeFact(customerId: Long, factId: Long) {
        val deleted = jdbcTemplate.update(
                "DELETE FROM facts WHERE id = ? AND customerId = ?", factId, customerId)
        if (deleted <= 0) {
            logger.error("Failed to delete fact {}:{}", customerId, factId)
        }
    }

    private fun getType(fact: PersistentFact) = fact.javaClass.name

    fun parseJson(data: String, type: String) = gson.fromJson(data, Class.forName(type))

    private class AddFactStatementCreator(private val customerId: Long,
                                          private val type: String,
                                          private val data: String) : PreparedStatementCreator {

        override fun createPreparedStatement(con: Connection): PreparedStatement {
            val ps = con.prepareStatement(
                    "INSERT IGNORE INTO facts(customerId, type, data) VALUES(?, ?, ?)",
                    RETURN_GENERATED_KEYS)
            ps.setLong(1, customerId)
            ps.setString(2, type)
            ps.setString(3, data)
            return ps
        }
    }
}
