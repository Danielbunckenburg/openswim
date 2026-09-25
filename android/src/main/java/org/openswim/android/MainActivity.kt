package org.openswim.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/** Android entry point. CloudRepository owns the authenticated Supabase session. */
class MainActivity : ComponentActivity() {
    private var firstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = CloudRepository(this)
        repository.acceptAuthRedirect(intent?.data)
        setContent { OpenSwimBrandedApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        repository.acceptAuthRedirect(intent.data)
    }

    override fun onResume() {
        super.onResume()
        if (firstResume) firstResume = false else repository.refresh()
    }
}

internal lateinit var repository: CloudRepository
