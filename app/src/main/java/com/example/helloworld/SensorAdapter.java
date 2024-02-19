package com.example.helloworld;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.hardware.Sensor;
import java.util.List;

public class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.ViewHolder> {

    private List<Sensor> sensors;
    private LayoutInflater inflater;

    public SensorAdapter(Context context, List<Sensor> sensors) {
        this.inflater = LayoutInflater.from(context);
        this.sensors = sensors;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sensor sensor = sensors.get(position);
        holder.sensorName.setText(sensor.getName());
    }

    @Override
    public int getItemCount() {
        return sensors.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView sensorName;

        ViewHolder(View itemView) {
            super(itemView);
            sensorName = itemView.findViewById(android.R.id.text1);
        }
    }
}

