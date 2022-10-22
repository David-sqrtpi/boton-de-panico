package com.example.botondepanicov1

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.models.BluetoothFrame
import com.example.botondepanicov1.util.Constants
import com.example.botondepanicov1.util.Encoder
import org.altbeacon.beacon.*
import java.util.*
import kotlin.random.Random

class BuscandoDispositivosBluetooth : AppCompatActivity(), BeaconConsumer {
    // variables para la configuracion de los beacons
    private var beacon: Beacon? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var beaconManager: BeaconManager? = null
    private var beaconTransmitter: BeaconTransmitter? = null
    private var bluetoothList: MutableList<BluetoothFrame> = ArrayList()
    private var mac: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscando_dispositivos_bluetooth)

        val btManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = btManager.adapter

        //LEER PREFERENCIAS DE LA MAC
        mac = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
            .getString(Constants.PREFERENCES_MAC, null)

        Log.v("Sergio", "PREFERENCES_MAC $mac")

        if (mac == null) {
            mac = alternativaMac()
            guardarMacAleatoria(mac)
        }

        beaconManager = BeaconManager.getInstanceForApplication(this)
        setupBeacon()
        envio()
        beaconManager!!.beaconParsers.add(
            BeaconParser()
                .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")
        )
        inicioDescubrimiento()
    }

    //si no es posible obtener la MAC guarda la aleatoria en preferencias
    private fun guardarMacAleatoria(mac: String?) {
        val prefs = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(Constants.PREFERENCES_MAC, mac)
        editor.apply()
    }

    //termina la actividad cuando se hace uso de boton atras
    override fun onBackPressed() {
        beaconManager!!.unbind(this@BuscandoDispositivosBluetooth)
        finish()
    }

    // inica el descubrimiento de dispositivos
    private fun inicioDescubrimiento() {
        beaconManager!!.bind(this@BuscandoDispositivosBluetooth)
    }

    //valida que el bluetooth habilitado y compatible con el dispositivo
    private val isBluetoothLEAvailable: Boolean
        get() = bluetoothAdapter != null && this.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    //valida que el bluetooth este encendido
    private val bluetoothOn: Boolean
        get() = bluetoothAdapter != null && bluetoothAdapter!!.isEnabled

    private fun envio() {
        if (bluetoothOn) {
            Log.i(TAG, "isBluetoothOn")
            transmitIBeacon()
        } else if (!isBluetoothLEAvailable) {
            Toast.makeText(
                this, "Bluetooth no disponible en su dispositivo.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Log.i(TAG, "BlueTooth is off")
            Toast.makeText(
                this, "Habilite bluetooth antes de transmitir iBeacon.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // envio de beacons
    private fun transmitIBeacon() {
        val isSupported: Boolean = bluetoothAdapter!!.isMultipleAdvertisementSupported
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
            Toast.makeText(
                this, "Su dispositivo no es compatible con LE Bluetooth.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    //configuracion del beacon concatenando la MAC
    private fun setupBeacon() {
        val uuid = "954e6dac-5612-4642-b2d1-$mac"
        Log.v("Sergio", "uuid: $uuid")
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
        beaconManager!!.setRangeNotifier { beacons: Collection<Beacon>, _: Region? ->
            for (oneBeacon in beacons) {
                Log.d(
                    TAG,
                    "distance: " + oneBeacon.distance + " address:" + oneBeacon.bluetoothAddress
                            + " id:" + oneBeacon.id1 + "/" + oneBeacon.id2 + "/" + oneBeacon.id3
                )

                bluetoothList = eliminarDuplicados(bluetoothList, oneBeacon)
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
    private fun eliminarDuplicados(
        lista: MutableList<BluetoothFrame>,
        oneBeacon: Beacon
    ): MutableList<BluetoothFrame> {
        val strDate = Encoder.dateToString(Date())
        bluetoothList.removeAll { x -> x.identifier == oneBeacon.id1 }

        val frame = BluetoothFrame().apply {
            identifier = oneBeacon.id1
            distance = oneBeacon.distance
            date = strDate
        }

        bluetoothList.add(frame)
        return lista
    }

    companion object {
        private const val TAG = "Sergio"

        private fun alternativaMac(): String {
            return numeroAleatorio() + numeroAleatorio()
        }

        private fun numeroAleatorio(): String {
            val randomNum = Random.nextInt(100000, 999999)
            println("Random Number: $randomNum")
            return randomNum.toString()
        }
    }
}