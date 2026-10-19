package com.example

import com.example.util.AppUpdateManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun testIsRemoteNewer_newerVersions() {
        // Simple patch bump
        assertTrue(AppUpdateManager.isRemoteNewer("2.5.9", "2.5.8"))
        // Minor bump
        assertTrue(AppUpdateManager.isRemoteNewer("2.6.0", "2.5.9"))
        // Major bump
        assertTrue(AppUpdateManager.isRemoteNewer("3.0.0", "2.5.9"))
        // Two digit patch
        assertTrue(AppUpdateManager.isRemoteNewer("2.5.10", "2.5.9"))
        assertTrue(AppUpdateManager.isRemoteNewer("2.5.10", "2.5.8"))
    }

    @Test
    fun testIsRemoteNewer_sameOrOlderVersions() {
        // Same version
        assertFalse(AppUpdateManager.isRemoteNewer("2.5.9", "2.5.9"))
        // Older version
        assertFalse(AppUpdateManager.isRemoteNewer("2.5.8", "2.5.9"))
        assertFalse(AppUpdateManager.isRemoteNewer("2.4.1", "2.5.9"))
        assertFalse(AppUpdateManager.isRemoteNewer("1.9.9", "2.5.9"))
    }

    @Test
    fun testIsRemoteNewer_withVPrefix() {
        assertTrue(AppUpdateManager.isRemoteNewer("v2.5.9", "2.5.8"))
        assertTrue(AppUpdateManager.isRemoteNewer("v2.6.0", "v2.5.9"))
        assertFalse(AppUpdateManager.isRemoteNewer("v2.5.8", "2.5.8"))
    }
}
