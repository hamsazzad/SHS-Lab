package com.shslab.app

import android.app.Application
import androidx.multidex.MultiDex
import android.content.Context

class SHSApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SHSApplication
            private set
    }
}
