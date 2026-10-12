package com.example.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.feature.checkout.presentation.navigation.CheckoutDestination
import com.example.feature.checkout.presentation.navigation.OrderConfirmationDestination
import com.example.feature.checkout.presentation.navigation.checkoutScreen
import com.example.feature.checkout.presentation.navigation.orderConfirmationScreen

@Composable
fun ShopNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = CheckoutDestination, modifier = modifier) {
        checkoutScreen(
            onOrderPlaced = { orderId ->
                // A paid checkout is never shown again: confirmation replaces it.
                navController.navigate(OrderConfirmationDestination(orderId)) {
                    popUpTo<CheckoutDestination> { inclusive = true }
                }
            },
        )
        orderConfirmationScreen(
            onBackToCheckout = {
                // A fresh checkout gets a new checkout id, so the next order is a new order.
                navController.navigate(CheckoutDestination) {
                    popUpTo<OrderConfirmationDestination> { inclusive = true }
                }
            },
        )
    }
}
