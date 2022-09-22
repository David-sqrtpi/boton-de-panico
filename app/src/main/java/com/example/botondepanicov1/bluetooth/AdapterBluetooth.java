package com.example.botondepanicov1.bluetooth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.botondepanicov1.R;
import com.example.botondepanicov1.models.BluetoothFrame;

import java.util.List;

public class AdapterBluetooth extends ArrayAdapter<BluetoothFrame> {

    //variables para las lista de los dispositivos bluetooth
    private final List<BluetoothFrame> miLista;
    private final Context mContext;
    private final int resourceLayout;

    public AdapterBluetooth(@NonNull Context context, int resource, List<BluetoothFrame> objects) {
        super(context, resource, objects);
        this.miLista = objects;
        this.mContext = context;
        this.resourceLayout = resource;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    //muestra los dispositivos bluetooth con sus losn datos recibidos
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(resourceLayout, null);
        }

        BluetoothFrame dispostivo = miLista.get(position);

        TextView nombre = view.findViewById(R.id.name);
        nombre.setText("Dispositivo: " + dispostivo.getDeviceName());

        TextView distancia = view.findViewById(R.id.distancia);
        if (dispostivo.getDistance() < 0) {
            distancia.setText("Distancia: " + 0 + "metros");
        } else {
            distancia.setText("Distancia: " + dispostivo.getDistance() + "metros");
        }


        //TextView indice = view.findViewById(R.id.indice);
        //indice.setText("Fecha de actualización: ");
        TextView fecha = view.findViewById(R.id.fecha);
        fecha.setText(dispostivo.getDate());
        return view;
    }
}
