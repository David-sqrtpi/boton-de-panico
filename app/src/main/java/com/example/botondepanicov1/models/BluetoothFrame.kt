package com.example.botondepanicov1.models

import org.altbeacon.beacon.Identifier

class BluetoothFrame {
    var identifier: Identifier? = null
    var distance: Double = 0.0
        get() = String.format("%.3f", field).toDouble()
}