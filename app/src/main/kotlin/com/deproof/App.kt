package com.deproof

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.deproof.data.local.DepRoofDatabase

class App : Application() {

    companion object {
        private lateinit var instance: App
        private lateinit var database: DepRoofDatabase

        fun getInstance(): App = instance

        fun getDatabase(): DepRoofDatabase = database
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        initializeDatabase()
        initializeLogging()
    }

    private fun initializeDatabase() {
        database = Room.databaseBuilder(
            this,
            DepRoofDatabase::class.java,
            DepRoofDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    private fun initializeLogging() {
        // Initialize logging framework here
        // Example: Timber.plant(Timber.DebugTree())
    }
}
