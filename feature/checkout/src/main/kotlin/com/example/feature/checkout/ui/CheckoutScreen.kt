package com.example.feature.checkout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.core.ui.UiText
import com.example.core.ui.asString
import com.example.feature.checkout.CheckoutViewModel
import com.example.feature.checkout.R
import com.example.feature.checkout.contract.AddressAction
import com.example.feature.checkout.contract.CartAction
import com.example.feature.checkout.contract.CheckoutEvent
import com.example.feature.checkout.contract.CheckoutIntent
import com.example.feature.checkout.contract.LifecycleAction
import com.example.feature.checkout.contract.OrderAction
import com.example.feature.checkout.contract.PaymentAction
import com.example.feature.checkout.contract.PromoAction
import com.example.feature.checkout.contract.ShippingAction
import com.example.feature.checkout.projector.toUiText
import com.example.feature.checkout.ui.model.AddressStatusUi
import com.example.feature.checkout.ui.model.CartLineUi
import com.example.feature.checkout.ui.model.CheckoutUiState
import com.example.feature.checkout.ui.model.SelectableRowUi
import com.example.feature.checkout.ui.model.ShippingSectionUi

/** Stateful entry point: wires the ViewModel. Everything below it is stateless and previewable. */
@Composable
fun CheckoutRoute(
    onOrderPlaced: (orderId: String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // One-off events, collected only while visible so navigation never fires in the background.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is CheckoutEvent.NavigateToConfirmation -> onOrderPlaced(event.orderId)
                    is CheckoutEvent.ShowTransientError -> snackbar.showSnackbar(event.error.toUiText().resolve(context))
                }
            }
        }
    }

    CheckoutScreen(state = state, snackbar = snackbar, onIntent = viewModel::onIntent)
}

@Composable
fun CheckoutScreen(
    state: CheckoutUiState,
    snackbar: SnackbarHostState,
    onIntent: (CheckoutIntent) -> Unit,
) {
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                CheckoutUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is CheckoutUiState.Error -> ErrorState(state, onRetry = { onIntent(LifecycleAction.RetryLoadClicked) })
                is CheckoutUiState.Content -> Content(state, onIntent)
            }
        }
    }
}

@Composable
private fun Content(state: CheckoutUiState.Content, onIntent: (CheckoutIntent) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.offlineBanner) item { Text(stringResource(R.string.checkout_offline), color = MaterialTheme.colorScheme.error) }

        item { Text(state.cart.title.asString(), style = MaterialTheme.typography.titleMedium) }
        items(state.cart.lines, key = { it.sku }) { line ->
            CartLineRow(line, onQuantity = { onIntent(CartAction.QuantityChanged(line.sku, it)) })
        }

        item { SectionTitle(R.string.checkout_section_address) }
        items(state.address.rows, key = { "address-${it.id}" }) { row ->
            SelectableRow(row) { onIntent(AddressAction.Selected(row.id)) }
        }
        item {
            when (val status = state.address.status) {
                AddressStatusUi.Checking -> Text(stringResource(R.string.checkout_address_checking))
                is AddressStatusUi.Warning -> Text(status.message.asString(), color = MaterialTheme.colorScheme.error)
                AddressStatusUi.None, AddressStatusUi.Deliverable -> Unit
            }
        }

        item { SectionTitle(R.string.checkout_section_shipping) }
        when (val shipping = state.shipping) {
            ShippingSectionUi.Hidden -> Unit
            ShippingSectionUi.Loading -> item { CircularProgressIndicator() }
            is ShippingSectionUi.Error -> item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(shipping.message.asString(), Modifier.weight(1f))
                    TextButton(onClick = { onIntent(ShippingAction.RetryClicked) }) { Text(stringResource(R.string.action_retry)) }
                }
            }
            is ShippingSectionUi.Options -> items(shipping.rows, key = { "ship-${it.id}" }) { row ->
                SelectableRow(row) { onIntent(ShippingAction.Selected(row.id)) }
            }
        }

        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.promo.input,
                        onValueChange = { onIntent(PromoAction.InputChanged(it)) },
                        enabled = state.promo.inputEnabled,
                        label = { Text(stringResource(R.string.checkout_promo_label)) },
                        modifier = Modifier.weight(1f),
                    )
                    if (state.promo.applied != null) {
                        TextButton(onClick = { onIntent(PromoAction.RemoveClicked) }) { Text(stringResource(R.string.action_remove)) }
                    } else {
                        TextButton(onClick = { onIntent(PromoAction.ApplyClicked) }, enabled = state.promo.applyEnabled) {
                            if (state.promo.isRedeeming) CircularProgressIndicator() else Text(stringResource(R.string.action_apply))
                        }
                    }
                }
                state.promo.applied?.let { Text(it.asString(), color = MaterialTheme.colorScheme.primary) }
                state.promo.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
            }
        }

        item { SectionTitle(R.string.checkout_section_payment) }
        items(state.payment.rows, key = { "pay-${it.id}" }) { row ->
            SelectableRow(row) { onIntent(PaymentAction.Selected(row.id)) }
        }

        item { HorizontalDivider() }
        items(state.summary.rows) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.label.asString()); Text(row.value.asString())
            }
        }

        item {
            Button(
                onClick = { onIntent(OrderAction.PlaceClicked) },
                enabled = state.placeOrder.enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.placeOrder.loading) CircularProgressIndicator() else Text(state.placeOrder.label.asString())
            }
        }
    }

    state.blockingError?.let { message ->
        AlertDialog(
            onDismissRequest = { onIntent(OrderAction.ErrorDismissed) },
            confirmButton = { TextButton(onClick = { onIntent(OrderAction.ErrorDismissed) }) { Text(stringResource(R.string.action_ok)) } },
            text = { Text(message.asString()) },
        )
    }
}

@Composable
private fun CartLineRow(line: CartLineUi, onQuantity: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(line.name, Modifier.weight(1f))
        TextButton(onClick = { onQuantity(line.quantity - 1) }, enabled = line.canDecrement) { Text("−") }
        Text("${line.quantity}")
        TextButton(onClick = { onQuantity(line.quantity + 1) }, enabled = line.canIncrement) { Text("+") }
        Text(line.lineTotal)
    }
}

@Composable
private fun SelectableRow(row: SelectableRowUi, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = row.selected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = row.selected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(row.title.asString())
            row.subtitle?.let { Text(it.asString(), style = MaterialTheme.typography.bodySmall) }
        }
        row.trailing?.let { Text(it.asString()) }
    }
}

@Composable
private fun SectionTitle(id: Int) = Text(stringResource(id), style = MaterialTheme.typography.titleSmall)

@Composable
private fun ErrorState(state: CheckoutUiState.Error, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(state.message.asString())
        if (state.canRetry) Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

/** Non-composable resolution, for snackbars launched from a coroutine. */
private fun UiText.resolve(context: android.content.Context): String = when (this) {
    is UiText.Raw -> value
    is UiText.Res -> context.getString(id, *args.toTypedArray())
    is UiText.Plural -> context.resources.getQuantityString(id, count, *args.toTypedArray())
}
