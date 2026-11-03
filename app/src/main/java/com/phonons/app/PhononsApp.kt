package com.phonons.app

import android.app.Application
import com.phonons.app.data.CrashLog
import com.phonons.app.data.Repo

class PhononsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        Repo.init(this)
    }
}
