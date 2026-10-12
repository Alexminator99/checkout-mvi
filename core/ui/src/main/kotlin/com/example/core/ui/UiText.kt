package com.example.core.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Text resolved at render time, so the code that produces it stays free of Context and testable on
 * the JVM. Equality is structural.
 */
sealed interface UiText {
    data class Raw(val value: String) : UiText
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText

    companion object {
        fun res(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())

        fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): UiText = Plural(id, count, args.toList())
    }
}
