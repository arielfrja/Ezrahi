package com.arielfaridja.ezrahi.UI

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.arielfaridja.ezrahi.R

class IconTextAdapter(context: Context?, private val elements: Array<Triple<Int, Int, Int>>) :
    ArrayAdapter<Triple<Int, Int, Int>>(
        context!!,
        R.layout.icon_text_layout, R.id.text_spinner, elements
    ) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.icon_text_layout, parent, false)
            val icon = view.findViewById<ImageView>(R.id.icon_spinner)
            val text = view.findViewById<TextView>(R.id.text_spinner)
            holder = ViewHolder(icon, text)
        } else {
            view = convertView
            holder = ViewHolder(
                view.findViewById(R.id.icon_spinner),
                view.findViewById(R.id.text_spinner)
            )
        }
        val element = getItem(position)
        holder.icon.setImageDrawable(ContextCompat.getDrawable(context, element!!.first))
        holder.text.text = context.getString(element.second)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.icon_text_layout, parent, false)
            val icon = view.findViewById<ImageView>(R.id.icon_spinner)
            val text = view.findViewById<TextView>(R.id.text_spinner)
            holder = ViewHolder(icon, text)
        } else {
            view = convertView
            holder = ViewHolder(
                view.findViewById(R.id.icon_spinner),
                view.findViewById(R.id.text_spinner)
            )
        }
        val element = getItem(position)
        holder.icon.setImageDrawable(ContextCompat.getDrawable(context, element!!.first))
        holder.text.text = context.getString(element.second)
        return view
    }

    private class ViewHolder(val icon: ImageView, val text: TextView)
}