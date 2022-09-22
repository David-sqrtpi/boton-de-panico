package com.example.botondepanicov1.util

import com.example.botondepanicov1.wifi_direct.Encoder
import com.example.botondepanicov1.wifi_direct.Ingredient
import java.util.*
import kotlin.collections.HashMap

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

        fun hashMapToIngredient(message: MutableMap<String, String>): Ingredient {
            val ingredient = Ingredient().apply {
                username = message["username"]!!
                longitude = java.lang.Double.valueOf(message["longitude"]!!)
                latitude = java.lang.Double.valueOf(message["latitude"]!!)
                date = message["date"]!!
            }

            return ingredient
        }
    }
}