package com.example.botondepanicov1.models

import com.google.android.gms.maps.model.Marker
import java.util.*

class Ingredient {
    var wiFiFrame: WiFiFrame = WiFiFrame()
    var bluetoothFrame: BluetoothFrame = BluetoothFrame()

    var btUpdateDate: Date? = null
    var marker: Marker? = null
    var gpsDistance: Double = 0.0
        get() = String.format("%.3f", field).toDouble()
}