package com.example.botondepanicov1.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.R
import com.example.botondepanicov1.services.AlarmService
import com.example.botondepanicov1.util.Constants
import com.example.botondepanicov1.util.GPSUtils
import com.example.botondepanicov1.wifi_direct.Encoder
import com.example.botondepanicov1.wifi_direct.Ingredient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import java.util.*


class MainContent : AppCompatActivity(), OnMapReadyCallback {
    private var toggleAlarm = false
    private lateinit var toggleAlarmIB: ImageButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiP2pChannel: WifiP2pManager.Channel
    private lateinit var wifiP2pManager: WifiP2pManager

    private lateinit var currentLocation: Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createLocationRequest()
        init()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContentView(R.layout.activity_main_content)
        toggleAlarmIB = findViewById(R.id.toggle_alarm)
        toggleAlarmIB.setImageResource(R.drawable.alarm_off)

        toggleAlarmIB.setOnClickListener {
            if (toggleAlarm) {
                toggleOff()
            } else {
                toggleOn()
            }
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun init() {
        wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        wifiP2pChannel = wifiP2pManager.initialize(applicationContext, mainLooper, null)
    }

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

    private fun toggleOn() {
        toggleAlarmIB.setImageResource(R.drawable.alarm)

        val intent = Intent(this, AlarmService::class.java)
        startService(intent)
        toggleAlarm = !toggleAlarm
    }

    private fun toggleOff() {
        toggleAlarmIB.setImageResource(R.drawable.alarm_off)

        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        toggleAlarm = !toggleAlarm
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        /*val file = File(applicationContext.filesDir, "bogota_tiles.mbtiles")

        val tileProvider: TileProvider =
            MapBoxOfflineTileProvider(file)

        googleMap
            .addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))*/

        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        googleMap.setMaxZoomPreference(100f)

        googleMap.setMinZoomPreference(5f)

        getLastKnownLocation()
    }

    //Todo do this until implementation of updates
    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation() {
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    println(location.toString())
                    createMarker(location, "Yo")
                    updateCamera(location)

                    currentLocation = location

                    startRegistration(location)
                    discoverService()
                }
            }
    }

    private fun createMarker(location: Location, title: String) {
        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(location.latitude, location.longitude))
                .title(title)
        )
    }

    //Todo hacer el caso en el que está en otro zoom. Actualizar la cámara pero no el zoom...
    // ahora que pienso, si se mueve la cámara cuando el usuario está viendo otra ubicación puede ser incómodo
    private fun updateCamera(location: Location) {
        val cameraUpdate: CameraUpdate =
            CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f)

        googleMap.moveCamera(cameraUpdate)
    }

    //TODO ejecutar esto cada vez que cambia la ubicación
    private fun startRegistration(location: Location) {
        val message: HashMap<String, String> = HashMap()

        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
        val username = sharedPreferences.getString(Constants.PREFERENCES_USERNAME, "Usuario")
            .toString()

        message["longitude"] = location.longitude.toString()
        message["latitude"] = location.latitude.toString()
        message["index"] = "deviceName"
        message["date"] = Encoder.dateToString(Date())!!
        message["username"] = username

        val serviceInfo =
            WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", message)

        wifiP2pManager.addLocalService(
            wifiP2pChannel,
            serviceInfo,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(Constants.TAG_WIFI, "Success! Adding local service 1")
                }

                override fun onFailure(arg0: Int) {
                    Log.d(
                        Constants.TAG_WIFI,
                        "Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY." +
                                "\n Adding local service 1"
                    )
                }
            })
    }

    private fun discoverService() {
        val servListener =
            WifiP2pManager.DnsSdServiceResponseListener { instanceName, registrationType, resourceType ->
                Log.d(
                    Constants.TAG_WIFI,
                    "DnsSdServiceResponseListener 2" +
                            "\n instanceName: $instanceName, registrationType: $registrationType, resourceType: $resourceType"
                )
            }

        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { _, record, device ->
            Log.d(
                Constants.TAG_WIFI,
                "DnsSdTxtRecordListener 2" +
                        "\n record: $record, device: $device"
            )

            println(record.toString())
            println(device.toString())

            val ingredient = Ingredient().apply {
                deviceName = record["index"]!!
                deviceAddress = device.deviceAddress
                longitude = java.lang.Double.valueOf(record["latitude"]!!)
                latitude = java.lang.Double.valueOf(record["longitude"]!!)
                date = record["date"]!!
                distance = GPSUtils.calculateDistance(currentLocation, latitude, longitude)
            }

            //todo why
            //distance = ingredient.distance

            /*if (!isObjectInArray(device.deviceAddress)) {
                ingredients.add(ingredient)

                val arrayList: ArrayList<Ingredient> = ingredients

                adapter!!.clear()
                for (i in arrayList.indices) {
                    adapter!!.add(arrayList[i])
                }

                lv.setSelection(adapter!!.count - 1)

                Log.d("Add device", java.lang.String.valueOf(adapter!!.count))
                Log.d("Add device", ingredient.deviceName + " device")
            }*/
            Log.d(Constants.TAG_WIFI, ingredient.toString() + 2)
        }

        wifiP2pManager.setDnsSdResponseListeners(wifiP2pChannel, servListener, txtListener)

        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance("_test", "_presence._tcp")

        wifiP2pManager.addServiceRequest(
            wifiP2pChannel,
            serviceRequest,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(Constants.TAG_WIFI, "Success! Adding service Request 3")
                }

                override fun onFailure(code: Int) {
                    Log.d(
                        Constants.TAG_WIFI,
                        "Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY." +
                                "\n Adding service request 3"
                    )
                }
            }
        )

        wifiP2pManager.discoverServices(
            wifiP2pChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(Constants.TAG_WIFI, "Success! Discovering services 4")
                }

                override fun onFailure(code: Int) {
                    Log.d(
                        Constants.TAG_WIFI,
                        "Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY." +
                                "\n Discovering services 4"
                    )
                }
            }
        )
    }

    private fun createLocationRequest() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
            maxWaitTime = 20000
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
}