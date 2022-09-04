package com.example.botondepanicov1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.botondepanicov1.activities.MainContent
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
        val sharedPreference =  getSharedPreferences("PREFERENCE_NAME", MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putString("name", name)
        editor.apply()
    }
}