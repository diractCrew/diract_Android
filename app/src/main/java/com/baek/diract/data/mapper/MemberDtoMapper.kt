package com.baek.diract.data.mapper

import com.baek.diract.data.remote.dto.TeamspaceMemberDto
import com.baek.diract.domain.model.TeamMemberSummary

fun TeamspaceMemberDto.toTeamMemberSummary(): TeamMemberSummary =
    TeamMemberSummary(id = userId, name = name /* + email/joinedAt 필요하면 추가 */)
