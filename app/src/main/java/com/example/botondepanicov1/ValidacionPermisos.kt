package com.example.botondepanicov1

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class ValidacionPermisos {
    companion object {
        fun isLocationPermissionGranted(context: Context): Boolean {
            return (
                    ActivityCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    )
        }
    }
}