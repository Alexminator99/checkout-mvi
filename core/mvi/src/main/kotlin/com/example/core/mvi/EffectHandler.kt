package com.example.core.mvi

import kotlinx.coroutines.flow.Flow

/**
 * Runs an effect and reports its results back as actions.
 *
 * One-shot work emits one action, a subscription keeps emitting, fire-and-forget emits nothing.
 * Failures must be translated into actions: an exception escaping the flow is a bug.
 */
fun interface EffectHandler<Effect, Action> {
    fun handle(effect: Effect): Flow<Action>
}
