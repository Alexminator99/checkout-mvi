package com.example.core.ui

/**
 * Text that is resolved at render time, so projectors stay pure and Context-free.
 * Equality is structural, which makes projector tests one-line assertEquals.
 *
 * (In the real module, annotate ids with androidx.annotation.StringRes / PluralsRes.)
 */
sealed interface UiText {
    data class Raw(val value: String) : UiText
    data class Res(val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText

    companion object {
        fun res(id: Int, vararg args: Any): UiText = Res(id, args.toList())
        fun plural(id: Int, count: Int, vararg args: Any): UiText = Plural(id, count, args.toList())
    }
}
