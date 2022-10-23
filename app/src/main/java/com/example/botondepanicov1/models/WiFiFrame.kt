package com.example.botondepanicov1.models

//This class represents transmittable fields across Wi-Fi Direct
class WiFiFrame {
    var username: String = "Yo" // "u" in HashMap<String, String>
    var deviceName: String = "" // "n" in HashMap<String, String>
    var uuid: String = "" // "i" in HashMap<String, String>
    var longitude: Double = 0.0 // "o" in HashMap<String, String>
    var latitude: Double = 0.0 // "a" in HashMap<String, String>
    var date: String = "" // "d" in HashMap<String, String>
    var role: Int = Role.SURVIVOR.ordinal // "r" in HashMap<String, String>
}