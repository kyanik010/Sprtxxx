package com.example

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun launcherActivityStartsAndRemainsVisible() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse("MainActivity unexpectedly finished", activity.isFinishing)
                assertNotNull("Activity window was not created", activity.window)
                assertNotNull("Activity content view was not created", activity.window.decorView.rootView)
            }
        }
    }
}
