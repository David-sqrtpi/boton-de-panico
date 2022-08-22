package com.example.botondepanicov1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val validacionPermisos = ValidacionPermisos()
        validacionPermisos.validacionUbicacion(this)

        val next: Button = findViewById(R.id.continuar)

        next.setOnClickListener{
            val intent = Intent(this, PantallaPrincipal::class.java)
            startActivity(intent)
        }
    }

    fun validateName (){

    }
}