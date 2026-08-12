package com.example

import android.app.Application
import com.example.data.JarvisDatabase

class JarvisApplication : Application() {
    val database: JarvisDatabase by lazy {
        JarvisDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: JarvisApplication
            private set
    }
}
