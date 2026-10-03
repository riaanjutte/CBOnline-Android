package io.github.riaanjutte.cbonline

import android.app.Application

class CbOnlineApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
