package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

data class PromoSectionUi(
    val input: String,
    val inputEnabled: Boolean,
    val applyEnabled: Boolean,
    val removeEnabled: Boolean,
    val isRedeeming: Boolean,
    val applied: UiText?,
    val error: UiText?,
)
