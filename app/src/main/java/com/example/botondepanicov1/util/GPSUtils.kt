package com.example.botondepanicov1.util

import android.location.Location
import com.example.botondepanicov1.models.WiFiFrame

class GPSUtils {
    companion object {
        fun calculateDistance(wiFiFrame0: WiFiFrame, wiFiFrame1: WiFiFrame): Double {
            return calculateDistance(
                wiFiFrame0.latitude,
                wiFiFrame0.longitude,
                wiFiFrame1.latitude,
                wiFiFrame1.longitude
            )
        }

        fun calculateDistance(
            latitude0: Double,
            longitude0: Double,
            latitude1: Double,
            longitude1: Double
        ): Double {
            val location0 = Location("")
            location0.latitude = latitude0
            location0.longitude = longitude0

            val location1 = Location("")
            location1.latitude = latitude1
            location1.longitude = longitude1

            return location0.distanceTo(location1).toDouble()
        }
    }
}