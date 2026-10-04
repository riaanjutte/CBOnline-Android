package io.github.riaanjutte.cbonline

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.github.riaanjutte.cbonline.notify.Notifications

class CbOnlineApp : Application() {

    lateinit var container: AppContainer
        private set

    /** True while one of the app's screens is visible; friend alerts stay quiet then. */
    @Volatile
    var inForeground: Boolean = false
        private set
    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                inForeground = true
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                inForeground = startedActivities > 0
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
