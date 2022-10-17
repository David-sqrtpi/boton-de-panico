package com.example.botondepanicov1.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.preference.PreferenceManager
import android.util.Log
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.botondepanicov1.R
import com.example.botondepanicov1.adapters.IngredientAdapter
import com.example.botondepanicov1.core.Role
import com.example.botondepanicov1.models.BluetoothFrame
import com.example.botondepanicov1.wifi_direct.Ingredient
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import org.altbeacon.beacon.*
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.and

class BuscandoDispositivosBluetooth : AppCompatActivity(), OnMapReadyCallback, BeaconConsumer {
    // variables para la configuracion de los beacons
    private var beacon: Beacon? = null
    private var btAdapter: BluetoothAdapter? = null
    private var beaconManager: BeaconManager? = null
    private var beaconTransmitter: BeaconTransmitter? = null
    private var listaBluetooth: ListView? = null
    private var mLista: MutableList<BluetoothFrame> = ArrayList()
    private var mAdapter: AdapterBluetooth? = null
    private lateinit var googleMap: GoogleMap

    //Direccion MAC
    var mac: String? = null
    var KEY_MAC = "MAC"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscando_dispositivos_bluetooth)

        val childList = listOf(
            Ingredient().apply {
                username = "Homero Sompson"
                distance = 123.1
                role = Role.SURVIVOR.ordinal
            },
            Ingredient().apply {
                username = "Lisa Sompson"
                distance = 123.1
                role = Role.RESCUER.ordinal
            },
            Ingredient().apply {
                username = "Bort Sompson"
                distance = 123.1
                role = Role.SURVIVOR.ordinal
            },
            Ingredient().apply {
                username = "Marge Sompson"
                distance = 123.1
                role = Role.RESCUER.ordinal
            },
            Ingredient().apply {
                username = "Abraham Sompson"
                distance = 123.1
                role = Role.SURVIVOR.ordinal
            },
            Ingredient().apply {
                username = "The Game Sompson"
                distance = 123.1
                role = Role.SURVIVOR.ordinal
            },
            Ingredient().apply {
                username = "Homero Sompson"
                distance = 123.1
                role = Role.SURVIVOR.ordinal
            },
            Ingredient().apply {
                username = "Lisa Sompson"
                distance = 123.1
                role = Role.RESCUER.ordinal
            },
            Ingredient().apply {
                username = "Bort Sompson"
                distance = 123.1
                role = Role.SURVIVOR.ordinal
            },
            Ingredient().apply {
                username = "Marge Sompson"
                distance = 123.1
                role = Role.RESCUER.ordinal
            },
            Ingredient().apply {
                username = "Abraham Sompson"
                distance = 123.1
                role = Role.SURVIVOR.ordinal
            },
            Ingredient().apply {
                username = "The Game Sompson"
                distance = 123.1
                role = Role.SURVIVOR.ordinal
            }, Ingredient().apply {
                username = "Homero Sompson"
                distance = 123.1
                role = Role.SURVIVOR.ordinal
            },
            Ingredient().apply {
                username = "Lisa Sompson"
                distance = 123.1
                role = Role.RESCUER.ordinal
            },
            Ingredient().apply {
                username = "Bort Sompson"
                distance = 123.1
                role = Role.SURVIVOR.ordinal
            },
            Ingredient().apply {
                username = "Marge Sompson"
                distance = 123.1
                role = Role.RESCUER.ordinal
            },
            Ingredient().apply {
                username = "Abraham Sompson"
                distance = 123.1
                role = Role.SURVIVOR.ordinal
            },
            Ingredient().apply {
                username = "The Game Sompson"
                distance = 123.1
                role = Role.SURVIVOR.ordinal
            }
        )
        val survivors = childList.filter { x -> x.role == Role.SURVIVOR.ordinal }
        val rescuers = childList.filter { x -> x.role == Role.RESCUER.ordinal }
        val collection = hashMapOf(Role.SURVIVOR to survivors, Role.RESCUER to rescuers)

        val expandableListView = findViewById<ExpandableListView>(R.id.expandable_list)
        val expandableListAdapter: ExpandableListAdapter = IngredientAdapter(this, collection, listOf(Role.SURVIVOR, Role.RESCUER))
        expandableListView.setAdapter(expandableListAdapter)

        expandableListView.setOnChildClickListener { _, _, group, child, _ ->
            val selected = expandableListAdapter.getChild(group, child)
            Toast.makeText(this, selected.toString(), Toast.LENGTH_LONG).show()
            true
        }

        locationUpdates()

        //LEER PREFERENCIAS DE LA MAC
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val datos = pref.getString(KEY_MAC, "No hay datos")
        Log.v("Sergio", "MACKEY$datos")
        // valida que se pueda optener la MAC, si no es posible genera el numero aleatorio
        if (datos == "No hay datos") {
            mac = trasformarMac()
            guardarMacAleatoria(mac)
        }
        run { mac = datos }

        //llama a funcion de validar permisos de localozacion
        checkPermission()
        //TODO listaBluetooth = findViewById(R.id.listBluetooth)
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        beaconManager = BeaconManager.getInstanceForApplication(this)
        encenderBluetooth()
        //llama la funcion para la configuracion del beacon
        setupBeacon()
        //llama la funcion para envio de los beacons
        envio()
        beaconManager!!.beaconParsers.add(
            BeaconParser()
                .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")
        )
        inicioDescubrimiento()
    }

    //si no es posible obtener la MAC guarda la aleatoria en preferencias
    fun guardarMacAleatoria(mac: String?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs.edit()
        editor.putString(KEY_MAC, mac)
        editor.apply()
    }

    //termina la actividad cuando se hace uso de boton atras
    override fun onBackPressed() {
        beaconManager!!.unbind(this@BuscandoDispositivosBluetooth)
        finish()
    }

    //encender o apagar bluetooth
    private fun encenderBluetooth() {
        if (!btAdapter!!.isEnabled) {
            btAdapter!!.enable()
        }
    }

    // inica el descubrimiento de dispositivos
    private fun inicioDescubrimiento() {
        beaconManager!!.bind(this@BuscandoDispositivosBluetooth)
    }

    //valida que el bluetooth habilitado y compatible con el dispositivo
    private val isBluetoothLEAvailable: Boolean
        private get() = btAdapter != null && this.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    //valida que el bluetooth este encendido
    private val blueToothOn: Boolean
        private get() = btAdapter != null && btAdapter!!.isEnabled

    ///llama la funcion que valida si el bluetooth este encendido
    private fun envio() {
        if (blueToothOn) {
            Log.i(TAG, "isBlueToothOn")
            transmitIBeacon()
        } else if (!isBluetoothLEAvailable) {
            val toast = Toast.makeText(
                this,
                "Bluetooth no disponible en su dispositivo.",
                Toast.LENGTH_LONG
            )
            toast.show()
        } else {
            Log.i(TAG, "BlueTooth is off")
            val toast = Toast.makeText(
                this,
                "Habilite bluetooth antes de transmitir iBeacon.",
                Toast.LENGTH_LONG
            )
            toast.show()
        }
    }

    // envio de beacons
    private fun transmitIBeacon() {
        val isSupported: Boolean
        isSupported = btAdapter!!.isMultipleAdvertisementSupported
        if (isSupported) {
            Log.v(TAG, "is support advertistment")
            if (beaconTransmitter!!.isStarted) {
                beaconTransmitter!!.stopAdvertising()
            } else {
                beaconTransmitter!!.startAdvertising(beacon, object : AdvertiseCallback() {
                    override fun onStartFailure(errorCode: Int) {
                        Log.e(TAG, "Advertisement start failed with code: $errorCode")
                    }

                    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                        Log.i(TAG, "Advertisement start succeeded.$settingsInEffect")
                    }
                })
            }
        } else {
            val toast = Toast.makeText(
                this,
                "Su dispositivo no es compatible con LE Bluetooth.",
                Toast.LENGTH_LONG
            )
            toast.show()
        }
    }

    //configuracion del beacon concatenando la MAC
    private fun setupBeacon() {
        val uuid = "954e6dac-5612-4642-b2d1-$mac"
        Log.v("Sergio", "uuid $uuid")
        beacon = Beacon.Builder()
            .setId1(uuid) // UUID for beacon
            .setId2("5") // Major for beacon
            .setId3("12") // Minor for beacon
            .setManufacturer(0x004C) // Radius Networks.0x0118  Change this for other beacon layouts//0x004C for iPhone
            .setTxPower(-56) // Power in dB
            .setDataFields(
                Arrays.asList(
                    2L,
                    3L
                )
            ) // Remove this for beacon layouts without d: fields
            .build()
        val btManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = btManager.adapter
        beaconTransmitter = BeaconTransmitter(
            this, BeaconParser()
                .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")
        )
    }

    //recepcion de beacons
    override fun onBeaconServiceConnect() {
        val region =
            Region("myBeaons", Identifier.parse("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6"), null, null)
        val region2 = Region("myBeaons", null, null, null)
        beaconManager!!.setMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region) {
                try {
                    Log.d(TAG, "didEnterRegion")
                    beaconManager!!.startRangingBeaconsInRegion(region)
                    beaconManager!!.startRangingBeaconsInRegion(region2)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun didExitRegion(region: Region) {
                try {
                    Log.d(TAG, "didExitRegion")
                    beaconManager!!.stopRangingBeaconsInRegion(region)
                    beaconManager!!.stopRangingBeaconsInRegion(region2)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun didDetermineStateForRegion(i: Int, region: Region) {}
        })

        //se obtienen los datos recibidos de los beacons
        beaconManager!!.setRangeNotifier { beacons: Collection<Beacon>, region1: Region? ->
            for (oneBeacon in beacons) {
                Log.d(
                    TAG, "distance: " + oneBeacon.distance + " adrres:" + oneBeacon.bluetoothAddress
                            + " id:" + oneBeacon.id1 + "/" + oneBeacon.id2 + "/" + oneBeacon.id3
                )

                mLista = eliminarDuplicados(mLista, oneBeacon)
                mAdapter = AdapterBluetooth(
                    this@BuscandoDispositivosBluetooth,
                    R.layout.adapter_dispositivos_encontrados_wifi,
                    mLista
                )
                listaBluetooth!!.adapter = mAdapter
            }
        }
        try {
            beaconManager!!.startMonitoringBeaconsInRegion(region)
            beaconManager!!.startMonitoringBeaconsInRegion(region2)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    // elimina el duplicados de dispositivos encontrados
    fun eliminarDuplicados(
        lista: MutableList<BluetoothFrame>,
        oneBeacon: Beacon
    ): MutableList<BluetoothFrame> {
        val c = Calendar.getInstance()
        @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val strDate = sdf.format(c.time)
        for (i in mLista.indices) {
            if (mLista[i].identifier == oneBeacon.id1) {
                mLista.removeAt(i)
            }
        }

        val frame = BluetoothFrame().apply {
            identifier = oneBeacon.id1
            distance = oneBeacon.distance
            date = strDate
        }

        mLista.add(frame)
        return lista
    }

    // valida los permisos de localizacion
    fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), 1
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        } else {
            checkPermission()
        }
    }

    @SuppressLint("MissingPermission")
    private fun locationUpdates() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager

        try {
            lm.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 100, 0f
            ) { location ->
                //longitude = location.longitude
                //latitude = location.latitude
                println("La ubicación ha cambiado chaval")
                //println("longitude: $longitude")
                //println("latitude: $latitude")
            }
        } catch (e: Exception) {
            Log.v("Sergio", e.toString())
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap.setMaxZoomPreference(100f)
        googleMap.setMinZoomPreference(5f)
    }

    companion object {
        private const val TAG = "Sergio"

        //transforma la MAC para que quede Hexadecimal
        fun trasformarMac(): String {
            var mac = ""
            Log.v("Sergio", "uuid|" + (obtenerMac() == "") + "|")
            return if (obtenerMac() == "") {
                alternativaMac()
            } else {
                val macArray = obtenerMac().split(":").toTypedArray()
                for (i in macArray.indices) {
                    if (macArray[i].length == 1) {
                        macArray[i] = macArray[i] + "0"
                    }
                }
                for (i in macArray) {
                    mac = mac + i
                }
                mac
            }
        }

        //TODO revisar traducción
        // optiene la MAC de los dispositivos si es posible
        fun obtenerMac(): String {
            try {
                val all: List<NetworkInterface> =
                    Collections.list(NetworkInterface.getNetworkInterfaces())
                for (nif in all) {
                    if (!nif.name.equals("wlan0", ignoreCase = true)) continue
                    val macBytes = nif.hardwareAddress ?: return ""
                    val res1 = StringBuilder()
                    for (b in macBytes) {
                        res1.append(Integer.toHexString((b and 0xFF.toByte()).toInt()) + ":")
                    }
                    if (res1.length > 0) {
                        res1.deleteCharAt(res1.length - 1)
                    }
                    return res1.toString()
                }
            } catch (ex: Exception) {
                Log.e("Error", ex.message!!)
            }
            return ""
        }

        // concatena los numeros aleatorios
        fun alternativaMac(): String {
            return numeroAleatorio() + numeroAleatorio()
        }

        // se genera numero aletario de 6 digitos
        fun numeroAleatorio(): String {
            val min_val: Long = 100000
            val max_val = 999999
            val randomNum = Math.random() * (max_val - min_val)
            println("Random Number: $randomNum")
            return randomNum.toInt().toString()
        }
    }
}