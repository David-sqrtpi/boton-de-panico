package com.example.botondepanicov1.wifi_direct

import com.example.botondepanicov1.core.Role
import com.google.android.gms.maps.model.Marker
import java.util.*

class Ingredient {
    var username: String = "Yo"
    var deviceName: String = "Dispositivo"
    var deviceAddress: String = ""
    var longitude: Double = 0.0
    var latitude: Double = 0.0
    var distance: Double = 0.0
        get() = String.format("%.3f", field).toDouble()

    var date: String = ""
    var marker: Marker? = null
    var role: Int = Role.SURVIVOR.ordinal
}