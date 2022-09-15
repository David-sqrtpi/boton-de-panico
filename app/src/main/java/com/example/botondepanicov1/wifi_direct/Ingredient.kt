package com.example.botondepanicov1.wifi_direct

data class Ingredient(
    var username: String = "Usuario cercano",
    var deviceName: String = "Dispositivo cercano",
    var deviceAddress: String = "",
    var longitude: Double = 0.0,
    var latitude: Double = 0.0,
    var date: String = "",
    var distance: Double = 0.0
)