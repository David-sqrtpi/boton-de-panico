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

class BuscandoDispositivosBluetooth : AppCompatActivity(), RangeNotifier, MonitorNotifier {
    private lateinit var beacon: Beacon
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var beaconManager: BeaconManager
    private lateinit var beaconTransmitter: BeaconTransmitter
    private var bluetoothList: MutableList<BluetoothFrame> = ArrayList()
    private var uuid: String? = null
    private var beaconParser = BeaconParser()
        .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscando_dispositivos_bluetooth)

        val btManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = btManager.adapter

        uuid = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
            .getString(Constants.PREFERENCES_UUID, null)

        Log.v(Constants.TAG_BT, "PREFERENCES_MAC $uuid")

        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            saveUUID(uuid)
        }

        beaconManager = BeaconManager.getInstanceForApplication(this)
        setupBeacon()
        checkBTToTransmit()
        beaconManager.beaconParsers.add(beaconParser)
        onBeaconServiceConnect()
    }

    //TODO si no es posible obtener la MAC guarda la aleatoria en preferencias
    private fun saveUUID(UUID: String?) {
        val prefs = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(Constants.PREFERENCES_UUID, UUID)
        editor.apply()
    }

    private val isBluetoothLEAvailable: Boolean
        get() = this.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    private fun checkBTToTransmit() {
        if (bluetoothAdapter.isEnabled) {
            Log.i(Constants.TAG_BT, "Bluetooth is enabled")
            transmitIBeacon()
        } else if (!isBluetoothLEAvailable) {
            Toast.makeText(
                this, "Bluetooth no disponible en su dispositivo.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Log.i(Constants.TAG_BT, "Bluetooth is off")
            Toast.makeText(
                this, "Habilite bluetooth antes de transmitir iBeacon.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // TODO enviar mensaje por WIFI de que no tiene BLE
    private fun transmitIBeacon() {
        if (bluetoothAdapter.isMultipleAdvertisementSupported) {
            Log.v(Constants.TAG_BT, "Multiple advertisement supported")
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
        } else {
            Toast.makeText(
                this, "Su dispositivo no es compatible con LE Bluetooth.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    //initialize beacon and beaconTransmitter
    private fun setupBeacon() {
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
        list: MutableList<BluetoothFrame>,
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

    override fun didEnterRegion(region: Region?) { }

    override fun didExitRegion(region: Region?) { }

    override fun didDetermineStateForRegion(state: Int, region: Region?) { }
}