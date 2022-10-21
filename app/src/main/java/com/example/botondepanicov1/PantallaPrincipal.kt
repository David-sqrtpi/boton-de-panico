package com.example.botondepanicov1

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.activities.MainContent
import com.example.botondepanicov1.util.Constants
//import com.example.botondepanicov1.util.StorageManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_pantalla_principal.*

//TODO check location request, bluetooth request and wifi. Think about a new activity for asking all of this
class PantallaPrincipal : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_principal)

        sos_button.setOnClickListener {
            val intent = Intent(this, MainContent::class.java)
            startActivity(intent)
        }

        if (!ValidacionPermisos.isLocationPermissionGranted(this)) {
            requestLocationPermission()
        } else {
            whenLocationPermissionGranted()
        }
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
            //StorageManager.storeRawToLocal(this)
            saludo.text = "Hola, $name"
            edit_name.setOnClickListener{
                startActivity(intent)
            }
        }
    }

    private fun createLocationRequest() {
        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_LOW_POWER
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this,
                        Constants.REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    /*//encender o apagar bluetooth
    private fun encenderBluetooth() {
        if (!btAdapter!!.isEnabled) {
            btAdapter!!.enable()
        }
    }*/

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            Constants.REQUEST_CHECK_SETTINGS -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Se ha activado la ubicación", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No se ha activado la ubicación", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}