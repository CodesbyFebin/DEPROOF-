package com.deproof

import android.app.Application
import timber.log.Timber

class DepRoofApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG_MODE) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}

private class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= android.util.Log.WARN) {
            android.util.Log.println(priority, tag, message)
        }
        if (t != null) {
            android.util.Log.println(priority, tag, android.util.Log.getStackTraceString(t))
        }
    }
}
