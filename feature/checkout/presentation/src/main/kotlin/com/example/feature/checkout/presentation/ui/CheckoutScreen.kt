package com.example.feature.checkout.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.ObserveAsEvents
import com.example.core.ui.asString
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.CheckoutViewModel
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutIntent
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PaymentAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.ShippingAction
import com.example.feature.checkout.presentation.projector.toUiText
import com.example.feature.checkout.presentation.ui.component.AddressSection
import com.example.feature.checkout.presentation.ui.component.BlockingErrorDialog
import com.example.feature.checkout.presentation.ui.component.CartSection
import com.example.feature.checkout.presentation.ui.component.CheckoutPaymentBar
import com.example.feature.checkout.presentation.ui.component.ErrorState
import com.example.feature.checkout.presentation.ui.component.OfflineBanner
import com.example.feature.checkout.presentation.ui.component.PaymentSection
import com.example.feature.checkout.presentation.ui.component.PlaceOrderButton
import com.example.feature.checkout.presentation.ui.component.PromoSection
import com.example.feature.checkout.presentation.ui.component.ShippingSection
import com.example.feature.checkout.presentation.ui.component.SummarySection
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import com.example.feature.checkout.presentation.ui.preview.CheckoutPreviewData
import kotlinx.coroutines.launch

/** Stateful entry point: wires the ViewModel, its one-off events and the snackbar. Everything below is stateless. */
@Composable
fun CheckoutRoute(
    onOrderPlaced: (orderId: String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CheckoutEvent.NavigateToConfirmation -> onOrderPlaced(event.orderId)
            is CheckoutEvent.ShowError -> {
                val message = event.error.toUiText().asString(context)
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        }
    }

    CheckoutScreen(state = state, onIntent = viewModel::onIntent, snackbarHostState = snackbarHostState)
}

@Composable
fun CheckoutScreen(
    state: CheckoutUiState,
    onIntent: (CheckoutIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state is CheckoutUiState.Content) CheckoutPaymentBar(state) { onIntent(OrderAction.PlaceClicked) }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when (state) {
                CheckoutUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is CheckoutUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { onIntent(LifecycleAction.RetryLoadClicked) },
                )
                is CheckoutUiState.Content -> CheckoutContent(state = state, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun CheckoutContent(state: CheckoutUiState.Content, onIntent: (CheckoutIntent) -> Unit) {
    // Leaving now would abandon the payment in flight, so Back does nothing until the order is answered.
    BackHandler(enabled = state.placeOrder.loading) {}

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "title") {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.checkout_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                )
                TextButton(
                    onClick = { onIntent(LifecycleAction.ResetDemoClicked) },
                    enabled = state.demoResetEnabled,
                ) { Text(stringResource(R.string.checkout_reset_demo)) }
            }
        }
        if (state.isOffline) {
            item(key = "offline") { OfflineBanner() }
        }
        item(key = "cart") {
            CartSection(
                cart = state.cart,
                onQuantityChange = { sku, quantity -> onIntent(CartAction.QuantityChanged(sku, quantity)) },
            )
        }
        item(key = "address") {
            AddressSection(address = state.address, onSelect = { onIntent(AddressAction.Selected(it)) })
        }
        item(key = "shipping") {
            ShippingSection(
                shipping = state.shipping,
                onSelect = { onIntent(ShippingAction.Selected(it)) },
                onRetry = { onIntent(ShippingAction.RetryClicked) },
            )
        }
        item(key = "promo") {
            PromoSection(
                promo = state.promo,
                onInputChange = { onIntent(PromoAction.InputChanged(it)) },
                onApply = { onIntent(PromoAction.ApplyClicked) },
                onRemove = { onIntent(PromoAction.RemoveClicked) },
            )
        }
        item(key = "payment") {
            PaymentSection(payment = state.payment, onSelect = { onIntent(PaymentAction.Selected(it)) })
        }
        item(key = "summary") { SummarySection(summary = state.summary) }
    }

    state.blockingError?.let { message ->
        BlockingErrorDialog(
            message = message,
            onDismiss = { onIntent(OrderAction.ErrorDismissed) },
            dismissEnabled = state.errorDismissEnabled,
            retry = state.retryPriceRefresh,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckoutScreenPreview() {
    ShopTheme { CheckoutScreen(state = CheckoutPreviewData.content, onIntent = {}) }
}
