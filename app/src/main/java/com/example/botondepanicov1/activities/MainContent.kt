package com.example.botondepanicov1.activities

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.R
import com.example.botondepanicov1.adapters.IngredientAdapter
import com.example.botondepanicov1.databinding.ActivityMainContentBinding
import com.example.botondepanicov1.models.Ingredient
import com.example.botondepanicov1.models.Role
import com.example.botondepanicov1.services.AlarmService
import com.example.botondepanicov1.util.Constants
import com.example.botondepanicov1.util.Constants.Companion.TAG_BT
import com.example.botondepanicov1.util.Constants.Companion.TAG_LOCATION
import com.example.botondepanicov1.util.Constants.Companion.TAG_MAPS
import com.example.botondepanicov1.util.Constants.Companion.TAG_WIFI
import com.example.botondepanicov1.util.GPSUtils
import com.example.botondepanicov1.util.WiFiFrameUtils
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import org.altbeacon.beacon.*
import java.util.*

//TODO update description when received location or changed location
class MainContent : AppCompatActivity(), OnMapReadyCallback, RangeNotifier, MonitorNotifier,
    LocationListener {
    private var toggleAlarm = false //TODO averiguar si se puede simplificar
    private lateinit var googleMap: GoogleMap
    private var collection = HashMap<Role, List<Ingredient>>()
    private lateinit var expandableListAdapter: IngredientAdapter
    private val ingredients = ArrayList<Ingredient>()
    private var myself: Ingredient = Ingredient()
    private var selectedIngredient: Ingredient? = null

    //WiFi Direct variables
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiP2pChannel: WifiP2pManager.Channel
    private lateinit var wifiP2pManager: WifiP2pManager

    //Bluetooth variables
    private lateinit var beacon: Beacon
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var beaconManager: BeaconManager
    private lateinit var beaconTransmitter: BeaconTransmitter
    private var beaconParser = BeaconParser()
        .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")

    private lateinit var binding: ActivityMainContentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainContentBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.i(TAG_BT, "BLE is not supported")
            // TODO enviar paquete con ble no disponible
        } else {
            val btManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = btManager.adapter

            if (!bluetoothAdapter.isMultipleAdvertisementSupported)
                Log.i(TAG_BT, "Multiple advertisement is not supported")
            // TODO enviar paquete con ble no disponible

            if (bluetoothAdapter.isEnabled) {
                beaconManager = BeaconManager.getInstanceForApplication(this)
                setupBeacon()
                transmitIBeacon()
                beaconManager.beaconParsers.add(beaconParser)
                onBeaconServiceConnect()
            } else {
                Log.i(TAG_BT, "Bluetooth is off")
                Toast.makeText(
                    this, "Habilite bluetooth antes de transmitir iBeacon.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        init()
        addLocalService()
        addServiceRequest()
        onReceivedLocation()
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()

        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            lm.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 0f, this
            )
        } catch (e: Exception) {
            Log.e(TAG_LOCATION, e.toString())
        }
    }

    @SuppressLint("MissingPermission")
    override fun onPause() {
        super.onPause()

        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        lm.removeUpdates(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.style_json
                )
            )
            if (!success) Log.e(TAG_MAPS, "Style parsing failed.")
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG_MAPS, "Can't find style. Error: ", e)
        }

        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap.uiSettings.isMapToolbarEnabled = false

        googleMap.setOnMarkerClickListener { marker ->
            selectedIngredient = ingredients.firstOrNull { x -> x.marker == marker } ?: myself
            showDescription()
            updateDescription(selectedIngredient)
            false
        }

        //TODO create button to go back in sheet and do this
        googleMap.setOnInfoWindowCloseListener {
            showList()
        }
    }

    private fun init() {
        myself.wiFiFrame = WiFiFrameUtils.buildMyWiFiFrame(this)

        collection = hashMapOf(Role.SURVIVOR to ingredients, Role.RESCUER to ingredients)
        expandableListAdapter =
            IngredientAdapter(this, collection, listOf(Role.SURVIVOR, Role.RESCUER))
        binding.expandableList.setAdapter(expandableListAdapter)

        showList()

        binding.expandableList.setOnChildClickListener { _, _, group, child, _ ->
            selectedIngredient = expandableListAdapter.getChild(group, child)
            showDescription()
            updateDescription(selectedIngredient)
            true
        }

        wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        wifiP2pChannel = wifiP2pManager.initialize(applicationContext, mainLooper, null)

        //TODO Detener el servicio cuando termina la actividad
        binding.toggleAlarm.setOnClickListener {
            val intent = Intent(this, AlarmService::class.java)

            if (toggleAlarm) {
                binding.toggleAlarm.setImageResource(R.drawable.alarm_off)
                stopService(intent)
            } else {
                binding.toggleAlarm.setImageResource(R.drawable.alarm)
                startService(intent)
            }
            toggleAlarm = !toggleAlarm
        }

        binding.close.setOnClickListener {
            showList()
        }

        binding.changeRole.setOnClickListener {
            if (myself.wiFiFrame.role == Role.SURVIVOR.ordinal) {
                myself.wiFiFrame.role = Role.RESCUER.ordinal
                binding.changeRole.text = "Cambiar a sobreviviente"
            } else {
                myself.wiFiFrame.role = Role.SURVIVOR.ordinal
                binding.changeRole.text = "Cambiar a rescatista"
            }
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun addLocalService() {
        val record = WiFiFrameUtils.wifiFrameToHashMap(myself.wiFiFrame)

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
                    Log.e(TAG_WIFI, "Add local service has failed. $arg0")
                }
            })
    }

    private fun addServiceRequest() {
        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance(
            "_test",
            "_presence._tcp"
        )

        wifiP2pManager.addServiceRequest(
            wifiP2pChannel,
            serviceRequest,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    discoverServices()
                }

                override fun onFailure(code: Int) {
                    Log.e(TAG_WIFI, "Add service request has failed. $code")
                }
            }
        )
    }

    private fun onReceivedLocation() {
        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { _, record, _ ->
            Log.d(TAG_WIFI, "Record: $record")

            if (record.isEmpty()) return@DnsSdTxtRecordListener

            val wiFiFrame = WiFiFrameUtils.hashMapToWiFiFrame(record)

            val foundIngredient =
                ingredients.firstOrNull { x -> x.wiFiFrame.uuid == wiFiFrame.uuid }

            //TODO add bt update date and bt frame
            if (foundIngredient != null) {
                foundIngredient.wiFiFrame = wiFiFrame
                foundIngredient.gpsDistance =
                    GPSUtils.calculateDistance(myself.wiFiFrame, wiFiFrame)
                foundIngredient.marker = updateMarker(foundIngredient)

            } else {
                ingredients.add(Ingredient().apply {
                    this.wiFiFrame = wiFiFrame
                    gpsDistance = GPSUtils.calculateDistance(myself.wiFiFrame, wiFiFrame)
                    marker = createMarker(this)
                })
            }

            if (binding.list.visibility == View.VISIBLE) {
                val survivors =
                    ingredients.filter { x -> x.wiFiFrame.role == Role.SURVIVOR.ordinal }
                val rescuers = ingredients.filter { x -> x.wiFiFrame.role == Role.RESCUER.ordinal }
                collection = hashMapOf(Role.SURVIVOR to survivors, Role.RESCUER to rescuers)
                expandableListAdapter.setData(collection)
            } else if (binding.description.visibility == View.VISIBLE) {
                updateDescription(selectedIngredient)
            }
        }

        wifiP2pManager.setDnsSdResponseListeners(wifiP2pChannel, null, txtListener)
    }

    //TODO verificar cuando es el marcador propio y sobreviviente/rescatista
    private fun createMarker(ingredient: Ingredient): Marker? {
        if(ingredient == myself){
            return googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(ingredient.wiFiFrame.latitude, ingredient.wiFiFrame.longitude))
                    .flat(true)
                    .zIndex(10f)
                    .visible(true)
            )
        } else {
            if(ingredient.wiFiFrame.role == Role.SURVIVOR.ordinal){
                val b = BitmapFactory.decodeResource(resources, R.drawable.sos_icon)
                val smallMarker = Bitmap.createScaledBitmap(b, 75, 75, false)

                return googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(ingredient.wiFiFrame.latitude, ingredient.wiFiFrame.longitude))
                        .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                        .flat(true)
                        .visible(true)
                )
            } else {
                val b = BitmapFactory.decodeResource(resources, R.drawable.red_cross)
                val smallMarker = Bitmap.createScaledBitmap(b, 75, 75, false)

                return googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(ingredient.wiFiFrame.latitude, ingredient.wiFiFrame.longitude))
                        .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                        .flat(true)
                        .visible(true)
                )
            }
        }
    }

    //TODO verificar parámetros
    private fun updateMarker(ingredient: Ingredient): Marker? {
        if(ingredient != myself){
            val b = if(ingredient.wiFiFrame.role == Role.SURVIVOR.ordinal){
                BitmapFactory.decodeResource(resources, R.drawable.sos_icon)
            } else {
                BitmapFactory.decodeResource(resources, R.drawable.red_cross)
            }

            val smallMarker = Bitmap.createScaledBitmap(b, 75, 75, false)
            ingredient.marker?.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker))
        }

        ingredient.marker?.position =
            LatLng(ingredient.wiFiFrame.latitude, ingredient.wiFiFrame.longitude)

        return ingredient.marker
    }

    private fun updateCamera(ingredient: Ingredient) {
        val cameraUpdate: CameraUpdate =
            CameraUpdateFactory.newLatLngZoom(
                LatLng(ingredient.wiFiFrame.latitude, ingredient.wiFiFrame.longitude),
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
                Log.e(TAG_WIFI, "Clear local services has failed. $arg0")
            }
        })
    }

    private fun discoverUpdates() {
        wifiP2pManager.clearServiceRequests(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                addServiceRequest()
            }

            override fun onFailure(code: Int) {
                Log.e(TAG_WIFI, "Clear service requests has failed. $code")
            }
        })
    }

    private fun discoverPeers() {
        wifiP2pManager.discoverPeers(
            wifiP2pChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {}

                override fun onFailure(error: Int) {
                    Log.e(TAG_WIFI, "Discover peers has failed. $error")
                }
            })
    }

    private fun discoverServices() {
        wifiP2pManager.discoverServices(
            wifiP2pChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {}

                override fun onFailure(code: Int) {
                    Log.e(TAG_WIFI, "Discover services has failed. $code")
                }
            }
        )
    }

    private fun showList() {
        binding.list.visibility = View.VISIBLE
        binding.description.visibility = View.GONE
    }

    private fun showDescription() {
        binding.list.visibility = View.GONE
        binding.description.visibility = View.VISIBLE
    }

    //TODO derivated values like gpsdistance and height could be removed from Ingredient
    private fun updateDescription(ingredient: Ingredient?) {
        if (ingredient != null) {
            binding.device.text = ingredient.wiFiFrame.deviceName
            binding.role.text =
                if (ingredient.wiFiFrame.role == Role.SURVIVOR.ordinal) "Sobreviviente" else "Rescatista"

            if (ingredient == myself) {
                descriptionVisibility(View.GONE)
                binding.username.text = "${ingredient.wiFiFrame.username} (yo)"
            } else {
                descriptionVisibility(View.VISIBLE)
                binding.username.text = ingredient.wiFiFrame.username
                binding.gpsDistance.text = "GPS: ${ingredient.gpsDistance} metros"
                binding.gpsCaption.text = "Actualización: ${ingredient.wiFiFrame.date}"
                binding.btDistance.text = "Absoluta: ${ingredient.bluetoothFrame.distance} metros"
                binding.height.text = "Altura: ${ingredient.height} metros"
                binding.btCaption.text = "Actualización: ${ingredient.btUpdateDate}"
            }
        }
    }

    private fun descriptionVisibility(visibility: Int) {
        binding.distances.visibility = visibility
        binding.gpsDistance.visibility = visibility
        binding.gpsCaption.visibility = visibility
        binding.btDistance.visibility = visibility
        binding.height.visibility = visibility
        binding.btCaption.visibility = visibility
    }


    private fun transmitIBeacon() {
        if (beaconTransmitter.isStarted) {
            beaconTransmitter.stopAdvertising()
        } else {
            beaconTransmitter.startAdvertising(beacon, object : AdvertiseCallback() {
                override fun onStartFailure(errorCode: Int) {
                    Log.e(TAG_BT, "Advertisement start failed with code: $errorCode")
                }

                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    Log.i(TAG_BT, "Advertisement start succeeded. $settingsInEffect")
                }
            })
        }
    }

    //initialize beacon and beaconTransmitter
    private fun setupBeacon() {
        val uuid = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
            .getString(Constants.PREFERENCES_UUID, "TODO").toString()

        Log.v(TAG_BT, "UUID: $uuid")
        beacon = Beacon.Builder()
            .setId1(uuid) // UUID for beacon
            .setId2("5") // Major for beacon
            .setId3("12") // Minor for beacon
            .setManufacturer(0x004C) // Radius Networks.0x0118  Change this for other beacon layouts//0x004C for iPhone
            .setTxPower(-56) // Power in dB
            .setDataFields(
                listOf(
                    2L,
                    3L
                )
            ) // Remove this for beacon layouts without d: fields
            .build()

        beaconTransmitter = BeaconTransmitter(this, beaconParser)
    }

    private fun onBeaconServiceConnect() {
        val region = Region("panic-button-region", null, null, null)

        beaconManager.addMonitorNotifier(this)
        beaconManager.addRangeNotifier(this)
        beaconManager.startMonitoring(region)
        beaconManager.startRangingBeacons(region)
    }

    override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>?, region: Region?) {
        if (beacons != null) {
            Log.d(TAG_BT, "BEACONS")
            for (oneBeacon in beacons) {
                Log.d(TAG_BT, oneBeacon.toString())

                val theIngredient = ingredients.firstOrNull { x ->
                    x.wiFiFrame.uuid == oneBeacon.id1.toString()
                }

                if (theIngredient != null) {
                    theIngredient.bluetoothFrame.distance = oneBeacon.distance
                    theIngredient.bluetoothFrame.identifier = oneBeacon.id1
                    theIngredient.btUpdateDate = Date()

                    if (binding.description.visibility == View.VISIBLE) {
                        updateDescription(selectedIngredient)
                    }
                }
            }
        }
    }

    override fun didEnterRegion(region: Region?) {}

    override fun didExitRegion(region: Region?) {}

    override fun didDetermineStateForRegion(state: Int, region: Region?) {}

    override fun onLocationChanged(location: Location) {
        Log.d(
            TAG_LOCATION,
            "Latitude: ${location.latitude},longitude: ${location.longitude}"
        )

        myself.wiFiFrame.latitude = location.latitude
        myself.wiFiFrame.longitude = location.longitude

        if (myself.marker == null) {
            myself.marker = createMarker(myself)
            updateCamera(myself)
        } else {
            myself.marker = updateMarker(myself)
        }

        broadcastUpdate()
        discoverUpdates()
    }
}