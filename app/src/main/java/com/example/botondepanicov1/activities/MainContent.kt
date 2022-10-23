package com.example.botondepanicov1.activities

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
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
import com.example.botondepanicov1.models.BluetoothFrame
import com.example.botondepanicov1.models.Ingredient
import com.example.botondepanicov1.models.Role
import com.example.botondepanicov1.models.WiFiFrame
import com.example.botondepanicov1.services.AlarmService
import com.example.botondepanicov1.util.Constants
import com.example.botondepanicov1.util.Constants.Companion.TAG_LOCATION
import com.example.botondepanicov1.util.Constants.Companion.TAG_MAPS
import com.example.botondepanicov1.util.Constants.Companion.TAG_WIFI
import com.example.botondepanicov1.util.Encoder
import com.example.botondepanicov1.util.GPSUtils
import com.example.botondepanicov1.util.WiFiFrameUtils
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_main_content.*
import org.altbeacon.beacon.*
import java.util.*

//TODO update description when received location or changed location
class MainContent : AppCompatActivity(), OnMapReadyCallback, RangeNotifier, MonitorNotifier {
    private var toggleAlarm = false //TODO averiguar si se puede simplificar
    private lateinit var googleMap: GoogleMap
    private var collection = HashMap<Role, List<Ingredient>>()
    private lateinit var expandableListAdapter: IngredientAdapter
    private val ingredients = ArrayList<Ingredient>()
    private var myself: Ingredient = Ingredient()

    //WiFi Direct variables
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiP2pChannel: WifiP2pManager.Channel
    private lateinit var wifiP2pManager: WifiP2pManager

    //Bluetooth variables
    private lateinit var beacon: Beacon
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var beaconManager: BeaconManager
    private lateinit var beaconTransmitter: BeaconTransmitter
    private var bluetoothList: MutableList<BluetoothFrame> = java.util.ArrayList()
    private var beaconParser = BeaconParser()
        .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_content)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.i(Constants.TAG_BT, "BLE is not supported")
            // TODO enviar paquete con ble no disponible
        } else {
            val btManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = btManager.adapter

            if (!bluetoothAdapter.isMultipleAdvertisementSupported)
                Log.i(Constants.TAG_BT, "Multiple advertisement is not supported")
            // TODO enviar paquete con ble no disponible

            if (bluetoothAdapter.isEnabled) {
                beaconManager = BeaconManager.getInstanceForApplication(this)
                setupBeacon()
                transmitIBeacon()
                beaconManager.beaconParsers.add(beaconParser)
                onBeaconServiceConnect()
            } else {
                Log.i(Constants.TAG_BT, "Bluetooth is off")
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
            if (!success) Log.e(TAG_MAPS, "Style parsing failed.")
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG_MAPS, "Can't find style. Error: ", e)
        }

        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap.uiSettings.isMapToolbarEnabled = false

        googleMap.setOnMarkerClickListener { marker ->
            val selectedIngredient = ingredients.firstOrNull { x -> x.marker == marker }

            if (selectedIngredient != null) showDescription(selectedIngredient)
            else showDescription(myself)

            false
            //TODO KEEP ZOOM ON UPDATECAMERA and delete info window
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
        expandable_list.setAdapter(expandableListAdapter)

        showList()

        expandable_list.setOnChildClickListener { _, _, group, child, _ ->
            val selectedIngredient = expandableListAdapter.getChild(group, child)
            if (selectedIngredient != null) showDescription(selectedIngredient)
            true
        }

        wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        wifiP2pChannel = wifiP2pManager.initialize(applicationContext, mainLooper, null)

        //TODO Detener el servicio cuando termina la actividad
        toggle_alarm.setOnClickListener {
            val intent = Intent(this, AlarmService::class.java)

            if (toggleAlarm) {
                toggle_alarm.setImageResource(R.drawable.alarm_off)
                stopService(intent)
            } else {
                toggle_alarm.setImageResource(R.drawable.alarm)
                startService(intent)
            }
            toggleAlarm = !toggleAlarm
        }

        change_role.setOnClickListener {
            if (myself.wiFiFrame.role == Role.SURVIVOR.ordinal) {
                myself.wiFiFrame.role = Role.RESCUER.ordinal
                change_role.text = "Cambiar a sobreviviente"
            } else {
                myself.wiFiFrame.role = Role.SURVIVOR.ordinal
                change_role.text = "Cambiar a rescatista"
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
                    marker = createMarker(wiFiFrame)
                })
            }

            val survivors = ingredients.filter { x -> x.wiFiFrame.role == Role.SURVIVOR.ordinal }
            val rescuers = ingredients.filter { x -> x.wiFiFrame.role == Role.RESCUER.ordinal }
            collection = hashMapOf(Role.SURVIVOR to survivors, Role.RESCUER to rescuers)
            expandableListAdapter.setData(collection)
        }

        wifiP2pManager.setDnsSdResponseListeners(wifiP2pChannel, null, txtListener)
    }

    @SuppressLint("MissingPermission")
    private fun locationUpdates() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager

        try {
            lm.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 0f
            ) { location ->
                Log.d(
                    TAG_LOCATION,
                    "Latitude: ${location.latitude},longitude: ${location.longitude}"
                )

                myself.wiFiFrame.latitude = location.latitude
                myself.wiFiFrame.longitude = location.longitude

                if (myself.marker == null) {
                    myself.marker = createMarker(myself.wiFiFrame)
                    updateCamera(myself)
                } else {
                    myself.marker = updateMarker(myself)
                }

                broadcastUpdate()
                discoverUpdates()
            }
        } catch (e: Exception) {
            Log.e(TAG_LOCATION, e.toString())
        }
    }

    //TODO verificar cuando es el marcador propio y sobreviviente/rescatista
    private fun createMarker(wifiFrame: WiFiFrame): Marker? {
        return googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(wifiFrame.latitude, wifiFrame.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker((1..360).random().toFloat()))
                .visible(true)
        )
    }

    //TODO verificar parámetros
    private fun updateMarker(ingredient: Ingredient): Marker? {
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
        list.visibility = View.VISIBLE
        description.visibility = View.GONE
    }

    private fun showDescription(ingredient: Ingredient) {
        list.visibility = View.GONE
        description.visibility = View.VISIBLE

        device.text = ingredient.wiFiFrame.deviceName
        role.text =
            if (ingredient.wiFiFrame.role == Role.SURVIVOR.ordinal) "Sobreviviente" else "Rescatista"

        if (ingredient == myself) {
            descriptionVisibility(View.GONE)
            username.text = "${ingredient.wiFiFrame.username} (yo)"
        } else {
            descriptionVisibility(View.VISIBLE)
            username.text = ingredient.wiFiFrame.username
            gps_distance.text = "GPS: ${ingredient.gpsDistance} metros"
            gps_caption.text = "Actualización: ${ingredient.wiFiFrame.date}"
        }
    }

    private fun descriptionVisibility(visibility: Int) {
        distances.visibility = visibility
        gps_distance.visibility = visibility
        gps_caption.visibility = visibility
        bt_distance.visibility = visibility
        height.visibility = visibility
        bt_caption.visibility = visibility
    }


    private fun transmitIBeacon() {
        if (beaconTransmitter.isStarted) {
            beaconTransmitter.stopAdvertising()
        } else {
            beaconTransmitter.startAdvertising(beacon, object : AdvertiseCallback() {
                override fun onStartFailure(errorCode: Int) {
                    Log.e(Constants.TAG_BT, "Advertisement start failed with code: $errorCode")
                }

                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    Log.i(Constants.TAG_BT, "Advertisement start succeeded. $settingsInEffect")
                }
            })
        }
    }

    //initialize beacon and beaconTransmitter
    private fun setupBeacon() {
        val uuid = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
            .getString(Constants.PREFERENCES_UUID, "TODO").toString()

        Log.v(Constants.TAG_BT, "UUID: $uuid")
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

    //TODO verificar para integrar con ingredient en wifi
    private fun deleteDuplicated(
        list: MutableList<BluetoothFrame>,
        oneBeacon: Beacon
    ): MutableList<BluetoothFrame> {
        val strDate = Encoder.dateToString(Date())
        bluetoothList.removeAll { x -> x.identifier == oneBeacon.id1 }

        val frame = BluetoothFrame().apply {
            identifier = oneBeacon.id1
            distance = oneBeacon.distance
            //date = strDate
        }

        bluetoothList.add(frame)
        return list
    }

    override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>?, region: Region?) {
        if (beacons != null) {
            for (oneBeacon in beacons) {
                Log.d(
                    Constants.TAG_BT,
                    "distance: " + oneBeacon.distance + " address:" + oneBeacon.bluetoothAddress
                            + " id:" + oneBeacon.id1 + "/" + oneBeacon.id2 + "/" + oneBeacon.id3
                )

                bluetoothList = deleteDuplicated(bluetoothList, oneBeacon)
            }
        }
    }

    override fun didEnterRegion(region: Region?) {}

    override fun didExitRegion(region: Region?) {}

    override fun didDetermineStateForRegion(state: Int, region: Region?) {}
}