package com.example.botondepanicov1.models

import com.google.android.gms.maps.model.Marker
import org.altbeacon.beacon.Identifier

data class BluetoothFrame (
    var username: String = "Usuario cercano",
    var deviceName: String = "Dispositivo cercano",
    //var deviceAddress: String = "",
    var longitude: Double = 0.0,
    var latitude: Double = 0.0,
    var marker: Marker? = null,


    var identifier: Identifier? = null,
    var distance: Double = 0.0,
    var date: String = "",
    var batteryLevel: Float = 0f
)