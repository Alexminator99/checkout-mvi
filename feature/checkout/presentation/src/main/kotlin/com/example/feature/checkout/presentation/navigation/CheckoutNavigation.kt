package com.example.feature.checkout.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.feature.checkout.presentation.ui.CheckoutRoute
import com.example.feature.checkout.presentation.ui.confirmation.OrderConfirmationScreen

fun NavGraphBuilder.checkoutScreen(onOrderPlaced: (orderId: String) -> Unit) {
    composable<CheckoutDestination> {
        CheckoutRoute(onOrderPlaced = onOrderPlaced)
    }
}

fun NavGraphBuilder.orderConfirmationScreen(onBackToCheckout: () -> Unit) {
    composable<OrderConfirmationDestination> { entry ->
        val destination = entry.toRoute<OrderConfirmationDestination>()
        OrderConfirmationScreen(orderId = destination.orderId, onBackToCheckout = onBackToCheckout)
    }
}
