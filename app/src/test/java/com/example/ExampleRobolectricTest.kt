package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.admob.RewardPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MY IPTV", appName)
  }

  @Test
  fun `reward preferences saves and restores progress`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = RewardPreferences(context)

    prefs.setProgress(3)
    assertEquals(3, prefs.getProgress())

    val status = prefs.checkStatus()
    assertEquals(3, status.progress)
    assertFalse(status.isAdFree)
  }

  @Test
  fun `reward preferences activates 24 hours ad free upon 5 completions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = RewardPreferences(context)

    prefs.setProgress(5)
    prefs.activateAdFree24Hours()

    val status = prefs.checkStatus()
    assertTrue(status.isAdFree)
    assertEquals(5, status.progress)
    assertTrue(status.remainingMs > 0L)
  }
}
