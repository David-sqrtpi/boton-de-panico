package com.example.botondepanicov1.wifi_direct

import java.text.SimpleDateFormat
import java.util.*

class Encoder {
    companion object {
        fun dateToString(date: Date?): String? {
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            return formatter.format(date)
        }
    }
}