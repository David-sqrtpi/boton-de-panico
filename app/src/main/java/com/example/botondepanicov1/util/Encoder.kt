package com.example.botondepanicov1.util

import java.text.SimpleDateFormat
import java.util.*

class Encoder {
    companion object {
        fun dateToString(date: Date): String {
            val formatter =  SimpleDateFormat("d 'de' MMMM\nh:m:s a")
            return formatter.format(date)
        }
    }
}