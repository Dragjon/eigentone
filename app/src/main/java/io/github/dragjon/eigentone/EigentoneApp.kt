package io.github.dragjon.eigentone

import android.app.Application
import io.github.dragjon.eigentone.data.CrashLog
import io.github.dragjon.eigentone.data.Repo

class EigentoneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        Repo.init(this)
    }
}
