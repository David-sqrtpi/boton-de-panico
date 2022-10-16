package com.example.botondepanicov1.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.botondepanicov1.R
import com.example.botondepanicov1.wifi_direct.Ingredient

class IngredientAdapter (context: Context, resource: Int, ingredients: List<Ingredient>) :
    ArrayAdapter<Ingredient>(context, resource, ingredients) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row: View = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.adapter_dispositivos_encontrados_wifi, parent, false)

        val ingredient = getItem(position)

        val name = row.findViewById<TextView>(R.id.name)
        val distance = row.findViewById<TextView>(R.id.distancia)
        name.text = ingredient?.username
        distance.text = ingredient?.distance.toString()

        return row
    }
}