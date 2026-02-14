package com.baek.diract.data.datasource.remote

import com.baek.diract.data.remote.dto.ProjectDto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProjectRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProjectRemoteDataSource {

    override suspend fun getProjects(teamspaceId: String): List<ProjectDto> {
        val snapshot = firestore
            .collection("teamspace")
            .document(teamspaceId)
            .collection("project")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(ProjectDto::class.java)
        }
    }

    override suspend fun createProject(
        teamspaceId: String,
        creatorId: String,
        projectName: String
    ): ProjectDto {
        val projectRef = firestore
            .collection("teamspace")
            .document(teamspaceId)
            .collection("project")
            .document()

        val now = Timestamp.now()
        val dto = ProjectDto(
            project_id = projectRef.id,
            teamspace_id = teamspaceId,
            creator_id = creatorId,
            project_name = projectName,
            created_at = now,
            updated_at = now
        )

        projectRef.set(dto).await()
        return dto
    }

    override suspend fun editProjectName(
        teamspaceId: String,
        projectId: String,
        newName: String
    ): ProjectDto {
        val ref = firestore
            .collection("teamspace")
            .document(teamspaceId)
            .collection("project")
            .document(projectId)

        ref.update(
            mapOf(
                "project_name" to newName,
                "updated_at" to Timestamp.now()
            )
        ).await()

        val updated = ref.get().await()
        return updated.toObject(ProjectDto::class.java)
            ?: throw IllegalStateException("Project not found")
    }

    override suspend fun deleteProject(teamspaceId: String, projectId: String) {
        firestore
            .collection("teamspace")
            .document(teamspaceId)
            .collection("project")
            .document(projectId)
            .delete()
            .await()
    }
}
