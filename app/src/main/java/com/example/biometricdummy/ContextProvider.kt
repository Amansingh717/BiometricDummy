package com.example.biometricdummy

class ContextProvider private constructor() {
    private var applicationContext: BiometricDummyApp? = null
    fun provideContext(): BiometricDummyApp? {
        return applicationContext
    }

    fun setApplicationContext(context: BiometricDummyApp?) {
        applicationContext = context
    }

    companion object {
        private var mContextProvider: ContextProvider? = null
        val contextProvider: ContextProvider?
            get() {
                if (mContextProvider == null) {
                    synchronized(ContextProvider::class.java) {
                        mContextProvider = ContextProvider()
                    }
                }
                return mContextProvider
            }
    }
}