package com.baek.diract.data.datasource.remote

import com.baek.diract.data.remote.dto.MemberDto
import com.baek.diract.data.remote.dto.TeamspaceDto

interface TeamspaceRemoteDataSource {
    suspend fun getMyTeamspaces(): List<TeamspaceDto>
    suspend fun createTeamspace(name: String): TeamspaceDto
    suspend fun renameTeamspace(teamspaceId: String, newName: String)
    suspend fun deleteTeamspace(teamspaceId: String)

    suspend fun getMembers(teamspaceId: String): List<MemberDto>

    suspend fun transferLeader(teamspaceId: String, newLeaderId: String)
    suspend fun leaveTeamspace(teamspaceId: String)
    suspend fun kickMembers(teamspaceId: String, memberIds: List<String>)
}
