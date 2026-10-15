package com.example.feature.checkout.presentation

import com.example.feature.checkout.presentation.reducer.AddressReducer
import com.example.feature.checkout.presentation.reducer.CartReducer
import com.example.feature.checkout.presentation.reducer.CheckoutReducer
import com.example.feature.checkout.presentation.reducer.ContextReducer
import com.example.feature.checkout.presentation.reducer.LifecycleReducer
import com.example.feature.checkout.presentation.reducer.OrderReducer
import com.example.feature.checkout.presentation.reducer.PaymentReducer
import com.example.feature.checkout.presentation.reducer.PromoReducer
import com.example.feature.checkout.presentation.reducer.ShippingReducer

internal fun checkoutReducer(): CheckoutReducer = CheckoutReducer(
    lifecycle = LifecycleReducer(),
    context = ContextReducer(),
    cart = CartReducer(ContextReducer()),
    address = AddressReducer(),
    shipping = ShippingReducer(),
    promo = PromoReducer(),
    payment = PaymentReducer(),
    order = OrderReducer(ContextReducer()),
)
