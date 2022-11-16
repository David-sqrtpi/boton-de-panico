package com.example.botondepanicov1.util

class Constants {
    companion object {
        const val PREFERENCES_KEY = "PREFERENCES" //Key for sharedPreferences
        const val PREFERENCES_USERNAME = "NAME"
        const val PREFERENCES_UUID = "UUID"

        const val REQUEST_CHECK_SETTINGS = 1 //Request permissions to enable location
        const val REQUEST_ENABLE_WIFI = 2
        const val REQUEST_ENABLE_BLUETOOTH = 3

        const val TAG_WIFI = "BDP_WIFI"
        const val TAG_BT = "BDP_BLUETOOTH"
        const val TAG_MAPS = "BDP_MAPS"
        const val TAG_LOCATION = "BDP_LOCATION"
    }
}