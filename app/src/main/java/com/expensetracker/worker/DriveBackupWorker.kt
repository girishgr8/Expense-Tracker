package com.expensetracker.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.data.repository.UserPreferencesRepository
import com.expensetracker.util.CsvExporter
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@HiltWorker
class DriveBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val authManager: AuthManager,
    private val csvExporter: CsvExporter,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    private val client = OkHttpClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userId = authManager.userId
            if (userId.isEmpty()) {
                Log.w(TAG, "No user logged in — skipping backup")
                return@withContext Result.failure()
            }

            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (account == null) {
                Log.w(TAG, "No Google account found — skipping backup")
                return@withContext Result.failure()
            }

            // Check if the Drive scope has been granted
            if (!GoogleSignIn.hasPermissions(account, Scope(DRIVE_SCOPE))) {
                Log.w(
                    TAG, "Drive scope not granted — backup skipped. " +
                            "User must grant access from Settings."
                )
                // Return failure (not retry) — retrying won't help without user action
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to ERROR_SCOPE_NOT_GRANTED)
                )
            }

            // Get access token
            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext, listOf(DRIVE_SCOPE)
            ).also { it.selectedAccount = account.account }

            val token: String = try {
                credential.token   // blocking — runs on IO dispatcher
            } catch (e: UserRecoverableAuthIOException) {
                Log.e(TAG, "Drive consent needed: ${e.message}")
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to ERROR_SCOPE_NOT_GRANTED)
                )
            } catch (e: UserRecoverableAuthException) {
                Log.e(TAG, "Drive consent needed: ${e.message}")
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to ERROR_SCOPE_NOT_GRANTED)
                )
            } catch (e: GoogleAuthException) {
                Log.e(TAG, "Auth error getting token: ${e.message}")
                return@withContext Result.retry()
            }

            if (token.isBlank()) {
                Log.e(TAG, "Received blank token")
                return@withContext Result.retry()
            }

            // Export CSV
            val transactions = transactionRepository.getAllTransactionsOneShot(userId)
            if (transactions.isEmpty()) {
                Log.i(TAG, "No transactions to backup")
                userPreferencesRepository.setLastBackupTimestamp(System.currentTimeMillis())
                return@withContext Result.success()
            }

            val timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
            val csvFile = csvExporter.exportToCsv(transactions, "backup_$timestamp.csv")

            // Upload to Drive
            val folderId = getOrCreateFolder(token)
            if (folderId == null) {
                Log.e(TAG, "Failed to get/create Drive folder")
                return@withContext Result.retry()
            }

            uploadFile(token, csvFile.absolutePath, csvFile.name, folderId)

            userPreferencesRepository.setLastBackupTimestamp(System.currentTimeMillis())
            Log.i(TAG, "Backup successful: ${csvFile.name}")
            Result.success()

        } catch (e: IOException) {
            Log.e(TAG, "Network error during backup: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed: ${e.javaClass.simpleName} — ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun getOrCreateFolder(token: String): String? {
        return try {
            val q = URLEncoder.encode(
                "mimeType='application/vnd.google-apps.folder' and name='$FOLDER_NAME' and trashed=false",
                "UTF-8"
            )
            val listReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?q=$q&fields=files(id)&spaces=drive")
                .header("Authorization", "Bearer $token")
                .get().build()

            val listResp = client.newCall(listReq).execute()
            Log.d(TAG, "Folder list response: ${listResp.code}")
            if (!listResp.isSuccessful) {
                Log.e(TAG, "Folder list failed: ${listResp.code} ${listResp.body?.string()}")
                return null
            }

            val files = JSONObject(listResp.body!!.string()).getJSONArray("files")
            if (files.length() > 0) {
                return files.getJSONObject(0).getString("id").also {
                    Log.d(TAG, "Found existing folder: $it")
                }
            }

            // Create folder
            val meta = JSONObject()
                .put("name", FOLDER_NAME)
                .put("mimeType", "application/vnd.google-apps.folder")
                .toString()

            val createReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?fields=id")
                .header("Authorization", "Bearer $token")
                .post(meta.toRequestBody("application/json".toMediaType()))
                .build()

            val createResp = client.newCall(createReq).execute()
            Log.d(TAG, "Folder create response: ${createResp.code}")
            if (!createResp.isSuccessful) {
                Log.e(TAG, "Folder create failed: ${createResp.code} ${createResp.body?.string()}")
                return null
            }

            JSONObject(createResp.body!!.string()).getString("id").also {
                Log.d(TAG, "Created folder: $it")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getOrCreateFolder error: ${e.message}")
            null
        }
    }

    private fun uploadFile(token: String, filePath: String, fileName: String, folderId: String) {
        val file = java.io.File(filePath)

        val meta = JSONObject()
            .put("name", fileName)
            .put("parents", JSONArray().put(folderId))
            .toString()

        // ✅ Correct way to build multipart parts in OkHttp —
        // Content-Type is set via the RequestBody's media type, NOT via Headers.headersOf()
        val metaPart = MultipartBody.Part.create(
            meta.toRequestBody("application/json; charset=UTF-8".toMediaType())
        )

        val filePart = MultipartBody.Part.create(
            file.asRequestBody("text/csv".toMediaType())
        )

        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metaPart)
            .addPart(filePart)
            .build()

        val req = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()

        val resp = client.newCall(req).execute()
        Log.d(TAG, "Upload response: ${resp.code}")
        if (!resp.isSuccessful) {
            throw IOException("Upload failed: ${resp.code} — ${resp.body?.string()}")
        }
        Log.i(TAG, "File uploaded: $fileName")
    }

    companion object {
        const val WORK_NAME = "monthly_drive_backup"
        const val KEY_ERROR = "backup_error"
        const val ERROR_SCOPE_NOT_GRANTED = "drive_scope_not_granted"
        private const val FOLDER_NAME = "ExpenseTracker Backups"
        private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        private const val TAG = "DriveBackupWorker"
    }
}