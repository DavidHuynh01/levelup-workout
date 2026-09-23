package com.davidhuynh.levelup

import android.app.Application
import com.davidhuynh.levelup.di.AppContainer

class LevelUpApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.warmUp()
    }
}
