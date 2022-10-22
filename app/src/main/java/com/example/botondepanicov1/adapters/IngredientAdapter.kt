package com.example.botondepanicov1.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import com.example.botondepanicov1.R
import com.example.botondepanicov1.models.Ingredient
import com.example.botondepanicov1.models.Role


class IngredientAdapter(
    private val context: Context,
    private var collection: Map<Role, List<Ingredient>>,
    private val groupList: List<Role>
) : BaseExpandableListAdapter() {
    override fun getGroupCount(): Int {
        return collection.size
    }

    override fun getChildrenCount(p0: Int): Int {
        return collection[groupList[p0]]?.size ?: 0
    }

    override fun getGroup(p0: Int): Any {
        return groupList[p0]
    }

    override fun getChild(p0: Int, p1: Int): Ingredient? {
        return collection[groupList[p0]]?.get(p1)
    }

    override fun getGroupId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getChildId(p0: Int, p1: Int): Long {
        return p1.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(p0: Int, p1: Boolean, p2: View?, p3: ViewGroup?): View {
        val groupID = groupList[p0]
        val childrenCount = getChildrenCount(p0)
        val view = p2 ?: if (groupID == Role.SURVIVOR)
            LayoutInflater.from(context).inflate(R.layout.group_item_survivor, p3, false)
        else LayoutInflater.from(context).inflate(R.layout.group_item_resucuer, p3, false)

        val textView: TextView = view.findViewById(R.id.text)
        textView.text = if (groupID == Role.SURVIVOR)
            "Sobrevivientes (${childrenCount})"
        else "Rescatistas (${childrenCount})"

        return view
    }

    override fun getChildView(p0: Int, p1: Int, p2: Boolean, p3: View?, p4: ViewGroup?): View {
        val ingredient = getChild(p0, p1)
        val view =
            p3 ?: LayoutInflater.from(context).inflate(R.layout.child_item, p4, false)

        val name = view.findViewById<TextView>(R.id.name)
        val distance = view.findViewById<TextView>(R.id.distance)

        if (ingredient != null) {
            name.text = ingredient.username
        }
        if (ingredient != null) {
            distance.text = "Distancia (GPS): ${ingredient.distance} metros"
        }

        return view
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean {
        return true
    }

    fun setData(collection: Map<Role, List<Ingredient>>) {
        this.collection = collection
        notifyDataSetChanged()
    }
}