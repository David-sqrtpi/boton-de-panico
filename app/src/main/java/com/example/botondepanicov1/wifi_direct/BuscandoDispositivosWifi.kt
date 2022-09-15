package com.example.botondepanicov1.wifi_direct

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.DataSetObserver
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.botondepanicov1.R
import com.example.botondepanicov1.util.Constants
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs

class BuscandoDispositivosWifi : AppCompatActivity() {
    private lateinit var wifiManager: WifiManager
    private var wifiP2pChannel: WifiP2pManager.Channel? = null
    private var wifiP2pManager: WifiP2pManager? = null
    private var serviceInfo: WifiP2pDnsSdServiceInfo? = null

    private lateinit var deviceName: String
    private var longitude = 0.0
    private var latitude = 0.0
    private var distance = 0.0

    private var record: HashMap<String, String> = HashMap()
    private var progressDialog: ProgressDialog? = null
    private var ingredients: ArrayList<Ingredient> = ArrayList()
    private var adapter: MapDevicesAdapter? = null
    private lateinit var lv: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscando_dispositivos_wifi)

        wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        deviceName =
            "Dispositivo: ${Build.MANUFACTURER.uppercase(Locale.ROOT)} ${Build.MODEL}"

        lv = findViewById<View>(R.id.FndListIdMap) as ListView

        enableWifi()
        start()
    }

    private fun enableWifi() {
        if (!wifiManager.isWifiEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                startActivityForResult(panelIntent, Constants.REQUEST_ACTION_INTERNET_CONNECTIVITY)
                //ToDo check if is it possible to close intent when the wifi is activated
            } else {
                wifiManager.isWifiEnabled = true
            }
        }
    }

    //WIFI Direct configuration
    @SuppressLint("MissingPermission")
    private fun start() {
        //TODO check location permission onStart
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        }

        turnGPSOn()

        val map = MapDevicesAdapter(this, R.layout.adapter_dispositivos_encontrados_wifi)
        adapter = map
        lv.adapter = adapter

        wifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        wifiP2pChannel = wifiP2pManager!!.initialize(applicationContext, mainLooper, null)

        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage("Buscando dispositivos por favor espere")
        progressDialog!!.setOnCancelListener { onBackPressed() }
        progressDialog!!.show()

        //Le dice que siempre esté seleccionado el último elemento, para eso está el let, para que
        //se ejecute siempre que haya elementos en la lista
        adapter?.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() {
                super.onChanged()
                adapter?.count?.minus(1)?.let { lv.setSelection(it) }
            }
        })

        //Establece la frecuencia de actualización del GPS y las coordenadas actuales
        setMap()

        val handler = Handler(mainLooper)

        handler.postDelayed(Runnable {
            record["longitude"] = longitude.toString()
            record["latitude"] = latitude.toString()
            record["index"] = deviceName
            try {
                record["date"] = Encoder.dateToString(Date())!!
            } catch (e: Exception) {
                println(e)
            }

            Log.d("MapDevices", "Record coordinates in the hashmap")

            serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                "_test", "_presence.tcp",
                record
            )

            //Incomprehensible
            if (ActivityCompat.checkSelfPermission(
                    this@BuscandoDispositivosWifi,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@Runnable
            }

            wifiP2pManager!!.addLocalService(
                wifiP2pChannel,
                serviceInfo,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        println("// Success!")
                    }

                    override fun onFailure(code: Int) {
                        println("// Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY")
                    }
                }
            )

            progressDialog!!.dismiss()

            wifiP2pManager!!.setDnsSdResponseListeners(
                wifiP2pChannel,
                { _, _, _ -> }
            ) { _, map, wifiP2pDevice ->
                val ingredient = Ingredient().apply {
                    deviceName = wifiP2pDevice.deviceName
                    deviceAddress = wifiP2pDevice.deviceAddress
                    longitude = java.lang.Double.valueOf(map["latitude"]!!)
                    latitude = java.lang.Double.valueOf(map["longitude"]!!)
                    date = map["date"]!!
                    distance = calculateDistance(longitude, latitude)
                }

                distance = ingredient.distance
                println(ingredient.toString())

                if (!isObjectInArray(wifiP2pDevice.deviceAddress)) {
                    ingredients.add(ingredient)

                    val arrayList: ArrayList<Ingredient> = ingredients

                    adapter!!.clear()
                    for (i in arrayList.indices) {
                        adapter!!.add(arrayList[i])
                    }

                    lv.setSelection(adapter!!.count - 1)

                    Log.d("Add device", java.lang.String.valueOf(adapter!!.count))
                    Log.d("Add device", ingredient.deviceName + " device")

                    when (ingredients.size) {
                        1 -> {
                            println(ingredients.size)
                            println(map["latitude"])
                            println(map["longitude"])
                        }
                        2 -> {
                            println(ingredients.size)
                            println(map["latitude"])
                            println(map["longitude"])
                        }
                        3 -> {
                            println(ingredients.size)
                            println(map["latitude"])
                            println(map["longitude"])
                        }
                        4 -> {
                            println(ingredients.size)
                            println(map["latitude"])
                            println(map["longitude"])
                        }
                        5 -> {
                            println(ingredients.size)
                            println(map["latitude"])
                            println(map["longitude"])
                        }
                        6 -> {
                            println(ingredients.size)
                            println(map["latitude"])
                            println(map["longitude"])
                        }
                        7 -> {
                            println(ingredients.size)
                            println(map["latitude"])
                            println(map["longitude"])
                        }
                        8 -> {
                            println(ingredients.size)
                            println(map["latitude"])
                            println(map["longitude"])
                        }
                        9 -> {
                            println(ingredients.size)
                            println(map["latitude"])
                            println(map["longitude"])
                        }
                        10 -> {
                            println(ingredients.size)
                            println(map["latitude"])
                            println(map["longitude"])
                        }
                    }
                }
            }

            val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()

            wifiP2pManager!!.addServiceRequest(
                wifiP2pChannel,
                serviceRequest,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        println("// Success! addServiceRequest")
                    }

                    override fun onFailure(code: Int) {
                        println("// Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY addServiceRequest")
                    }
                })

            wifiP2pManager!!.discoverServices(
                wifiP2pChannel,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        println("// Success! discoverServices")
                    }

                    override fun onFailure(code: Int) {
                        println("// Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY discoverServices")
                    }
                })
            Log.d("MapDevices", "Start services")
        }, 10000)

    }

    private fun isObjectInArray(deviceAddress: String): Boolean {
        val result = false
        for (ingredient in ingredients) {
            if (ingredient.deviceAddress == deviceAddress) {
                return true
            }
        }
        return result
    }

    private fun calculateDistance(otherLongitude: Double, otherLatitude: Double): Double {
        val loc1 = Location("")
        loc1.latitude = otherLatitude
        loc1.longitude = otherLongitude
        val loc2 = Location("")
        loc2.latitude = latitude
        loc2.longitude = longitude

        return loc1.distanceTo(loc2).toDouble()
    }

    //Se va a reemplazar por el Google para dispositivos que admitan los servicio de Google
    private fun turnGPSOn() {
        val provider =
            Settings.Secure.getString(contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
        if (!provider.contains("gps")) {
            Toast.makeText(this, "Activando GPS", Toast.LENGTH_SHORT).show()
            val intent1 = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent1)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setMap() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager

        try {
            lm.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 100, 5f, locationListenerGPS
            )
        } catch (e: Exception) {
            Log.v("Sergio", e.toString())
        }

    }

    private val locationListenerGPS: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            println("La ubicación ha cambiado chaval")
            longitude = location.longitude
            latitude = location.latitude
            println("longitude: $longitude")
            println("latitude: $latitude")
        }

        override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
        override fun onProviderEnabled(s: String) {}
        override fun onProviderDisabled(s: String) {}
    }
}
