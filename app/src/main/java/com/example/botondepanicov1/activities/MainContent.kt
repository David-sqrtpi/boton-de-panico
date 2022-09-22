package com.example.botondepanicov1.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.R
import com.example.botondepanicov1.services.AlarmService
import com.example.botondepanicov1.util.Constants
import com.example.botondepanicov1.util.GPSUtils
import com.example.botondepanicov1.util.IngredientUtils
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
    private var toggleAlarm = false //TODO averiguar si se puede simplificar
    private lateinit var toggleAlarmIB: ImageButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiP2pChannel: WifiP2pManager.Channel
    private lateinit var wifiP2pManager: WifiP2pManager

    private lateinit var currentLocation: Location
    private lateinit var locationRequest: LocationRequest

    private var myself: Ingredient? = null
    private val ingredients = ArrayList<Ingredient>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_content)

        createLocationRequest()
        init()
        locationUpdates()
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
    }

    private fun init() {
        wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        wifiP2pChannel = wifiP2pManager.initialize(applicationContext, mainLooper, null)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

    private fun setupMyLocation(location: Location) {
        myself = Ingredient().apply {
            username = getSharedPreferences(
                Constants.PREFERENCES_KEY,
                MODE_PRIVATE
            ).getString(Constants.PREFERENCES_USERNAME, "Usuario cercano")!!
            deviceName = "${Build.MANUFACTURER.uppercase(Locale.ROOT)} ${Build.MODEL}"
            deviceAddress = Constants.MYSELF_ADDRESS
            latitude = location.latitude
            longitude = location.longitude
            date = Encoder.dateToString(Date())!!
        }

        createMarker(myself!!)
        updateCamera(myself!!)
        sendLocationUpdate() //TODO aquí puede implementarse la logica del update marker
        discoverService()
    }

    //TODO iniciar y detener el servicio dependiendo de la bandera. También detener el servicio
    //Cuando termina la actividad
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

    @SuppressLint("MissingPermission")
    private fun locationUpdates() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager

        try {
            lm.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 10000, 0f
            ) { location ->
                println("TAG_WIFI $location")
                println("TAG_WIFI ${location.longitude}, ${location.latitude}")
                if (myself == null) {
                    setupMyLocation(location)
                } else {
                    updateMarker(myself!!, location)
                }
                sendLocationUpdate() //TODO aquí puede implementarse la logica del update marker
            }
        } catch (e: Exception) {
            Log.v("Sergio", e.toString())
        }
    }

    private fun updateMarker(ingredient: Ingredient, location: Location) {
        ingredient.marker?.position = LatLng(location.latitude, location.longitude)

    }

    private fun createMarker(ingredient: Ingredient) {
        ingredient.marker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(ingredient.latitude, ingredient.longitude))
                .title(ingredient.username)
                .visible(true)
        )
    }

    //Todo hacer el caso en el que está en otro zoom. Actualizar la cámara pero no el zoom...
    // ahora que pienso, si se mueve la cámara cuando el usuario está viendo otra ubicación puede ser incómodo
    private fun updateCamera(ingredient: Ingredient) {
        val cameraUpdate: CameraUpdate =
            CameraUpdateFactory.newLatLngZoom(
                LatLng(ingredient.latitude, ingredient.longitude),
                15f
            )

        googleMap.moveCamera(cameraUpdate)
    }

    //TODO ejecutar esto cada vez que cambia la ubicación
    private fun sendLocationUpdate() {
        val message = IngredientUtils.ingredientToHashMap(myself!!)

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

    //todo Primero setear el myself
    private fun discoverService() {
        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { _, record, device ->
            Log.d(
                Constants.TAG_WIFI,
                "DnsSdTxtRecordListener 2" +
                        "\n record: $record"
            )

/*            val ingredient = Ingredient().apply {
                deviceName = record["index"]!!
                deviceAddress = device.deviceAddress
                longitude = java.lang.Double.valueOf(record["latitude"]!!)
                latitude = java.lang.Double.valueOf(record["longitude"]!!)
                date = record["date"]!!
                distance = GPSUtils.calculateDistance(currentLocation, latitude, longitude)
            }*/

            val ingredientV2 = IngredientUtils.hashMapToIngredient(record)
            createMarker(ingredientV2)
            ingredientV2.deviceAddress = device.deviceAddress
            ingredientV2.deviceName = device.deviceName

            ingredientV2.distance = GPSUtils.calculateDistance(
                myself!!.latitude,
                myself!!.longitude,
                ingredientV2.latitude,
                ingredientV2.longitude
            )

            println("TAG_WIFI. $ingredientV2")

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
        }

        wifiP2pManager.setDnsSdResponseListeners(wifiP2pChannel, null, txtListener)

        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()

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
        //TODO iniciarlizar en el init
        locationRequest = LocationRequest.create().apply {
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