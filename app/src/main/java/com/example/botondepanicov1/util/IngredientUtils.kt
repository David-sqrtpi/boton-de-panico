package com.example.botondepanicov1.util

import android.net.wifi.p2p.WifiP2pDevice
import com.example.botondepanicov1.wifi_direct.Ingredient

class IngredientUtils {
    companion object {
        fun ingredientToHashMap(ingredient: Ingredient): HashMap<String, String> {
            val message = HashMap<String, String>()

            message["username"] = ingredient.username
            message["deviceName"] = ingredient.deviceName
            message["deviceAdress"] = ingredient.deviceAddress
            message["longitude"] = ingredient.longitude.toString()
            message["latitude"] = ingredient.latitude.toString()
            message["date"] = ingredient.date

            return message
        }

        fun hashMapToIngredient(
            message: MutableMap<String, String>,
            device: WifiP2pDevice,
            myself: Ingredient?
        ): Ingredient {
            val ingredient = Ingredient().apply {
                username = message["username"]!!
                longitude = java.lang.Double.valueOf(message["longitude"]!!)
                latitude = java.lang.Double.valueOf(message["latitude"]!!)
                date = message["date"]!!
                deviceAddress = device.deviceAddress
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