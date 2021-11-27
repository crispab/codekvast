package io.codekvast.backoffice.rules.impl

import io.codekvast.backoffice.facts.CollectionStarted
import io.codekvast.backoffice.facts.PersistentFact
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import kotlin.test.assertEquals

/**
 * Make sure already persisted facts can be deserialized.
 *
 * The sample test cases come from the production database.
 *
 * @author olle.hallin@crisp.se
 */
class FactsDAOTest {

    data class TestCase(val dbType: String, val dbData: String, val expectedFact: PersistentFact)

    private val factDAO = FactDAO(mock(JdbcTemplate::class.java))

    @ParameterizedTest
    @MethodSource("testCases")
    fun persistentFactShouldBeDeserializable(testCase: TestCase) {
        assertEquals(
                actual = factDAO.parseJson(testCase.dbData, testCase.dbType),
                expected = testCase.expectedFact)
    }

    companion object {

        @Suppress("unused")
        @JvmStatic
        fun testCases() = listOf(
                TestCase(
                        dbType = "io.codekvast.backoffice.facts.CollectionStarted",
                        dbData = """{"collectionStartedAt":"2019-11-18T21:06:13.985353Z"}""",
                        expectedFact = CollectionStarted(
                                collectionStartedAt = Instant.parse("2019-11-18T21:06:13.985353Z"),
                                trialPeriodEndsAt = null)),

                TestCase(
                        dbType = "io.codekvast.backoffice.facts.CollectionStarted",
                        dbData = """{"collectionStartedAt":"2019-11-20T08:17:05.308390Z","trialPeriodEndsAt":"2020-01-19T08:17:05.308390Z","welcomeMailSentTo":"olle.hallin@hit.se","welcomeMailSentAt":"2019-11-20T08:17:06.352504Z"}""",
                        expectedFact = CollectionStarted(
                                collectionStartedAt = Instant.parse("2019-11-20T08:17:05.308390Z"),
                                trialPeriodEndsAt = Instant.parse("2020-01-19T08:17:05.308390Z"),
                                welcomeMailSentTo = "olle.hallin@hit.se",
                                welcomeMailSentAt = Instant.parse("2019-11-20T08:17:06.352504Z")
                        )
                ))

    }

}
