package com.example.botondepanicov1

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.activities.MainContent
import com.example.botondepanicov1.util.Constants
import com.example.botondepanicov1.util.StorageManager
import kotlinx.android.synthetic.main.activity_pantalla_principal.*

class PantallaPrincipal : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_principal)

        if (!ValidacionPermisos.isLocationPermissionGranted(this)) {
            requestLocationPermission()
        } else {
            whenLocationPermissionGranted()
        }
    }

    fun onClickSolicitarAyuda(v: View) {
        val intent = Intent(this, MainContent::class.java)
        startActivity(intent)
    }

    private fun requestLocationPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) -> {
                    whenLocationPermissionGranted()
                }
                (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) -> {
                    //Creo que solo sale en android 12
                    Toast.makeText(this, "Coarse location", Toast.LENGTH_SHORT).show()
                    whenLocationPermissionGranted()
                }
                else -> {
                    Toast.makeText(
                        this,
                        "El permiso de ubicación es necesario para ver dispositivos cercanos",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }

        }

        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun whenLocationPermissionGranted() {
        val sharedPreference = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
        val name = sharedPreference.getString(Constants.PREFERENCES_USERNAME, null)
        val intent = Intent(this, Login::class.java)

        if(name == null){
            startActivity(intent)
            finish()
        } else {
            StorageManager.storeRawToLocal(this)
            saludo.text = "Hola, $name"
            edit_name.setOnClickListener{
                startActivity(intent)
            }
        }
    }
}