package com.baek.diract.presentation.home.video

sealed class SectionChipItem {
    data object SetSection : SectionChipItem()

    data class SectionUi(
        val id: String,
        val name: String,
        val isDefault: Boolean = false,
        val isSelected: Boolean = false
    ) : SectionChipItem()
}
