package com.example.learningreminder.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LearningTaskTest {

    private val task = LearningTask(
        childId = 1,
        title = "朗读",
        reminderTime = "19:30"
    )

    @Test
    fun adjustedReminderTime_takesPrecedence() {
        assertEquals(
            "18:45",
            task.copy(adjustedReminderTime = "18:45").effectiveReminderTime()
        )
    }

    @Test
    fun blankAdjustedReminderTime_fallsBackToOriginalTime() {
        assertEquals(
            "19:30",
            task.copy(adjustedReminderTime = null).effectiveReminderTime()
        )
        assertEquals(
            "19:30",
            task.copy(adjustedReminderTime = "   ").effectiveReminderTime()
        )
    }
}
