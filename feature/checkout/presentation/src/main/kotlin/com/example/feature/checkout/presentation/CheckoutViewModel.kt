package com.example.feature.checkout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.mvi.Store
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutIntent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.effect.CheckoutEffectHandler
import com.example.feature.checkout.presentation.projector.CheckoutProjector
import com.example.feature.checkout.presentation.reducer.CheckoutReducer
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject

/**
 * The checkout's lifecycle adapter: owns the store's scope, exposes projected UI state and one-off
 * events, and accepts intents. It holds no business rules, so it stays this size however much the
 * screen grows.
 */
@HiltViewModel
class CheckoutViewModel @Inject internal constructor(
    reducer: CheckoutReducer,
    effectHandler: CheckoutEffectHandler,
    private val projector: CheckoutProjector,
) : ViewModel() {

    private val store = Store(
        initialState = CheckoutState(checkoutId = UUID.randomUUID().toString()),
        scope = viewModelScope,
        reducer = reducer,
        effectHandler = effectHandler,
    )

    val uiState: StateFlow<CheckoutUiState> = store.state
        .map(projector::project)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), projector.project(store.state.value))

    val events: Flow<CheckoutEvent> = store.events

    init {
        store.dispatch(LifecycleAction.Started)
    }

    /** The UI can only send intents; effect results are not part of its vocabulary. */
    fun onIntent(intent: CheckoutIntent) = store.dispatch(intent)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
