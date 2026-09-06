package com.ps.dinear.utils

import android.content.Context
import com.google.ar.core.ArCoreApk

object ArCoreHelper {
    /**
     * Checks if ARCore is supported on this device.
     * Returns true if supported (even if not currently installed/up to date),
     * false if explicitly unsupported.
     */
    fun isArCoreSupported(context: Context): Boolean {
        val availability = ArCoreApk.getInstance().checkAvailability(context)
        return availability.isSupported || availability == ArCoreApk.Availability.UNKNOWN_CHECKING
    }
}
