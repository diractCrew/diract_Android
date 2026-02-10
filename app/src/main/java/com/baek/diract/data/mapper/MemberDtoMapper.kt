package com.baek.diract.data.mapper

import com.baek.diract.data.remote.dto.MemberDto
import com.baek.diract.domain.model.TeamMemberSummary

fun MemberDto.toDomain(): TeamMemberSummary =
    TeamMemberSummary(id = user_id, name = user_name)
