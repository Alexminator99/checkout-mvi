package com.example.core.mvi

/**
 * Pure mapping from domain-shaped screen state to render-ready UI state.
 *
 * A reducer answers "what happened?"; a projector answers "what does it look like?".
 * Projectors hold no memory and handle no actions. Same state in, same UI state out.
 */
fun interface Projector<S, U> {
    fun project(state: S): U
}
