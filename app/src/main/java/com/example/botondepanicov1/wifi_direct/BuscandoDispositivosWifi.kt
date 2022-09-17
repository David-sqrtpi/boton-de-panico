package com.example.botondepanicov1.wifi_direct

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.database.DataSetObserver
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.R
import com.example.botondepanicov1.util.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*

class BuscandoDispositivosWifi : AppCompatActivity() {
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiP2pChannel: WifiP2pManager.Channel
    private lateinit var wifiP2pManager: WifiP2pManager

    private lateinit var deviceName: String
    private var longitude = 0.0
    private var latitude = 0.0
    private var distance = 0.0

    private var progressDialog: ProgressDialog? = null
    private var ingredients: ArrayList<Ingredient> = ArrayList()
    private var adapter: MapDevicesAdapter? = null
    private lateinit var lv: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscando_dispositivos_wifi)

        wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        deviceName = "${Build.MANUFACTURER.uppercase(Locale.ROOT)} ${Build.MODEL}"

        lv = findViewById<View>(R.id.FndListIdMap) as ListView

        wifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        wifiP2pChannel = wifiP2pManager!!.initialize(applicationContext, mainLooper, null)

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
        turnGPSOn()

        val map = MapDevicesAdapter(this, R.layout.adapter_dispositivos_encontrados_wifi)
        adapter = map
        lv.adapter = adapter

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
        locationUpdates()
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
    private fun locationUpdates() {
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
            longitude = location.longitude
            latitude = location.latitude
            println("La ubicación ha cambiado chaval")
            println("longitude: $longitude")
            println("latitude: $latitude")
        }

        override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
        override fun onProviderEnabled(s: String) {}
        override fun onProviderDisabled(s: String) {}
    }

    private fun startRegistration() {
        val record: HashMap<String, String> = HashMap()

        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
        val username = sharedPreferences.getString(Constants.PREFERENCES_USERNAME, "Usuario")
            .toString()

        record["longitude"] = longitude.toString()
        record["latitude"] = latitude.toString()
        record["index"] = deviceName
        record["date"] = Encoder.dateToString(Date())!!
        record["username"] = username

        val serviceInfo =
            WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record)
        wifiP2pManager.addLocalService(
            wifiP2pChannel,
            serviceInfo,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("addLocalService", "Success!")
                }

                override fun onFailure(arg0: Int) {
                    Log.d(
                        "addLocalService",
                        "Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY"
                    )
                }
            })
    }

    private fun discoverService() {
        val servListener =
            WifiP2pManager.DnsSdServiceResponseListener { instanceName, registrationType, resourceType ->
                Log.d(
                    "TAG",
                    "instanceName: $instanceName, registrationType: $registrationType, resourceType: $resourceType"
                )
            }

        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { _, record, device ->
            val ingredient = Ingredient().apply {
                deviceName = record["index"]!!
                deviceAddress = device.deviceAddress
                longitude = java.lang.Double.valueOf(record["latitude"]!!)
                latitude = java.lang.Double.valueOf(record["longitude"]!!)
                date = record["date"]!!
                distance = calculateDistance(longitude, latitude)
            }

            distance = ingredient.distance

            if (!isObjectInArray(device.deviceAddress)) {
                ingredients.add(ingredient)

                val arrayList: ArrayList<Ingredient> = ingredients

                adapter!!.clear()
                for (i in arrayList.indices) {
                    adapter!!.add(arrayList[i])
                }

                lv.setSelection(adapter!!.count - 1)

                Log.d("Add device", java.lang.String.valueOf(adapter!!.count))
                Log.d("Add device", ingredient.deviceName + " device")
            }
            Log.d("ingredient", ingredient.toString())
        }

        wifiP2pManager.setDnsSdResponseListeners(wifiP2pChannel, servListener, txtListener)

        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance("_test", "_presence.tcp")

        wifiP2pManager.addServiceRequest(
            wifiP2pChannel,
            serviceRequest,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("addServiceRequest", "Success!")
                }

                override fun onFailure(code: Int) {
                    Log.d(
                        "addServiceRequest",
                        "Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY"
                    )
                }
            }
        )

        wifiP2pManager.discoverServices(
            wifiP2pChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("discoverServices", "Success!")
                }

                override fun onFailure(code: Int) {
                    Log.d(
                        "disoverServices",
                        "Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY"
                    )
                }
            }
        )

        Log.d("MapDevices", "Start services")
    }
}
