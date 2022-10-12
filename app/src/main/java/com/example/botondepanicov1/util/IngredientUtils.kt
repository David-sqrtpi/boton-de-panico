package com.example.botondepanicov1.util

import android.net.wifi.p2p.WifiP2pDevice
import com.example.botondepanicov1.wifi_direct.Encoder
import com.example.botondepanicov1.wifi_direct.Ingredient
import java.util.*
import kotlin.collections.HashMap

class IngredientUtils {
    companion object {
        fun ingredientToHashMap(ingredient: Ingredient?): HashMap<String, String>? {
            val message = HashMap<String, String>()
            if(ingredient == null) {
                return message
            }

            message["u"] = ingredient.username
            message["n"] = ingredient.deviceName
            message["a"] = ingredient.deviceAddress
            message["o"] = ingredient.longitude.toString()
            message["a"] = ingredient.latitude.toString()
            message["d"] = Encoder.dateToString(Date())!!
            message["r"] = ingredient.role

            return message
        }

        fun hashMapToIngredient(
            message: MutableMap<String, String>,
            device: WifiP2pDevice,
            myself: Ingredient?
        ): Ingredient {
            val ingredient = Ingredient().apply {
                username = message["u"]!!
                longitude = java.lang.Double.valueOf(message["o"]!!)
                latitude = java.lang.Double.valueOf(message["a"]!!)
                date = message["d"]!!
                deviceAddress = device.deviceAddress
                role = message["r"]!!
            }

            if (myself != null) {
                ingredient.distance =
                    GPSUtils.calculateDistance(
                        ingredient.latitude,
                        ingredient.longitude,
                        myself.latitude,
                        myself.longitude
                    )
            }

            return ingredient
        }
    }
}