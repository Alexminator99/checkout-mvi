package com.example.core.mvi

/**
 * Latest-wins concurrency, declared on the effect so the reducer stays in charge: starting an
 * effect whose [cancelKey] matches a running one cancels the running one.
 */
interface KeyedEffect {
    val cancelKey: Any
}
