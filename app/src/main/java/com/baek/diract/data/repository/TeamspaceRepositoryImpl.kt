package com.baek.diract.data.repository

import com.baek.diract.data.datasource.remote.TeamspaceRemoteDataSource
import com.baek.diract.data.mapper.toDomain
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.TeamMemberSummary
import com.baek.diract.domain.model.TeamspaceSummary
import com.baek.diract.domain.repository.TeamspaceRepository
import javax.inject.Inject

class TeamspaceRepositoryImpl @Inject constructor(
    private val remoteDataSource: TeamspaceRemoteDataSource
) : TeamspaceRepository {

    override suspend fun getMyTeamspaces(): DataResult<List<TeamspaceSummary>> {
        return try {
            val teamspaces = remoteDataSource.getMyTeamspaces()
            DataResult.Success(teamspaces.map { it.toDomain() })
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun createTeamspace(name: String): DataResult<TeamspaceSummary> {
        return try {
            val created = remoteDataSource.createTeamspace(name)
            DataResult.Success(created.toDomain())
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun renameTeamspace(teamspaceId: String, newName: String): DataResult<Unit> {
        return try {
            remoteDataSource.renameTeamspace(teamspaceId, newName)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun deleteTeamspace(teamspaceId: String): DataResult<Unit> {
        return try {
            remoteDataSource.deleteTeamspace(teamspaceId)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun getMembers(teamspaceId: String): DataResult<List<TeamMemberSummary>> {
        return try {
            val members = remoteDataSource.getMembers(teamspaceId)
            DataResult.Success(members.map { it.toDomain() })
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun transferLeader(teamspaceId: String, newLeaderId: String): DataResult<Unit> {
        return try {
            remoteDataSource.transferLeader(teamspaceId, newLeaderId)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun leaveTeamspace(teamspaceId: String): DataResult<Unit> {
        return try {
            remoteDataSource.leaveTeamspace(teamspaceId)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun kickMembers(teamspaceId: String, memberIds: List<String>): DataResult<Unit> {
        return try {
            remoteDataSource.kickMembers(teamspaceId, memberIds)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}