package app.orchard.tv.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChallengesTest {
    private val languages = listOf("en", "zh", "ru", "uk", "kk")

    private fun check(question: Challenge) {
        assertEquals("four choices", 4, question.options.size)
        assertEquals("no repeats", 4, question.options.distinct().size)
        assertTrue("an answer among the choices", question.answer in question.options.indices)
        assertTrue("the prompt is not empty", question.prompt.isNotBlank())
        assertTrue(question.isRight(question.answer))
        assertTrue(!question.isRight((question.answer + 1) % 4))
    }

    @Test
    fun defaultsToPin() {
        assertEquals(KidsGate.Pin, KidsGate.of(null))
        assertEquals(KidsGate.Pin, KidsGate.of("nonsense"))
        assertEquals(KidsGate.Math, KidsGate.of("math"))
        assertTrue(KidsGate.Pin.asks.not())
        assertTrue(KidsGate.Math.asks)
    }

    @Test
    fun pinyinIsChineseOnly() {
        assertTrue(KidsGate.Pinyin in KidsGate.available("zh"))
        assertTrue(KidsGate.Pinyin !in KidsGate.available("ru"))
        assertNotNull(Challenges.next(KidsGate.Pinyin, "zh", Random(1)))
        assertNull(Challenges.next(KidsGate.Pinyin, "ru", Random(1)))
    }

    @Test
    fun mathAlwaysFitsThePanel() {
        val random = Random(2)
        repeat(500) { check(Challenges.next(KidsGate.Math, "en", random)!!) }
    }

    @Test
    fun everyBankFitsThePanel() {
        for (language in languages) {
            val random = Random(language.hashCode())
            for (gate in KidsGate.entries.filter { it != KidsGate.Pin }) {
                repeat(100) {
                    val question = Challenges.next(gate, language, random)
                    if (gate == KidsGate.Pinyin && !language.startsWith("zh")) assertNull(question)
                    else check(question!!)
                }
            }
        }
    }

    @Test
    fun aRetryAsksSomethingElse() {
        val random = Random(3)
        for (gate in KidsGate.entries.filter { it != KidsGate.Pin }) {
            var previous = Challenges.next(gate, "zh", random)!!.prompt
            repeat(30) {
                val next = Challenges.next(gate, "zh", random, avoid = previous)!!
                assertTrue("asked the same question again", next.prompt != previous)
                previous = next.prompt
            }
        }
    }

    @Test
    fun pinAsksNoQuestion() {
        assertNull(Challenges.next(KidsGate.Pin, "en", Random(4)))
    }
}
