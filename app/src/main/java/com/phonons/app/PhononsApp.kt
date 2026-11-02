package com.phonons.app

import android.app.Application
import com.phonons.app.data.Repo

class PhononsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
    }
}
