package com.example.core.mvi

/**
 * Result of reducing one action.
 *
 * @param state   the next state (always present; may be the same instance)
 * @param effects work to run *after* the state is committed (network, disk, ...)
 * @param events  one-off signals for the UI (navigation, transient messages)
 */
data class Transition<out S, out E, out V>(
    val state: S,
    val effects: List<E> = emptyList(),
    val events: List<V> = emptyList(),
)

// ---- Small DSL so reducers read like rules, not like constructor calls ----

/** No effects, no events: just a new (or the same) state. */
fun <S> S.only(): Transition<S, Nothing, Nothing> = Transition(this)

fun <S, E> S.withEffects(vararg effects: E): Transition<S, E, Nothing> =
    Transition(this, effects.toList())

fun <S, V> S.withEvent(event: V): Transition<S, Nothing, V> =
    Transition(this, events = listOf(event))

/** Merge two transitions: keep the latter state, concatenate effects and events. */
operator fun <S, E, V> Transition<S, E, V>.plus(next: Transition<S, E, V>): Transition<S, E, V> =
    Transition(next.state, effects + next.effects, events + next.events)

/** Chain a rule onto the state produced by this transition, accumulating effects/events. */
inline fun <S, E, V> Transition<S, E, V>.then(rule: (S) -> Transition<S, E, V>): Transition<S, E, V> =
    this + rule(state)
