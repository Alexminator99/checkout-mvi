package com.example.core.mvi

/**
 * The result of reducing one action.
 *
 * @param state the next state, committed before any effect starts
 * @param effects work to run after the state is committed (network, disk, ...)
 * @param events one-off signals for the UI (navigation, transient messages)
 */
data class Transition<out State, out Effect, out Event>(
    val state: State,
    val effects: List<Effect> = emptyList(),
    val events: List<Event> = emptyList(),
)

/** A new (or unchanged) state with no effects and no events. */
fun <State> State.only(): Transition<State, Nothing, Nothing> = Transition(this)

fun <State, Effect> State.withEffects(vararg effects: Effect): Transition<State, Effect, Nothing> =
    Transition(this, effects.toList())

fun <State, Event> State.withEvent(event: Event): Transition<State, Nothing, Event> =
    Transition(this, events = listOf(event))

/** Keeps the latter state and concatenates effects and events. */
operator fun <State, Effect, Event> Transition<State, Effect, Event>.plus(
    next: Transition<State, Effect, Event>,
): Transition<State, Effect, Event> = Transition(next.state, effects + next.effects, events + next.events)

/** Applies [rule] to the state produced so far, accumulating effects and events. */
inline fun <State, Effect, Event> Transition<State, Effect, Event>.then(
    rule: (State) -> Transition<State, Effect, Event>,
): Transition<State, Effect, Event> = this + rule(state)
