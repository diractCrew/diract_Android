package com.baek.diract.domain.model

import java.time.LocalDate

data class TeamspaceSummary(
    val id: String,
    val name: String,
    val ownerId: String,
    val createdAt: LocalDate
)