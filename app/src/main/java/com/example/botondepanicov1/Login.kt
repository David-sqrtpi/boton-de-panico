package com.example.botondepanicov1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.botondepanicov1.activities.MainContent
import com.example.botondepanicov1.util.Constants
import kotlinx.android.synthetic.main.activity_login.*

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val validacionPermisos = ValidacionPermisos()
        validacionPermisos.validacionUbicacion(this)

        continuar.setOnClickListener{
            val name = user.text.toString()

            if(name.isNotBlank()){
                saveName(name)

                val intent = Intent(this, PantallaPrincipal::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(applicationContext, "Ingrese un nombre para continuar", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveName(name: String){
        val sharedPreference =  getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putString(Constants.PREFERENCES_USERNAME, name.trim())
        editor.apply()
    }
}