package com.example.botondepanicov1.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.R
import com.example.botondepanicov1.databinding.ActivityPantallaPrincipalBinding
import com.example.botondepanicov1.util.Constants
import com.example.botondepanicov1.util.Constants.Companion.REQUEST_CHECK_SETTINGS
import com.example.botondepanicov1.util.Constants.Companion.REQUEST_ENABLE_BLUETOOTH
import com.example.botondepanicov1.util.Constants.Companion.REQUEST_ENABLE_WIFI
import com.example.botondepanicov1.util.PermissionsCheck
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes.RESOLUTION_REQUIRED
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE
import com.google.android.gms.tasks.Task

class PantallaPrincipal : AppCompatActivity() {
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) -> {
                when {
                    (permissions[Manifest.permission.BLUETOOTH_CONNECT] == false
                            || permissions[Manifest.permission.ACCESS_FINE_LOCATION] == false
                            || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == false) -> {
                        requestPermissions()
                    }
                    else -> {
                        createLocationRequest()
                    }
                }
            }
            else -> {
                when {
                    (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == false
                            || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == false) -> {
                        requestPermissions()
                    }
                    else -> {
                        createLocationRequest()
                    }
                }
            }
        }
    }

    private lateinit var binding: ActivityPantallaPrincipalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPantallaPrincipalBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (!PermissionsCheck.isLocationPermissionGranted(this) || !PermissionsCheck.isBtPermissionGranted(
                this
            )
        ) {
            requestPermissions()
        } else {
            createLocationRequest()
        }
    }

    private fun requestPermissions() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        )
    }

    private fun onSetupCompleted() {
        val sharedPreference = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
        val name = sharedPreference.getString(Constants.PREFERENCES_USERNAME, null)
        val intent = Intent(this, Login::class.java)

        if (name == null) {
            startActivity(intent)
            finish()
        } else {
            binding.sosButton.setOnClickListener {
                startActivity(Intent(this, MainContent::class.java))
            }

            binding.saludo.text = "¡Hola $name!"
            binding.editName.setOnClickListener {
                startActivity(intent)
                finish()
            }
            binding.saludo.setOnClickListener {
                startActivity(intent)
                finish()
            }
        }
    }

    private fun createLocationRequest() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_LOW_POWER, 5000)

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest.build())

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            enableBluetooth()
        }

        task.addOnFailureListener { exception ->
            val apiException = exception as ApiException

            when (apiException.statusCode) {
                RESOLUTION_REQUIRED -> {
                    try {
                        ResolvableApiException(exception.status)
                            .startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }

                SETTINGS_CHANGE_UNAVAILABLE -> {
                    Toast.makeText(
                        this,
                        "El dispositivo no es compatible. No se encuentra sensor GPS",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun enableBluetooth() {
        val btManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = btManager.adapter

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            bluetoothAdapter.enable()
            enableWifi()
        } else if (!bluetoothAdapter.isEnabled) {
            //Bluetooth Request
            Toast.makeText(this, "Por favor activa el Bluetooth", Toast.LENGTH_SHORT).show()
            startActivityForResult(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                REQUEST_ENABLE_BLUETOOTH
            )
        } else {
            enableWifi()
        }
    }

    private fun enableWifi() {
        val wifiManager =
            this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            wifiManager.isWifiEnabled = true
            onSetupCompleted()
        } else if (!wifiManager.isWifiEnabled) {
            //Wifi activation
            Toast.makeText(this, "Por favor activa el Wi-Fi", Toast.LENGTH_SHORT).show()
            val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
            startActivityForResult(panelIntent, REQUEST_ENABLE_WIFI)
        } else {
            onSetupCompleted()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == RESULT_OK) {
                    enableBluetooth()
                } else {
                    Toast.makeText(
                        this,
                        "Active la ubicación del dispositivo para continuar",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    //createLocationRequest
                }
            }

            REQUEST_ENABLE_BLUETOOTH -> {
                if (resultCode == RESULT_OK) {
                    enableWifi()
                } else {
                    Toast.makeText(this, "Active el Bluetooth para continuar", Toast.LENGTH_SHORT)
                        .show()
                    //enableBluetooth
                }
            }

            REQUEST_ENABLE_WIFI -> {
                val wifiManager =
                    this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

                if (wifiManager.isWifiEnabled) {
                    onSetupCompleted()
                } else {
                    Toast.makeText(this, "Active el Wi-Fi para continuar", Toast.LENGTH_SHORT)
                        .show()
                    //enableWifi
                }
            }
        }
    }
}