package com.example.botondepanicov1.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.botondepanicov1.databinding.ActivityLoginBinding
import com.example.botondepanicov1.util.Constants
import java.util.*

class Login : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.continuar.setOnClickListener(onContinuarClickListener)

        sharedPreferences = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
        val savedName = sharedPreferences.getString(Constants.PREFERENCES_USERNAME, "")

        binding.user.setText(savedName)
    }

    private val onContinuarClickListener = View.OnClickListener {
        val name = binding.user.text.toString()

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