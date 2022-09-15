package com.example.botondepanicov1.activities

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Environment
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.AlarmService
import com.example.botondepanicov1.R
import com.example.botondepanicov1.util.ExpandedMBTilesTileProvider
import com.example.botondepanicov1.util.OfflineTileProvider
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main_content.*
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import java.io.*


class MainContent : AppCompatActivity(), OnMapReadyCallback {
    private var toggleAlarm = false;
    private lateinit var toggleAlarmIB: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_content)
        toggleAlarmIB = findViewById(R.id.toggle_alarm)
        toggleAlarmIB.setImageResource(R.drawable.alarm_off)

        toggleAlarmIB.setOnClickListener {
            if(toggleAlarm){
                toggleOff()
            } else {
                toggleOn()
            }
        }

        //val mapFragment = supportFragmentManager
        //    .findFragmentById(R.id.map) as SupportMapFragment
        //mapFragment.getMapAsync(this)

        createLocationRequest()

        copyTiles()

        map.setTileSource(XYTileSource(
            "${Environment.getExternalStorageDirectory()}/bogota_tiles.mbtiles",
            6,
            19,
            256,
            ".jpg",
            emptyArray()
        ))

        map.setUseDataConnection(false)
        map.setMultiTouchControls(true)
        val mapController: IMapController = map.controller;
        mapController.setZoom(15)
        val startPoint: GeoPoint = GeoPoint(4.569616601020904, -74.2323260796485);
        mapController.setCenter(startPoint);
    }

    private fun createLocationRequest() {
        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            Toast.makeText(this, "La ubicación ya está activada", Toast.LENGTH_SHORT).show()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this,
                        Companion.REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if(resultCode == RESULT_OK){
                    Toast.makeText(this, "Se ha activado la ubicación", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No se ha activado la ubicación", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

    override fun onMapReady(googleMap: GoogleMap) {
        val tileProvider: TileProvider =
            ExpandedMBTilesTileProvider(MBTilesToFile(), 256, 256)

        googleMap.mapType = GoogleMap.MAP_TYPE_NONE

        googleMap
            .addTileOverlay(TileOverlayOptions().tileProvider(OfflineTileProvider(this)))

        googleMap.setMaxZoomPreference(19f)

        val cameraUpdate: CameraUpdate =
            CameraUpdateFactory.newLatLngZoom(LatLng(4.569616601020904, -74.2323260796485), 15f)

        googleMap.moveCamera(cameraUpdate)

        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(4.569616601020904, -74.2323260796485))
                .title("Yo")
        )
    }

    fun MBTilesToFile() : File{
        try {
            val inputStream = resources.openRawResource(R.raw.bogota_tiles)
            val tempFile = File.createTempFile("pre", "suf")
            copyFile(inputStream, FileOutputStream(tempFile))

            return tempFile
        } catch (e: IOException) {
            throw RuntimeException("Can't create temp file ", e)
        }
    }

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }

    private fun copyTiles() {
        val file = File(this.filesDir, "bogota_tiles.mbtiles")
        val `in` = resources.openRawResource(R.raw.bogota_tiles)
        val out = FileOutputStream(Environment.getExternalStorageDirectory())
        val buff = ByteArray(1024)
        var read = 0

        try {
            while (`in`.read(buff).also { read = it } > 0) {
                out.write(buff, 0, read)
            }
        } finally {
            `in`.close()
            out.close()
        }
    }

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 1;
    }
}