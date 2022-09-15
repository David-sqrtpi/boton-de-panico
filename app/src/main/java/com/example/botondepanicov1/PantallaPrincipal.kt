package com.example.botondepanicov1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.botondepanicov1.wifi_direct.BuscandoDispositivosWifi
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.preference.PreferenceManager
import android.util.Log
import com.example.botondepanicov1.activities.MainContent
import kotlinx.android.synthetic.main.activity_pantalla_principal.*


class PantallaPrincipal : AppCompatActivity() {
    //Variables usadas para las prefrencias
    private var key: String = "MY_KEY"//Datos personales
    private var keyLogin: String = "LOGIN"//Logueo auto
    private var keyAlarma: String = "ALARMA"//Etsado alarma

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_principal)

        val sharedPreference =  getSharedPreferences("PREFERENCE_NAME",MODE_PRIVATE)
        val name = sharedPreference.getString("name", "Usuario")

        saludo.text = "Hola, $name"

        estadoAlarma()
    }

    //Asigna INACTIVO a la alarma para que inicie apagda
    fun estadoAlarma(){
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs.edit()
        editor.putString(keyAlarma, "Inactiva")
        editor.apply()
    }
    //Captura accion boton solicitar ayuda
    fun onClickSolicitarAyuda(v:View){
        val intent = Intent(this, BuscandoDispositivosWifi::class.java)
        startActivity(intent)
    }
    //Captura accion boton cerrar sesion
    fun onClickCerrarSesion(v:View){
        finish()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        Log.v("Sergio","Pref a " + prefs.getString(key,"1").toString())
        Log.v("Sergio","Pref a" + prefs.getString(keyLogin,"2").toString())
        Log.v("Sergio","Pref a" + prefs.getString(keyAlarma,"3").toString())

        val editor = prefs.edit()
        editor.clear().apply()

        Log.v("Sergio","Pref d" + prefs.getString(key,"1").toString())
        Log.v("Sergio","Pref d" + prefs.getString(keyLogin,"2").toString())
        Log.v("Sergio","Pref d" + prefs.getString(keyAlarma,"3").toString())

        val intent = Intent(this,Login::class.java)
        startActivity(intent)
    }
}