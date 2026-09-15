package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.EVCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VoltLedger", appName)
  }

  @Test
  fun `test ev capacity and buffer formula`() {
    val declared = 60.0
    val expectedBuffer = 1.5 + (0.025 * 60.0) // 3.0
    val expectedUsable = 60.0 - expectedBuffer // 57.0

    assertEquals(expectedBuffer, EVCalculator.calculateBuffer(declared), 0.001)
    assertEquals(expectedUsable, EVCalculator.calculateUsableCapacity(declared), 0.001)
  }
}
