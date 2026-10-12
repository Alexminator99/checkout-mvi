package com.example.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UiTextTest {

    @Test
    fun `factory helpers build values that compare structurally`() {
        assertEquals(UiText.Res(1, listOf("a", 2)), UiText.res(1, "a", 2))
        assertEquals(UiText.Plural(3, 4, listOf(4)), UiText.plural(3, 4, 4))
    }
}
