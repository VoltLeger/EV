package com.example

import com.example.util.AppUpdateManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun testIsRemoteNewer_newerVersions() {
        // Simple patch bump
        assertTrue(AppUpdateManager.isRemoteNewer("2.6.1", "2.6.0"))
        // Minor bump
        assertTrue(AppUpdateManager.isRemoteNewer("2.7.0", "2.6.0"))
        // Major bump
        assertTrue(AppUpdateManager.isRemoteNewer("3.0.0", "2.6.0"))
        // Two digit patch
        assertTrue(AppUpdateManager.isRemoteNewer("2.6.10", "2.6.0"))
        assertTrue(AppUpdateManager.isRemoteNewer("2.6.10", "2.6.9"))
    }

    @Test
    fun testIsRemoteNewer_sameOrOlderVersions() {
        // Same version
        assertFalse(AppUpdateManager.isRemoteNewer("2.6.0", "2.6.0"))
        // Older version
        assertFalse(AppUpdateManager.isRemoteNewer("2.5.9", "2.6.0"))
        assertFalse(AppUpdateManager.isRemoteNewer("2.5.8", "2.6.0"))
        assertFalse(AppUpdateManager.isRemoteNewer("1.9.9", "2.6.0"))
    }

    @Test
    fun testIsRemoteNewer_withVPrefix() {
        assertTrue(AppUpdateManager.isRemoteNewer("v2.6.1", "2.6.0"))
        assertTrue(AppUpdateManager.isRemoteNewer("v2.7.0", "v2.6.0"))
        assertFalse(AppUpdateManager.isRemoteNewer("v2.6.0", "2.6.0"))
    }
}
