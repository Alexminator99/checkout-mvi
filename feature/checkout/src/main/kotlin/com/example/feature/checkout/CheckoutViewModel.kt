package com.example.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.mvi.EffectHandler
import com.example.core.mvi.Projector
import com.example.core.mvi.Reducer
import com.example.core.mvi.StoreFactory
import com.example.feature.checkout.contract.CheckoutAction
import com.example.feature.checkout.contract.CheckoutEffect
import com.example.feature.checkout.contract.CheckoutEvent
import com.example.feature.checkout.contract.CheckoutIntent
import com.example.feature.checkout.contract.CheckoutState
import com.example.feature.checkout.contract.LifecycleAction
import com.example.feature.checkout.ui.model.CheckoutUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Lifecycle adapter, nothing more. It knows no repositories, no use cases and no
 * business rules: it owns the store's scope, exposes projected UI state, and
 * accepts intents. Every collaborator is an abstraction (Dependency Inversion).
 *
 * This ViewModel is ~40 lines and will stay that size however much the screen grows.
 */
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    storeFactory: StoreFactory,
    reducer: Reducer<CheckoutState, CheckoutAction, CheckoutEffect, CheckoutEvent>,
    effectHandler: EffectHandler<CheckoutEffect, CheckoutAction>,
    private val projector: Projector<CheckoutState, CheckoutUiState>,
) : ViewModel() {

    private val store = storeFactory.create(
        initialState = CheckoutState(),
        scope = viewModelScope,
        reducer = reducer,
        effectHandler = effectHandler,
    )

    val uiState: StateFlow<CheckoutUiState> = store.state
        .map(projector::project)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), projector.project(store.state.value))

    val events: Flow<CheckoutEvent> = store.events

    init {
        store.dispatch(LifecycleAction.Started)
    }

    /** The UI can only send intents; effect results are not part of its vocabulary. */
    fun onIntent(intent: CheckoutIntent) = store.dispatch(intent)
}
