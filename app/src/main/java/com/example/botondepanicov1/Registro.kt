package com.example.botondepanicov1


import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.util.Log

import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
//import com.google.firebase.auth.*

import kotlinx.android.synthetic.main.activity_registro.*
import java.util.*
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_registro.error_password
import kotlinx.android.synthetic.main.activity_registro.password
import java.text.SimpleDateFormat


class Registro : AppCompatActivity() {
    //Variables para la conexion de FIREBASE
//    private lateinit var dbReferenciaUser: DatabaseReference
//    private lateinit var database: FirebaseDatabase
//    private lateinit var auth: FirebaseAuth
    //Variable para el correo
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        title = "REGISTRO"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        //Llamada de funcion
        inicializarSpinnerDocumento()
        inicializarSpinnerGenero()
        inicializarSpinnerRh()
        inicializarSpinnerSigno()
        //Inicializacion variables
//        database = FirebaseDatabase.getInstance()
//        auth = FirebaseAuth.getInstance()
        //Se le inidca el PATH con el cual busca el FIREBASE
//        dbReferenciaUser = database.reference.child("User")
    }
    //Asinga la fecha actual al calendario
    @SuppressLint("SetTextI18n")
    fun onClickCalendario(v: View) {
        val c = Calendar.getInstance()
        val dia = c.get(Calendar.DAY_OF_MONTH)
        val mes = c.get(Calendar.MONTH)
        val anio = c.get(Calendar.YEAR)
        val date = DatePickerDialog(
            this,
            { view, year, month, dayOfMonth ->
                fecha_nacimiento.setText("$dayOfMonth / ${month + 1} / $year")
            }, anio, mes, dia
        )
        date.show()
    }
    //Captura la accion btn registrar
    fun onClickRegistrar(v: View) {
        val cm =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
//        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true
        //Valida la conexion a internet
//        if (isConnected) {
//            //CONTROLAR QUE SI HAY ERROR, NO PASE A AUTENTIFICAR
//            if (falloRegistro()) {
//                autentificar()
//            }
//        } else {
//            mostrarErrorConexion()
//        }
    }
    //Se sube ka información del usuario a FIREBASE
    @SuppressLint("CommitPrefEdits")
    private fun crearNuevaCuenta() {
        val persona = Persona()
        persona.setTipoDocumento(document_type.selectedItem.toString())
        persona.setNumeroDocumento(document_number.text.toString())
        persona.setNombres(first_name.text.toString())
        persona.setApellidos(last_name.text.toString())
        persona.setGenero(gender.selectedItem.toString())
        persona.setRh(rh.selectedItem.toString() + signo.selectedItem.toString())
        persona.setFechaNacimiento(fecha_nacimiento.text.toString())
        persona.setContrasenia(password.text.toString())

        val user = email.replace("@gmail.com", "")
        Log.v("Sergio", user)
//        auth.createUserWithEmailAndPassword(email, persona.getContrasenia())
//            .addOnCompleteListener(this) { task ->
//                if (task.isComplete) {
//                    val userBD = dbReferenciaUser.child(user)
//                    userBD.child("tipo_documento").setValue(persona.getTipoDocumento())
//                    userBD.child("documento").setValue(persona.getNumeroDocumento())
//                    userBD.child("nombre").setValue(persona.getNombres())
//                    userBD.child("apellido").setValue(persona.getApellidos())
//                    userBD.child("genero").setValue(persona.getGenero())
//                    userBD.child("rh").setValue(persona.getRh())
//                    userBD.child("fecha_nacimiento").setValue(persona.getFechaNacimiento())
//                    userBD.child("contraseña").setValue(persona.getContrasenia())
//                }
//            }
    }

    //Valida que el ususario no este registrado, si no lo esta, lo crea. Si esta registrado arroja la alerta
    private fun autentificar() {
//        FirebaseAuth.getInstance()
//            .createUserWithEmailAndPassword(email, password.text.toString())
//            .addOnCompleteListener {
//                if (it.isSuccessful) {
//                    crearNuevaCuenta()
//                    mostrarExitoRegistro()
//
//                } else {
//                    mostrarErrorRegistro()
//                }
//            }
    }
    //Dialogo de registro exitoso
    private fun mostrarExitoRegistro() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Registro Exitoso")
            .setMessage("Gracias por confiar en nosotros")
            .setPositiveButton("Aceptar") { _, _ ->
                finish()
            }
            .create()
        dialog.show()
    }
    //Dialogo de error en conexion
    private fun mostrarErrorConexion() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage("Verifique su conexión a internet ")
            .setPositiveButton("Aceptar") { _, _ ->
            }
            .create()
        dialog.show()
    }
    //Dialogo de error en registro
    private fun mostrarErrorRegistro() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage("Ya se ha registrado anteriormente con este documento")
            .setPositiveButton("Aceptar") { _, _ ->
                finish()
            }
            .create()

        dialog.show()
    }
    //Valida que todos los campos esten diligenciados
    private fun falloRegistro(): Boolean {
        var resultado = true
        email = ""
        // Validcion campo password nonull
        if (password.text.toString().isEmpty()) {
            error_password.text = ("Ingrese la contraseña ")
            error_password.error = ""
            resultado = false
        } else {
            error_password.text = null
            error_password.error = null
        }
        // Validcion campo password_confirmation nonull
        when {
            password_confirmation.text.toString().isEmpty() -> {
                error_confirmation.text = ("Ingrese la contraseña ")
                error_confirmation.error = ""
                resultado = false
            }
            password_confirmation.text.toString() != password.text.toString() -> {
                error_password.text = ("Las contraseñas deben de ser iguales ")
                error_password.error = ""
                error_confirmation.text = ("Las contraseñas deben de ser iguales ")
                error_confirmation.error = ""
                resultado = false
            }
            else -> {
                error_confirmation.text = null
                error_confirmation.error = null
                if (password.text.toString().length < 6) {
                    error_password.text = ("La contraseña debe contener al menos 6 caracteres ")
                    error_password.error = ""
                    resultado = false
                }
                if (password_confirmation.text.toString().length < 6) {
                    error_confirmation.text =
                        ("La contraseña debe contener al menos  6 caracteres ")
                    error_confirmation.error = ""
                    resultado = false
                }
            }
        }
        //validacion spinner documento
        if (document_type.selectedItem == "Seleccione") {
            error_document_type.text = ("Seleccione una opción ")
            error_document_type.error = ""
            resultado = false
        } else {
            error_document_type.text = null
            error_document_type.error = null
        }
        //Validacion del campo numero documento
        if (document_number.text.toString().isEmpty()) {
            error_document_number.text = ("Ingrese su número de documento ")
            error_document_number.error = ""
            resultado = false
        } else {
            email =
                document_type.selectedItem.toString() + document_number.text.toString() + "@gmail.com"
            error_document_number.text = null
            error_document_number.error = null
        }
        //Validacion del campo nombre
        if (first_name.text.toString().isEmpty()) {
            error_first_name.text = ("Ingrese sus nombres ")
            error_first_name.error = ""
            resultado = false
        } else {
            error_first_name.text = null
            error_first_name.error = null
        }
        //Validacion del campo apellido
        if (last_name.text.toString().isEmpty()) {
            error_last_name.text = ("Ingrese sus apellidos ")
            error_last_name.error = ""
            resultado = false
        } else {
            error_last_name.text = null
            error_last_name.error = null
        }
        //validacion spinner genero
        if (gender.selectedItem == "Seleccione") {
            error_gender.text = ("Seleccione una opción ")
            error_gender.error = ""
            resultado = false
        } else {
            error_gender.text = null
            error_gender.error = null
        }
        //validacion spinner rh
        if (rh.selectedItem == "Seleccione") {
            error_rh.text = ("Seleccione una opción ")
            error_rh.error = ""
            resultado = false
        } else {
            error_rh.text = null
            error_rh.error = null
        }
        //validacion spinner rh
        if (signo.selectedItem == "") {
            error_rh.text = ("Seleccione una opción ")
            error_rh.error = ""
            resultado = false
        } else {
            error_rh.text = null
            error_rh.error = null
        }
        //Validacion del campo fecha nacimiento
        if (fecha_nacimiento.text.toString().isEmpty()) {
            error_fecha_nacimiento.text = ("Ingrese su fecha de nacimiento ")
            error_fecha_nacimiento.error = ""
            resultado = false
        } else {
            val edad = 10
            if (validacionEdad(fecha_nacimiento.text.toString()) < edad) {
                error_fecha_nacimiento.text = ("Minimo debe tener $edad años ")
                error_fecha_nacimiento.error = ""
                resultado = false
            } else {
                error_fecha_nacimiento.text = null
                error_fecha_nacimiento.error = null
            }
        }

        return resultado
    }
    //Resta entre la edad del usuario y la fecha actual
    @SuppressLint("SimpleDateFormat")
    private fun validacionEdad(fechaNacimiento: String): Int {
        val c = Calendar.getInstance()
        val diaActual = c.get(Calendar.DAY_OF_MONTH)
        var mesActual = c.get(Calendar.MONTH)
        mesActual += 1
        val anioActual = c.get(Calendar.YEAR)

        val formatoActual = SimpleDateFormat("dd/MM/yyyy")
        val formatoNacimiento = SimpleDateFormat("dd / MM / yyyy")
        val fechaActual: Date = formatoActual.parse("$diaActual/$mesActual/$anioActual")
        val fechaNacimiento: Date = formatoNacimiento.parse(fechaNacimiento)

        return fechaActual.year - fechaNacimiento.year
    }
    //Inicializa la lista desplegable del signo del RH
    private fun inicializarSpinnerSigno() {
        val spinnerSigno = findViewById<Spinner>(R.id.signo)
        val listaSigno = resources.getStringArray(R.array.signo)
        val adaptadorSigno =
            ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, listaSigno)

        spinnerSigno.adapter = adaptadorSigno
    }
    //Inicializa la lista desplegable tipos de RH
    private fun inicializarSpinnerRh() {
        val spinnerRh = findViewById<Spinner>(R.id.rh)
        val listaRh = resources.getStringArray(R.array.rh)
        val adaptadorRh =
            ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, listaRh)

        spinnerRh.adapter = adaptadorRh
    }
    //Inicializa la lista desplegable del genero
    private fun inicializarSpinnerGenero() {
        val spinnerGenero = findViewById<Spinner>(R.id.gender)
        val listaGenero = resources.getStringArray(R.array.genero)
        val adaptadorGenero =
            ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, listaGenero)

        spinnerGenero.adapter = adaptadorGenero
    }
    //Inicializa la lista desplegable del tipo documento
    private fun inicializarSpinnerDocumento() {
        val spinnerDocumento = findViewById<Spinner>(R.id.document_type)
        val listaDocumento = resources.getStringArray(R.array.tipos_documento)
        val adaptadorDocumento =
            ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, listaDocumento)

        spinnerDocumento.adapter = adaptadorDocumento
    }
}