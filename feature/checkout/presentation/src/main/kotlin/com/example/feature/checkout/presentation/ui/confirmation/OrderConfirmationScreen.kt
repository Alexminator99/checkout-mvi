package com.example.feature.checkout.presentation.ui.confirmation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.ui.theme.ShopTheme
import com.example.feature.checkout.presentation.R

@Composable
fun OrderConfirmationScreen(
    orderId: String,
    onBackToCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = stringResource(R.string.confirmation_title), style = MaterialTheme.typography.headlineMedium)
            Text(text = stringResource(R.string.confirmation_order_number, orderId), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onBackToCheckout) { Text(text = stringResource(R.string.confirmation_back_to_checkout)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderConfirmationScreenPreview() {
    ShopTheme { OrderConfirmationScreen(orderId = "ORD-1001", onBackToCheckout = {}) }
}
