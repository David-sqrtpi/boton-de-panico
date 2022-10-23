package com.example.botondepanicov1.util

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.models.WiFiFrame
import java.util.*

class WiFiFrameUtils {
    companion object {
        fun wifiFrameToHashMap(wiFiFrame: WiFiFrame?): HashMap<String, String> {
            val message = HashMap<String, String>()
            if (wiFiFrame == null) {
                return message
            }

            message["u"] = wiFiFrame.username
            message["n"] = wiFiFrame.deviceName
            message["i"] = wiFiFrame.uuid
            message["o"] = wiFiFrame.longitude.toString()
            message["a"] = wiFiFrame.latitude.toString()
            message["d"] = Encoder.dateToString(Date())
            message["r"] = wiFiFrame.role.toString()

            return message
        }

        fun hashMapToWiFiFrame(message: MutableMap<String, String>): WiFiFrame {
            return WiFiFrame().apply {
                username = message["u"]!!
                deviceName = message["n"]!!
                longitude = java.lang.Double.valueOf(message["o"]!!)
                latitude = java.lang.Double.valueOf(message["a"]!!)
                date = message["d"]!!
                uuid = message["i"]!!
                role = message["r"]!!.toInt()
            }
        }

        fun buildMyWiFiFrame(context: Context): WiFiFrame {
            val sharedPreferences = context.getSharedPreferences(
                Constants.PREFERENCES_KEY,
                AppCompatActivity.MODE_PRIVATE
            )

            return WiFiFrame().apply {
                username = sharedPreferences
                    .getString(Constants.PREFERENCES_USERNAME, "Usuario cercano").toString()
                deviceName = "${Build.MANUFACTURER.uppercase(Locale.ROOT)} ${Build.MODEL}"
                uuid = sharedPreferences
                    .getString(Constants.PREFERENCES_UUID, "TODO").toString()
            }
        }
    }
}