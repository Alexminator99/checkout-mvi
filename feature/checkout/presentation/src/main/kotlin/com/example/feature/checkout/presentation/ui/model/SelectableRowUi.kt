package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

data class SelectableRowUi(
    val id: String,
    val title: UiText,
    val subtitle: UiText?,
    val trailing: UiText?,
    val selected: Boolean,
)
