package com.example.botondepanicov1.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import java.io.*
import java.net.HttpURLConnection
import java.net.URL


class OfflineTileProvider(context: Context) : TileProvider {
    private var context: Context? = context


    override fun getTile(x: Int, y: Int, z: Int): Tile {
        try {
            val data: ByteArray
            val file = File(TILES_DIR + z, x.toString() + "_" + y + ".png")
            if (false) {
                data = readTile(FileInputStream(file), BUFFER_SIZE_FILE)
            } else {
                if (!isInternetAvailable()) {
                    return TileProvider.NO_TILE
                }

                data = readTile(
                    getOSMTile(x, y, z),
                    BUFFER_SIZE_NETWORK
                )
                //BufferedOutputStream(FileOutputStream(file)).use { out -> out.write(data) }
            }
            println("TILE")
            return Tile(TILE_WIDTH, TILE_HEIGHT, data)
        } catch (ex: Exception) {
            println("NO_TILE")
            println(ex)
            return TileProvider.NO_TILE
        }
    }

    @Throws(IOException::class)
    private fun readTile(`in`: InputStream, bufferSize: Int): ByteArray {
        val buffer = ByteArrayOutputStream()
        try {
            var i: Int
            val data = ByteArray(bufferSize)
            while ((`in`.read(data, 0, bufferSize).also { i = it }) != -1) {
                buffer.write(data, 0, i)
            }
            buffer.flush()
            return buffer.toByteArray()
        } finally {
            `in`.close()
            buffer.close()
        }
    }

    private fun isInternetAvailable(): Boolean {
        var result = false
        val connectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }

        return result
    }

    @Throws(IOException::class)
    private fun getOSMTile(x: Int, y: Int, z: Int): InputStream{
        val url =
            URL("https://a.tile.openstreetmap.org/$z/$x/$y.png")
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty(
            "User-Agent",
            "Panic Button/1.0"
        )
        connection.connect()
        connection.requestMethod = "GET"

        return connection.inputStream
    }

    companion object {
        private const val TILES_DIR = "/storage/emulated/0/Android/data/com.example.botondepanicov1/tiles"
        private const val TILE_WIDTH = 256
        private const val TILE_HEIGHT = 256
        private const val BUFFER_SIZE_FILE = 16384
        private const val BUFFER_SIZE_NETWORK = 8192
    }
}