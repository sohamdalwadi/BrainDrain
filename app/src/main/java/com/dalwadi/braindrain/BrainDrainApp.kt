package com.dalwadi.braindrain

import android.app.Application
import com.dalwadi.braindrain.core.data.PreferencesRepository

class BrainDrainApp : Application() {

    lateinit var preferencesRepository: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = PreferencesRepository(applicationContext)
    }

    companion object {
        lateinit var instance: BrainDrainApp
            private set
    }
}
