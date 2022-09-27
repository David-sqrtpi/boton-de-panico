package com.example.botondepanicov1.wifi_direct

import com.google.android.gms.maps.model.Marker

data class Ingredient(
    var username: String = "Usuario cercano",
    var deviceName: String = "Dispositivo cercano",
    var deviceAddress: String = "",
    var longitude: Double = 0.0,
    var latitude: Double = 0.0,
    var date: String = "", //TODO enviar la fecha por el wifi (para saber la fecha remota de actualización GPS)
    var distance: Double = 0.0,
    var marker: Marker? = null
)