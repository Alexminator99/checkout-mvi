package com.example.feature.checkout.di

import com.example.core.mvi.EffectHandler
import com.example.core.mvi.Projector
import com.example.core.mvi.Reducer
import com.example.feature.checkout.contract.AddressAction
import com.example.feature.checkout.contract.CartAction
import com.example.feature.checkout.contract.CheckoutAction
import com.example.feature.checkout.contract.CheckoutEffect
import com.example.feature.checkout.contract.CheckoutEvent
import com.example.feature.checkout.contract.CheckoutState
import com.example.feature.checkout.contract.ContextAction
import com.example.feature.checkout.contract.LifecycleAction
import com.example.feature.checkout.contract.OrderAction
import com.example.feature.checkout.contract.PaymentAction
import com.example.feature.checkout.contract.PromoAction
import com.example.feature.checkout.contract.ShippingAction
import com.example.feature.checkout.effect.CheckoutEffectHandler
import com.example.feature.checkout.projector.AddressSectionProjector
import com.example.feature.checkout.projector.CartSectionProjector
import com.example.feature.checkout.projector.CheckoutProjector
import com.example.feature.checkout.projector.PaymentSectionProjector
import com.example.feature.checkout.projector.PromoSectionProjector
import com.example.feature.checkout.projector.SectionProjector
import com.example.feature.checkout.projector.ShippingSectionProjector
import com.example.feature.checkout.projector.SummaryProjector
import com.example.feature.checkout.reducer.AddressReducer
import com.example.feature.checkout.reducer.CartReducer
import com.example.feature.checkout.reducer.CheckoutReducer
import com.example.feature.checkout.reducer.CheckoutSliceReducer
import com.example.feature.checkout.reducer.ContextReducer
import com.example.feature.checkout.reducer.LifecycleReducer
import com.example.feature.checkout.reducer.OrderReducer
import com.example.feature.checkout.reducer.PaymentReducer
import com.example.feature.checkout.reducer.PromoReducer
import com.example.feature.checkout.reducer.ShippingReducer
import com.example.feature.checkout.ui.model.AddressSectionUi
import com.example.feature.checkout.ui.model.CartSectionUi
import com.example.feature.checkout.ui.model.CheckoutUiState
import com.example.feature.checkout.ui.model.PaymentSectionUi
import com.example.feature.checkout.ui.model.PromoSectionUi
import com.example.feature.checkout.ui.model.ShippingSectionUi
import com.example.feature.checkout.ui.model.SummaryUi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Scoped to ViewModelComponent: these pieces live and die with the screen.
 * They are all stateless, so no scope annotation is needed.
 */
@Module
@InstallIn(ViewModelComponent::class)
internal abstract class CheckoutModule {

    // --- Store collaborators the ViewModel sees ---
    @Binds abstract fun reducer(impl: CheckoutReducer): Reducer<CheckoutState, CheckoutAction, CheckoutEffect, CheckoutEvent>
    @Binds abstract fun effectHandler(impl: CheckoutEffectHandler): EffectHandler<CheckoutEffect, CheckoutAction>
    @Binds abstract fun projector(impl: CheckoutProjector): Projector<CheckoutState, CheckoutUiState>

    // --- Slice reducers the root reducer routes to ---
    @Binds abstract fun lifecycle(impl: LifecycleReducer): CheckoutSliceReducer<LifecycleAction>
    @Binds abstract fun context(impl: ContextReducer): CheckoutSliceReducer<ContextAction>
    @Binds abstract fun cart(impl: CartReducer): CheckoutSliceReducer<CartAction>
    @Binds abstract fun address(impl: AddressReducer): CheckoutSliceReducer<AddressAction>
    @Binds abstract fun shipping(impl: ShippingReducer): CheckoutSliceReducer<ShippingAction>
    @Binds abstract fun promo(impl: PromoReducer): CheckoutSliceReducer<PromoAction>
    @Binds abstract fun payment(impl: PaymentReducer): CheckoutSliceReducer<PaymentAction>
    @Binds abstract fun order(impl: OrderReducer): CheckoutSliceReducer<OrderAction>

    // --- Section projectors the screen projector composes ---
    @Binds abstract fun cartSection(impl: CartSectionProjector): SectionProjector<CartSectionUi>
    @Binds abstract fun addressSection(impl: AddressSectionProjector): SectionProjector<AddressSectionUi>
    @Binds abstract fun shippingSection(impl: ShippingSectionProjector): SectionProjector<ShippingSectionUi>
    @Binds abstract fun promoSection(impl: PromoSectionProjector): SectionProjector<PromoSectionUi>
    @Binds abstract fun paymentSection(impl: PaymentSectionProjector): SectionProjector<PaymentSectionUi>
    @Binds abstract fun summarySection(impl: SummaryProjector): SectionProjector<SummaryUi>
}
