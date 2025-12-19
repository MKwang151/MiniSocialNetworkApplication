package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.domain.model.Report
import com.example.minisocialnetworkapplication.core.domain.model.ReportStatus
import com.example.minisocialnetworkapplication.core.domain.repository.ReportRepository
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class ReportRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ReportRepository {

    companion object {
        private const val REPORTS_COLLECTION = "reports"
    }

    override suspend fun submitReport(report: Report): Result<String> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.Error(Exception("User not logged in"))
        }

        return try {
            val reportId = firestore.collection(REPORTS_COLLECTION).document().id
            
            // Fetch reporter name from Firestore users collection
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val reporterName = userDoc.getString("displayName") 
                ?: userDoc.getString("name")
                ?: currentUser.displayName 
                ?: "Unknown"
            
            val reportData = hashMapOf(
                "id" to reportId,
                "targetId" to report.targetId,
                "targetType" to report.targetType,
                "reporterId" to currentUser.uid,
                "reporterName" to reporterName,
                "authorId" to report.authorId,
                "groupId" to report.groupId,
                "reason" to report.reason,
                "description" to report.description,
                "status" to ReportStatus.PENDING.name,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection(REPORTS_COLLECTION)
                .document(reportId)
                .set(reportData)
                .await()
            
            Timber.d("Report submitted successfully: $reportId")
            Result.Success(reportId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to submit report")
            Result.Error(e)
        }
    }

    override suspend fun getReportsForPost(postId: String): Result<List<Report>> {
        return try {
            val snapshot = firestore.collection(REPORTS_COLLECTION)
                .whereEqualTo("targetId", postId)
                .get()
                .await()
            
            val reports = snapshot.documents.mapNotNull { doc ->
                try {
                    Report(
                        id = doc.getString("id") ?: "",
                        targetId = doc.getString("targetId") ?: doc.getString("postId") ?: "",
                        targetType = doc.getString("targetType") ?: "POST",
                        reporterId = doc.getString("reporterId") ?: "",
                        reporterName = doc.getString("reporterName") ?: "",
                        authorId = doc.getString("authorId") ?: "",
                        groupId = doc.getString("groupId"),
                        reason = doc.getString("reason") ?: "",
                        description = doc.getString("description") ?: "",
                        status = try {
                            ReportStatus.valueOf(doc.getString("status") ?: "PENDING")
                        } catch (e: Exception) {
                            ReportStatus.PENDING
                        },
                        createdAt = (doc.getTimestamp("createdAt") ?: com.google.firebase.Timestamp(java.util.Date(doc.getLong("createdAt") ?: 0L))) 
                            ?: com.google.firebase.Timestamp.now()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.Success(reports)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get reports for post")
            Result.Error(e)
        }
    }

    override suspend fun getReportsForGroup(groupId: String): Result<List<Report>> {
        return try {
            val snapshot = firestore.collection(REPORTS_COLLECTION)
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
            
            val reports = snapshot.documents.mapNotNull { doc ->
                try {
                    Report(
                        id = doc.getString("id") ?: "",
                        targetId = doc.getString("targetId") ?: doc.getString("postId") ?: "",
                        targetType = doc.getString("targetType") ?: "POST",
                        reporterId = doc.getString("reporterId") ?: "",
                        reporterName = doc.getString("reporterName") ?: "",
                        authorId = doc.getString("authorId") ?: "",
                        groupId = doc.getString("groupId"),
                        reason = doc.getString("reason") ?: "",
                        description = doc.getString("description") ?: "",
                        status = try {
                            ReportStatus.valueOf(doc.getString("status") ?: "PENDING")
                        } catch (e: Exception) {
                            ReportStatus.PENDING
                        },
                        createdAt = (doc.getTimestamp("createdAt") ?: com.google.firebase.Timestamp(java.util.Date(doc.getLong("createdAt") ?: 0L))) 
                            ?: com.google.firebase.Timestamp.now()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.Success(reports)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get reports for group")
            Result.Error(e)
        }
    }

    override suspend fun updateReportStatus(reportId: String, status: ReportStatus): Result<Unit> {
        return try {
            firestore.collection(REPORTS_COLLECTION)
                .document(reportId)
                .update("status", status.name)
                .await()
            
            Timber.d("Report $reportId status updated to $status")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update report status")
            Result.Error(e)
        }
    }

    override suspend fun dismissReport(reportId: String): Result<Unit> {
        return updateReportStatus(reportId, ReportStatus.DISMISSED)
    }
}
