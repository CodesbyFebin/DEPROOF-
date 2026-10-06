package com.deproof.data.security

import android.content.Context
import androidx.annotation.NonNull
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.deproof.domain.exception.DomainException
import com.google.gson.Gson
import timber.log.Timber
import java.io.File
import java.io.IOException

private const val TAG = "EncryptedBackupManager"

class EncryptedBackupManager(
    @NonNull private val context: Context,
    @NonNull private val gson: Gson = Gson()
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    suspend fun createBackup(
        @NonNull data: BackupData,
        @NonNull backupFilePath: String
    ): Result<String> = try {
        val backupFile = File(backupFilePath)
        backupFile.parentFile?.mkdirs()

        val encryptedData = encryptedPrefs.getString(BACKUP_DATA_KEY, null)
        if (encryptedData == null) {
            storeBackupData(data)
        }

        val metadata = BackupMetadata(
            timestamp = System.currentTimeMillis(),
            version = BACKUP_VERSION,
            deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
        )

        val jsonContent = BackupContent(
            metadata = metadata,
            data = data
        )

        backupFile.writeText(gson.toJson(jsonContent))
        Timber.d("Backup created: $backupFilePath")
        Result.Success(backupFilePath)
    } catch (e: IOException) {
        Timber.e(e, "Failed to create backup")
        Result.Error(DomainException.StorageError("Failed to create backup file", e))
    } catch (e: Exception) {
        Timber.e(e, "Backup creation error")
        Result.Error(DomainException.StorageError("Backup operation failed", e))
    }

    suspend fun restoreBackup(
        @NonNull backupFilePath: String
    ): Result<BackupData> = try {
        val backupFile = File(backupFilePath)
        if (!backupFile.exists()) {
            Timber.w("Backup file not found: $backupFilePath")
            return Result.Error(
                DomainException.NotFoundError("Backup file not found")
            )
        }

        val jsonContent = backupFile.readText()
        val backup = gson.fromJson(jsonContent, BackupContent::class.java)

        if (backup == null) {
            Timber.e("Failed to parse backup")
            return Result.Error(
                DomainException.ParseError("Invalid backup format")
            )
        }

        validateBackup(backup)

        storeBackupData(backup.data)
        Timber.d("Backup restored from: $backupFilePath")
        Result.Success(backup.data)
    } catch (e: IOException) {
        Timber.e(e, "Failed to read backup file")
        Result.Error(DomainException.StorageError("Failed to read backup", e))
    } catch (e: Exception) {
        Timber.e(e, "Backup restoration error")
        Result.Error(DomainException.StorageError("Backup restoration failed", e))
    }

    suspend fun deleteBackup(@NonNull backupFilePath: String): Result<Unit> = try {
        val backupFile = File(backupFilePath)
        if (backupFile.exists()) {
            backupFile.delete()
            Timber.d("Backup deleted: $backupFilePath")
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to delete backup")
        Result.Error(DomainException.StorageError("Failed to delete backup", e))
    }

    suspend fun getBackupMetadata(@NonNull backupFilePath: String): Result<BackupMetadata> = try {
        val backupFile = File(backupFilePath)
        if (!backupFile.exists()) {
            return Result.Error(DomainException.NotFoundError("Backup file not found"))
        }

        val jsonContent = backupFile.readText()
        val backup = gson.fromJson(jsonContent, BackupContent::class.java)
            ?: return Result.Error(DomainException.ParseError("Invalid backup format"))

        Result.Success(backup.metadata)
    } catch (e: Exception) {
        Timber.e(e, "Failed to read backup metadata")
        Result.Error(DomainException.StorageError("Failed to read backup metadata", e))
    }

    suspend fun listBackups(@NonNull backupDir: String): Result<List<BackupInfo>> = try {
        val backupDirectory = File(backupDir)
        if (!backupDirectory.exists()) {
            return Result.Success(emptyList())
        }

        val backupFiles = backupDirectory.listFiles { file ->
            file.isFile && file.name.endsWith(BACKUP_FILE_EXTENSION)
        } ?: emptyArray()

        val backupInfos = backupFiles.mapNotNull { file ->
            try {
                val content = file.readText()
                val backup = gson.fromJson(content, BackupContent::class.java)
                BackupInfo(
                    path = file.absolutePath,
                    timestamp = backup?.metadata?.timestamp ?: 0L,
                    version = backup?.metadata?.version ?: 0,
                    sizeBytes = file.length()
                )
            } catch (e: Exception) {
                Timber.w(e, "Failed to read backup info for ${file.name}")
                null
            }
        }

        Result.Success(backupInfos)
    } catch (e: Exception) {
        Timber.e(e, "Failed to list backups")
        Result.Error(DomainException.StorageError("Failed to list backups", e))
    }

    private fun storeBackupData(@NonNull data: BackupData) {
        encryptedPrefs.edit().apply {
            putString(BACKUP_DATA_KEY, gson.toJson(data))
            apply()
        }
    }

    private fun validateBackup(@NonNull backup: BackupContent) {
        if (backup.metadata.version != BACKUP_VERSION) {
            Timber.w("Backup version mismatch: expected $BACKUP_VERSION, got ${backup.metadata.version}")
        }
        if (backup.metadata.timestamp <= 0) {
            throw DomainException.ValidationError("Invalid backup timestamp")
        }
    }

    companion object {
        private const val PREFS_NAME = "deproof_encrypted_prefs"
        private const val BACKUP_DATA_KEY = "backup_data"
        private const val BACKUP_VERSION = 1
        private const val BACKUP_FILE_EXTENSION = ".backup"
    }
}

data class BackupData(
    @NonNull val receiptIds: List<String> = emptyList(),
    @NonNull val transactionHashes: List<String> = emptyList(),
    @NonNull val verdicts: Map<String, String> = emptyMap(),
    @NonNull val metadata: Map<String, String> = emptyMap()
)

data class BackupMetadata(
    val timestamp: Long,
    val version: Int,
    @NonNull val deviceId: String
)

data class BackupContent(
    @NonNull val metadata: BackupMetadata,
    @NonNull val data: BackupData
)

data class BackupInfo(
    @NonNull val path: String,
    val timestamp: Long,
    val version: Int,
    val sizeBytes: Long
)
