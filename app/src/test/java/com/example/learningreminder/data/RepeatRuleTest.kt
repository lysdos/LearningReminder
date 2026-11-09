package com.example.learningreminder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RepeatRuleTest {

    private val monday = LocalDate.of(2026, 10, 19) // Monday
    private val saturday = LocalDate.of(2026, 10, 24) // Saturday
    private val sunday = LocalDate.of(2026, 10, 25) // Sunday

    @Test
    fun daily_matchesEveryDay() {
        assertTrue(RepeatRule.isScheduled("DAILY", monday))
        assertTrue(RepeatRule.isScheduled("DAILY", saturday))
        assertTrue(RepeatRule.isScheduled("DAILY", sunday))
    }

    @Test
    fun weekday_excludesWeekend() {
        assertTrue(RepeatRule.isScheduled("WEEKDAY", monday))
        assertFalse(RepeatRule.isScheduled("WEEKDAY", saturday))
        assertFalse(RepeatRule.isScheduled("WEEKDAY", sunday))
    }

    @Test
    fun explicitDays_matchOnlyListedDays() {
        val rule = "MON,WED"
        assertTrue(RepeatRule.isScheduled(rule, monday))
        assertTrue(RepeatRule.isScheduled(rule, LocalDate.of(2026, 10, 21)))
        assertFalse(RepeatRule.isScheduled(rule, saturday))
        assertFalse(RepeatRule.isScheduled(rule, LocalDate.of(2026, 10, 20)))
    }

    @Test
    fun caseInsensitiveAndWhitespaceTolerant() {
        assertTrue(RepeatRule.isScheduled(" mon , fri ", monday))
        assertTrue(RepeatRule.isScheduled(" mon , fri ", LocalDate.of(2026, 10, 23)))
    }

    @Test
    fun unknownRule_fallsBackToDaily() {
        assertTrue(RepeatRule.isScheduled("WHATEVER", monday))
    }
}
