package com.example.botondepanicov1.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.R
import com.example.botondepanicov1.util.Constants
import kotlinx.android.synthetic.main.activity_login.*
import java.util.*

class Login : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        continuar.setOnClickListener(onContinuarClickListener)

        sharedPreferences = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
        val savedName = sharedPreferences.getString(Constants.PREFERENCES_USERNAME, "")

        user.setText(savedName)
    }

    private val onContinuarClickListener = View.OnClickListener {
        val name = user.text.toString()

        if (name.isNotBlank()) {
            saveName(name)

            startPanicButton()
        } else {
            Toast.makeText(
                applicationContext,
                "Ingrese un nombre para continuar",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun saveName(name: String) {
        val editor = sharedPreferences.edit()
        editor.putString(Constants.PREFERENCES_USERNAME, name.trim())
        editor.apply()
    }

    private fun startPanicButton() {
        sharedPreferences.getString(Constants.PREFERENCES_UUID, null) ?: saveUUID()
        val intent = Intent(this, PantallaPrincipal::class.java)
        startActivity(intent)
        finish()
    }

    private fun saveUUID() {
        val uuid = UUID.randomUUID().toString()
        val editor = sharedPreferences.edit()
        editor.putString(Constants.PREFERENCES_UUID, uuid)
        editor.apply()
    }
}