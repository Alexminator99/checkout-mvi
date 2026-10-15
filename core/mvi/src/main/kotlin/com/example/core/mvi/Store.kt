package com.example.core.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * The MVI loop of one screen.
 *
 * Every action goes through one channel and is reduced by one coroutine, so actions are reduced
 * strictly in order and the reducer never runs concurrently with itself. Effects run in [scope] and
 * report their results back as actions. Events are buffered without limit, so emitting one never
 * blocks the loop, and each event reaches one collector once.
 *
 * Keyed results carry a generation checked by the loop, so replacing an effect also invalidates
 * results that the previous generation already queued.
 *
 * Effect handlers own error translation: an exception escaping an effect is a bug and is not
 * caught here.
 */
class Store<State, Action, Effect, Event>(
    initialState: State,
    private val scope: CoroutineScope,
    private val reducer: Reducer<State, Action, Effect, Event>,
    private val effectHandler: EffectHandler<Effect, Action>,
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    private data class QueuedAction<A>(val action: A, val key: Any? = null, val generation: Long = 0)

    private val actions = Channel<QueuedAction<Action>>(Channel.UNLIMITED)
    private val generations = HashMap<Any, Long>()

    // Touched only by the loop coroutine, so it needs no locking.
    private val runningKeyedEffects = HashMap<Any, Job>()

    init {
        scope.launch {
            for (queued in actions) {
                if (queued.key == null || generations[queued.key] == queued.generation) reduce(queued.action)
            }
        }
    }

    fun dispatch(action: Action) {
        actions.trySend(QueuedAction(action))
    }

    private fun reduce(action: Action) {
        val transition = reducer.reduce(_state.value, action)
        _state.value = transition.state
        transition.events.forEach { _events.trySend(it) }
        transition.effects.forEach(::start)
    }

    private fun start(effect: Effect) {
        val key = (effect as? KeyedEffect)?.cancelKey
        key?.let { runningKeyedEffects.remove(it)?.cancel() }
        val generation = key?.let { (generations[it] ?: 0L) + 1L } ?: 0L
        key?.let { generations[it] = generation }
        val job = scope.launch {
            effectHandler.handle(effect).collect { result -> actions.trySend(QueuedAction(result, key, generation)) }
        }
        key?.let { runningKeyedEffects[it] = job }
    }
}
