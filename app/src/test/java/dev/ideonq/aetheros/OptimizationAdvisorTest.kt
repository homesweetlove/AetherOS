package dev.ideonq.aetheros

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimizationAdvisorTest {
    @Test fun eachProfileHasSafeSuggestions() {
        ExperienceProfile.entries.forEach { profile ->
            val items = OptimizationAdvisor.suggestions(profile, false)
            assertEquals(5, items.size)
            assertTrue(items.any { it.title.contains("보호") })
            assertTrue(items.none { it.detail.contains("자동 비활성화") })
        }
    }

    @Test fun powerModeChangesGuidance() {
        val on = OptimizationAdvisor.suggestions(ExperienceProfile.BATTERY, true)
        val off = OptimizationAdvisor.suggestions(ExperienceProfile.BATTERY, false)
        assertTrue(on[1].title.contains("사용 중"))
        assertTrue(off[1].title.contains("꺼짐"))
    }
}
