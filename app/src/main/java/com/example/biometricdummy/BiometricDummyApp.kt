package com.example.biometricdummy

import android.app.Application

class BiometricDummyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ContextProvider.contextProvider?.setApplicationContext(this)
    }
}