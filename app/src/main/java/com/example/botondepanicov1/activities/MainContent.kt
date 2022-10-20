package com.example.botondepanicov1.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.R
import com.example.botondepanicov1.adapters.IngredientAdapter
import com.example.botondepanicov1.core.Role
import com.example.botondepanicov1.services.AlarmService
import com.example.botondepanicov1.util.Constants
import com.example.botondepanicov1.util.IngredientUtils
import com.example.botondepanicov1.wifi_direct.Ingredient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main_content.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainContent : AppCompatActivity(), OnMapReadyCallback {
    private var toggleAlarm = false //TODO averiguar si se puede simplificar
    private lateinit var toggleAlarmIB: ImageButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiP2pChannel: WifiP2pManager.Channel
    private lateinit var wifiP2pManager: WifiP2pManager

    private lateinit var locationRequest: LocationRequest

    private var myself: Ingredient? = null
    private val ingredients = ArrayList<Ingredient>()

    private var collection = HashMap<Role, List<Ingredient>>()
    private lateinit var expandableListAdapter : IngredientAdapter
    private lateinit var expandableListView: ExpandableListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_content)


        collection = hashMapOf(Role.SURVIVOR to ingredients, Role.RESCUER to ingredients)
        expandableListView = findViewById(R.id.expandable_list)
        expandableListAdapter = IngredientAdapter(this, collection, listOf(Role.SURVIVOR, Role.RESCUER))
        expandableListView.setAdapter(expandableListAdapter)

        expandableListView.setOnChildClickListener { _, _, group, child, _ ->
            val selectedIngredient = expandableListAdapter.getChild(group, child)

            if(selectedIngredient != null){
                list.visibility = View.GONE
                description.visibility = View.VISIBLE

                username.text = "Nombre: ${selectedIngredient.username}"
                gps_distance.text = "Distancia GPS: ${selectedIngredient.distance}"
                role.text = if (selectedIngredient.role == Role.SURVIVOR.ordinal) "Rol: Sobreviviente" else ("Rol: Rescatista")
                updateCamera(selectedIngredient)
            }

            true
        }

        description.visibility = View.GONE

        //createLocationRequest()
        init()

        addLocalService()
        addServiceRequest()

        onReceivedLocation()

        locationUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.style_json
                )
            )
            if (!success) {
                Log.e("mapStyling", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("mapStyling", "Can't find style. Error: ", e)
        }

        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap.setMaxZoomPreference(100f)
        googleMap.setMinZoomPreference(5f)
        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.setOnMarkerClickListener { marker ->
            list.visibility = View.GONE
            description.visibility = View.VISIBLE

            val theIngredient = ingredients.firstOrNull { x -> x.marker == marker}
            if(theIngredient != null){
                username.text = "Nombre: ${theIngredient.username}"
                gps_distance.text = "Distancia GPS: ${theIngredient.distance}"
                role.text = if (theIngredient.role == Role.SURVIVOR.ordinal) "Rol: Sobreviviente" else ("Rol: Rescatista")
                //updateCamera(theIngredient)
            } else if (marker == myself!!.marker){
                username.text = "Nombre: ${myself!!.username} (yo)"
                role.text = if (myself!!.role == Role.SURVIVOR.ordinal) "Rol: Sobreviviente" else ("Rol: Rescatista")
                //updateCamera(myself!!)
            }
            false
            //TODO KEEP ZOOM ON UPDATECAMERA and delete info window
        }

        googleMap.setOnInfoWindowCloseListener {
            list.visibility = View.VISIBLE
            description.visibility = View.GONE
        }

        /*val file = File(applicationContext.filesDir, "bogota_tiles.mbtiles")
        val tileProvider: TileProvider =
            MapBoxOfflineTileProvider(file)
        googleMap
            .addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))*/
    }

    private fun init() {
        wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        wifiP2pChannel = wifiP2pManager.initialize(applicationContext, mainLooper, null)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        toggleAlarmIB = findViewById(R.id.toggle_alarm)

        toggleAlarmIB.setOnClickListener {
            if (toggleAlarm) {
                toggleOff()
            } else {
                toggleOn()
            }
        }

        change_role.setOnClickListener {
            if (myself != null) {
                if (myself!!.role == Role.SURVIVOR.ordinal) {
                    myself!!.role = Role.RESCUER.ordinal
                    change_role.text = "Cambiar a sobreviviente"
                } else {
                    myself!!.role = Role.SURVIVOR.ordinal
                    change_role.text = "Cambiar a rescatista"
                }
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
            latitude = location.latitude
            longitude = location.longitude
            marker = createMarker(this)
        }

        updateCamera(myself!!)
    }

    //TODO iniciar y detener el servicio dependiendo de la bandera. También detener el servicio
    // Cuando termina la actividad
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
                LocationManager.GPS_PROVIDER, 5000, 0f
            ) { location ->
                Log.d(
                    Constants.TAG_WIFI,
                    "Latitude: ${location.latitude}, longitude: ${location.longitude}"
                )
                if (myself == null) {
                    setupMyLocation(location)
                } else {
                    myself!!.latitude = location.latitude
                    myself!!.longitude = location.longitude
                    myself!!.marker = updateMarker(myself!!)
                }

                broadcastUpdate()
                discoverUpdates()
            }
        } catch (e: Exception) {
            Log.e("Sergio", e.toString())
        }
    }

    //TODO verificar si es necesario retornar el marker
    //TODO verificar parámetros
    private fun updateMarker(ingredient: Ingredient): Marker? {
        ingredient.marker?.position = LatLng(ingredient.latitude, ingredient.longitude)
        ingredient.marker?.snippet =
            "Distancia: ${String.format("%.3f", ingredient.distance)} metros"

        return ingredient.marker
    }

    //TODO verificar parámetros
    private fun createMarker(ingredient: Ingredient): Marker? {
        if (myself != null && ingredient == myself) {
            return googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(ingredient.latitude, ingredient.longitude))
                    .title("Yo")
                    .snippet(null)
                    .visible(true)
            )
        }

        return googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(ingredient.latitude, ingredient.longitude))
                .title(ingredient.username)
                .snippet("Distancia: ${String.format("%.3f", ingredient.distance)} metros")
                .icon(BitmapDescriptorFactory.defaultMarker((1..360).random().toFloat()))
                .visible(true)
        )
    }

    private fun updateCamera(ingredient: Ingredient) {
        val cameraUpdate: CameraUpdate =
            CameraUpdateFactory.newLatLngZoom(
                LatLng(ingredient.latitude, ingredient.longitude),
                15f
            )

        googleMap.moveCamera(cameraUpdate)
    }

    private fun broadcastUpdate() {
        wifiP2pManager.clearLocalServices(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                addLocalService()
            }

            override fun onFailure(arg0: Int) {
                Log.e(
                    Constants.TAG_WIFI,
                    "Command failed. clearLocalServices $arg0"
                )
            }
        })
    }

    private fun addLocalService() {
        val record = IngredientUtils.ingredientToHashMap(myself)

        val serviceInfo =
            WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record)

        wifiP2pManager.addLocalService(
            wifiP2pChannel,
            serviceInfo,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    discoverPeers()
                }

                override fun onFailure(arg0: Int) {
                    Log.e(
                        Constants.TAG_WIFI,
                        "Command failed.  addLocalService"
                    )
                }
            })
    }

    private fun discoverPeers() {
        wifiP2pManager.discoverPeers(
            wifiP2pChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {}

                override fun onFailure(error: Int) {
                    Log.e(Constants.TAG_WIFI, "DISCOVERING PEERS")
                }
            })
    }


    private fun discoverUpdates() {
        wifiP2pManager.clearServiceRequests(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                addServiceRequest()
            }

            override fun onFailure(code: Int) {
                Log.e(
                    Constants.TAG_WIFI,
                    "Command failed. clearServiceRequests $code"
                )
            }
        })
    }

    private fun addServiceRequest() {
        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance("_test", "_presence._tcp")

        wifiP2pManager.addServiceRequest(
            wifiP2pChannel,
            serviceRequest,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    discoverServices()
                }

                override fun onFailure(code: Int) {
                    Log.e(
                        Constants.TAG_WIFI,
                        "Command failed. addServiceRequest"
                    )
                }
            }
        )
    }

    private fun discoverServices() {
        wifiP2pManager.discoverServices(
            wifiP2pChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {}

                override fun onFailure(code: Int) {
                    Log.e(
                        Constants.TAG_WIFI,
                        "Command failed. discoverServices"
                    )
                }
            }
        )
    }


    //TODO ignorar si no se ha configurado myself (pensar en eso, myself es nullable)
    private fun onReceivedLocation() {
        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { _, record, device ->
            Log.d(
                Constants.TAG_WIFI,
                "Arriving message (second implementation) record: $record"
            )

            if (record.isEmpty()) return@DnsSdTxtRecordListener

            val ingredient = IngredientUtils.hashMapToIngredient(record, device, myself)

            val foundIngredient =
                ingredients.find { x -> x.deviceAddress == ingredient.deviceAddress }

            if (foundIngredient != null) {
                foundIngredient.longitude = ingredient.longitude
                foundIngredient.latitude = ingredient.latitude
                foundIngredient.distance = ingredient.distance
                foundIngredient.marker = updateMarker(foundIngredient)
                foundIngredient.role = ingredient.role
            } else {
                ingredient.marker = createMarker(ingredient)
                ingredients.add(ingredient)
            }


            val survivors = ingredients.filter { x -> x.role == Role.SURVIVOR.ordinal }
            val rescuers = ingredients.filter { x -> x.role == Role.RESCUER.ordinal }
            collection = hashMapOf(Role.SURVIVOR to survivors, Role.RESCUER to rescuers)
            expandableListAdapter.setData(collection)
        }

        wifiP2pManager.setDnsSdResponseListeners(wifiP2pChannel, null, txtListener)
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