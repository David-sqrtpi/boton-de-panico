package com.example.botondepanicov1.activities

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.AlarmService
import com.example.botondepanicov1.R
import com.example.botondepanicov1.util.MapBoxOfflineTileProvider
import com.example.botondepanicov1.util.StorageManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.tasks.Task
import java.io.File


class MainContent : AppCompatActivity(), OnMapReadyCallback {
    private var toggleAlarm = false
    private lateinit var toggleAlarmIB: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StorageManager.storeRawToLocal(this)
        //StorageManager.getLocalFile(this)

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

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        createLocationRequest()

        /*map.setUseDataConnection(false)

        map.setTileSource(XYTileSource(
            "/data/user/0/com.example.botondepanicov1/files/bogota_tiles.mbtiles",
            6,
            19,
            256,
            ".jpg",
            emptyArray()
        ))

        map.setMultiTouchControls(true)

        val mapController: IMapController = map.controller;
        mapController.setZoom(15)
        val startPoint = GeoPoint(4.569616601020904, -74.2323260796485)
        mapController.setCenter(startPoint)*/
    }

    private fun createLocationRequest() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
            maxWaitTime = 20000
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            Toast.makeText(this, "La ubicación ya está activada", Toast.LENGTH_SHORT).show()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this,
                        REQUEST_CHECK_SETTINGS
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
        val file = File(applicationContext.filesDir, "bogota_tiles.mbtiles")

        val tileProvider: TileProvider =
            MapBoxOfflineTileProvider(file)

        googleMap
            .addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))

        googleMap.mapType = GoogleMap.MAP_TYPE_NONE

        /*googleMap.setMaxZoomPreference(10f)*/

        /*val cameraUpdate: CameraUpdate =
            CameraUpdateFactory.newLatLngZoom(LatLng(4.569616601020904, -74.2323260796485), 10f)

        googleMap.moveCamera(cameraUpdate)*/

        /*googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(4.569616601020904, -74.2323260796485))
                .title("Yo")
        )*/
    }

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 1
    }
}