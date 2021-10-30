package com.example.botondepanicov1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.error_password
import kotlinx.android.synthetic.main.activity_login.password

class Login : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    private lateinit var email: String
    private var key: String = "MY_KEY"
    private var keyLogin: String = "LOGIN"

    override fun onCreate(savedInstanceState: Bundle?) {
        title = "BOTÓN DE PÁNICO"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val validacionPermisos = ValidacionPermisos()
        validacionPermisos.validacionUbicacion(this)

        iniciarSesionAutomatico()
        inicializarSpinnerDocumento()
        auth = Firebase.auth
    }

    private fun capturarDatosPersona() {
        val mDatabase = FirebaseDatabase.getInstance().reference
        val documento = email.replace("@gmail.com","")
        val persona = Persona()
        mDatabase.child("User").child(documento).get().addOnSuccessListener {
            persona.setTipoDocumento(it.child("tipo_documento").value.toString())
            persona.setNumeroDocumento(it.child("documento").value.toString())
            persona.setNombres(it.child("nombre").value.toString())
            persona.setApellidos(it.child("apellido").value.toString())
            persona.setGenero(it.child("genero").value.toString())
            persona.setRh(it.child("rh").value.toString())
            persona.setFechaNacimiento(it.child("fecha_nacimiento").value.toString())

            Log.v("Sergio", persona.concatenado())
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = prefs.edit()
            editor.putString(key, persona.concatenado())
            editor.apply()
        }
    }

    fun capturarCredenciales(email: String, contrasenia: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs.edit()
        editor.putString(keyLogin, "$email;$contrasenia")
        editor.apply()
    }

    fun iniciarSesionAutomatico() {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val datos = pref.getString(keyLogin, "No hay datos").toString()
        Log.v("Sergio", datos)
        if (datos != "No hay datos") {
            finish()
            val intent = Intent(this, PantallaPrincipal::class.java)
            startActivity(intent)
        }
    }

    fun iniciarSesion(v: View) {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true
        //Valida la conexion a internet
        if (isConnected){
            //CONTROLAR QUE SI HAY ERROR, NO PASE A AUTENTIFICAR
            if (falloInisioDeSesion()) {
                validacionFirebase()
            }
        } else {
            mostrarErrorConexion()
        }
    }

    private fun mostrarErrorConexion() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage("Verifique su conexión a internet ")
            .setPositiveButton("Aceptar") { _, _ ->
            }
            .create()
        dialog.show()
    }

    private fun validacionFirebase() {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password.text.toString())
            .addOnCompleteListener {
                FirebaseDatabase.getInstance().getReference("/User")
                if (it.isSuccessful) {
                    capturarDatosPersona()
                    capturarCredenciales(email, password.text.toString())
                    finish()
                    val intent = Intent(this, PantallaPrincipal::class.java)
                    startActivity(intent)
                } else {
                    credencialesInvalidas()
                }
            }
    }

    private fun credencialesInvalidas() {
        error_validacion.text = ("Credenciales invalidas")
        error_validacion.error = ""
    }

    private fun falloInisioDeSesion(): Boolean {
        var resultado = true
        email = ""
        //validacion spinner documento
        if (document_type_login.selectedItem == "Seleccione") {
            error_document_type_login.text = ("Seleccione una opción ")
            error_document_type_login.error = ""
            resultado = false
        } else {
            error_document_type_login.text = null
            error_document_type_login.error = null
        }
        // valicaion campo numero de documento
        if (user.text.toString().isEmpty()) {
            error_user.text = ("El documento es necesario")
            error_user.error = ""
            resultado = false
        } else {
            email =
                document_type_login.selectedItem.toString() + user.text.toString() + "@gmail.com"
            error_user.text = null
            error_user.error = null
        }
        //valicion campo contraseña
        if (password.text.toString().isEmpty()) {
            error_password.text = ("La contraseña es necesaria")
            error_password.error = ""
            resultado = false
        } else {
            error_password.text = null
            error_password.error = null
        }
        return resultado
    }

    private fun inicializarSpinnerDocumento() {
        val spinnerDocumento = findViewById<Spinner>(R.id.document_type_login)
        val listaDocumento = resources.getStringArray(R.array.tipos_documento)
        val adaptadorDocumento =
            ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, listaDocumento)

        spinnerDocumento.adapter = adaptadorDocumento
    }

    fun onClickInicarSesionSinCredenciales(v: View) {
        val intent = Intent(this, PantallaPrincipal::class.java)
        startActivity(intent)
    }

    fun onClickRegistro(v: View) {
        val intent = Intent(this, Registro::class.java)
        startActivity(intent)
    }
}