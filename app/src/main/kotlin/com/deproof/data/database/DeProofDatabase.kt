package com.deproof.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.deproof.data.database.converters.CustodyDecisionStatusConverter
import com.deproof.data.database.converters.InstructionListConverter
import com.deproof.data.database.converters.SignatureStatusConverter
import com.deproof.data.database.dao.CustodyDecisionDao
import com.deproof.data.database.dao.PendingApprovalDao
import com.deproof.data.database.entity.CustodyDecisionEntity
import com.deproof.data.database.entity.PendingApprovalEntity

@Database(
    entities = [
        CustodyDecisionEntity::class,
        PendingApprovalEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(
    InstructionListConverter::class,
    CustodyDecisionStatusConverter::class,
    SignatureStatusConverter::class
)
abstract class DeProofDatabase : RoomDatabase() {

    abstract fun custodyDecisionDao(): CustodyDecisionDao
    abstract fun pendingApprovalDao(): PendingApprovalDao

    companion object {
        private const val DATABASE_NAME = "deproof_phase3.db"

        @Volatile
        private var instance: DeProofDatabase? = null

        fun getInstance(context: Context): DeProofDatabase {
            return instance ?: synchronized(this) {
                instance ?: createDatabase(context).also { instance = it }
            }
        }

        private fun createDatabase(context: Context): DeProofDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                DeProofDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // For development; use migrations in production
            .build()
        }
    }
}
