package com.example.botondepanicov1.models

import com.google.android.gms.maps.model.Marker
import org.altbeacon.beacon.Identifier

data class BluetoothFrame (
    var identifier: Identifier? = null,
    var distance: Double = 0.0,
    var date: String = "",
)