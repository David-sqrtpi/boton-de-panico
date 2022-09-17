package com.example.botondepanicov1

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.util.Constants
import kotlinx.android.synthetic.main.activity_login.*

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        continuar.setOnClickListener(onContinuarClickListener)
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
        val sharedPreferences: SharedPreferences = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(Constants.PREFERENCES_USERNAME, name.trim())
        editor.apply()
    }

    private fun startPanicButton() {
        val intent = Intent(this, PantallaPrincipal::class.java)
        startActivity(intent)
        finish()
    }
}