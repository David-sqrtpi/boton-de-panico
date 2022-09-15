package com.example.botondepanicov1.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.botondepanicov1.R
import java.io.*

class StorageManager {
    companion object {
        fun storeRawToLocal(context: Context){
            val `in` = context.resources.openRawResource(R.raw.bogota)
            val out = ByteArrayOutputStream()
            val buff = ByteArray(1024)
            var read: Int

            try {
                while (`in`.read(buff).also { read = it } > 0) {
                    out.write(buff, 0, read)
                }
            } finally {
                `in`.close()
                out.close()
            }

            val file = context.openFileOutput("bogota_tiles.mbtiles", Context.MODE_PRIVATE)
            file.write(out.toByteArray())
            file.flush()
            file.close()
        }

        fun getLocalFile(context: Context) {
            context.fileList().forEach { x -> println(x) }
            //context.getFileStreamPath("bogota_tiles.mbtiles")
        }
    }
}