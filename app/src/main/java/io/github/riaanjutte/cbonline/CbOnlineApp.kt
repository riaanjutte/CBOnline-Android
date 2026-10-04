package io.github.riaanjutte.cbonline

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import io.github.riaanjutte.cbonline.notify.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        // Friend alerts that are switched on but have no scheduled checks (e.g. a restored backup) start again
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.friendAlertSwitch.ensureRunning()
            } catch (e: Exception) {
                Log.w("CBOnline", "Restarting friend alerts failed", e)
            }
        }
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
