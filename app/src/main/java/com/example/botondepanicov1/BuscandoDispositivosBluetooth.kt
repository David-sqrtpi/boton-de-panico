package com.example.botondepanicov1

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.models.BluetoothFrame
import com.example.botondepanicov1.util.Constants
import com.example.botondepanicov1.util.Encoder
import org.altbeacon.beacon.*
import java.util.*
import kotlin.random.Random

class BuscandoDispositivosBluetooth : AppCompatActivity(), RangeNotifier, MonitorNotifier {
    private lateinit var beacon: Beacon
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var beaconManager: BeaconManager
    private lateinit var beaconTransmitter: BeaconTransmitter
    private var bluetoothList: MutableList<BluetoothFrame> = ArrayList()
    private var mac: String? = null
    private var beaconParser = BeaconParser()
        .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscando_dispositivos_bluetooth)

        val btManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = btManager.adapter

        mac = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
            .getString(Constants.PREFERENCES_MAC, null)

        Log.v(TAG, "PREFERENCES_MAC $mac")

        if (mac == null) {
            mac = lastUUIDPart()
            saveLastUUIDPart(mac)
        }

        beaconManager = BeaconManager.getInstanceForApplication(this)
        setupBeacon()
        checkBTToTransmit()
        beaconManager.beaconParsers.add(beaconParser)
        onBeaconServiceConnect()
    }

    //si no es posible obtener la MAC guarda la aleatoria en preferencias
    private fun saveLastUUIDPart(mac: String?) {
        val prefs = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(Constants.PREFERENCES_MAC, mac)
        editor.apply()
    }

    //valida que el bluetooth habilitado y compatible con el dispositivo
    private val isBluetoothLEAvailable: Boolean
        get() = this.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    private fun checkBTToTransmit() {
        if (bluetoothAdapter.isEnabled) {
            Log.i(TAG, "Bluetooth is enabled")
            transmitIBeacon()
        } else if (!isBluetoothLEAvailable) {
            Toast.makeText(
                this, "Bluetooth no disponible en su dispositivo.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Log.i(TAG, "Bluetooth is off")
            Toast.makeText(
                this, "Habilite bluetooth antes de transmitir iBeacon.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // envio de beacons
    private fun transmitIBeacon() {
        if (bluetoothAdapter.isMultipleAdvertisementSupported) {
            Log.v(TAG, "Multiple advertisement supported")
            if (beaconTransmitter.isStarted) {
                beaconTransmitter.stopAdvertising()
            } else {
                beaconTransmitter.startAdvertising(beacon, object : AdvertiseCallback() {
                    override fun onStartFailure(errorCode: Int) {
                        Log.e(TAG, "Advertisement start failed with code: $errorCode")
                    }

                    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                        Log.i(TAG, "Advertisement start succeeded. $settingsInEffect")
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

    //initialize beacon and beaconTransmitter
    private fun setupBeacon() {
        //TODO verificar si tiene que ver con setDataFields y layout
        val uuid = "954e6dac-5612-4642-b2d1-$mac"
        Log.v(TAG, "uuid: $uuid")
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

    //recepcion de beacons
    private fun onBeaconServiceConnect() {
        val region =
            Region("panic-button-region", null, null, null)

        beaconManager.addMonitorNotifier(this)
        beaconManager.addRangeNotifier(this)
        beaconManager.startMonitoring(region)
        beaconManager.startRangingBeacons(region)
    }

    //TODO verificar para integrar con ingredient en wifi
    private fun deleteDuplicated(
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

        private fun lastUUIDPart(): String {
            return "${randNumber()}${randNumber()}"
        }

        private fun randNumber(): Int {
            return Random.nextInt(100000, 999999)
        }
    }

    override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>?, region: Region?) {
        if (beacons != null) {
            for (oneBeacon in beacons) {
                Log.d(
                    TAG,
                    "distance: " + oneBeacon.distance + " address:" + oneBeacon.bluetoothAddress
                            + " id:" + oneBeacon.id1 + "/" + oneBeacon.id2 + "/" + oneBeacon.id3
                )

                bluetoothList = deleteDuplicated(bluetoothList, oneBeacon)
            }
        }
    }

    override fun didEnterRegion(region: Region?) {
        Log.d(TAG, "didEnterRegion")
    }

    override fun didExitRegion(region: Region?) {
        Log.d(TAG, "didExitRegion")
    }

    override fun didDetermineStateForRegion(state: Int, region: Region?) {
        Log.d(TAG, "didDetermineStateForRegion")
    }
}