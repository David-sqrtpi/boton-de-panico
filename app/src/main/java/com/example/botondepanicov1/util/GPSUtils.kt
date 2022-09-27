package com.example.botondepanicov1.util

import android.location.Location

class GPSUtils {
    companion object {
         fun calculateDistance(latitude0: Double,
                               longitude0: Double,
                               latitude1: Double,
                               longitude1: Double): Double {
            val location0 = Location("")
            location0.latitude = latitude0
            location0.longitude = longitude0

            val location1 = Location("")
            location1.latitude = latitude1
            location1.longitude = longitude1

            return location0.distanceTo(location1).toDouble()
        }

        fun calculateDistance(location0: Location, location1: Location): Double {
            return location0.distanceTo(location1).toDouble()
        }

        fun calculateDistance(location: Location, latitude: Double, longitude: Double): Double {
            val location1 = Location("")
            location1.latitude = latitude
            location1.longitude = longitude

            return location.distanceTo(location1).toDouble()
        }
    }
}