package com.example.helloworld;

import java.util.ArrayList;
// Callback to get the bus stops, onResult is to get the total while onAllBusStops returns all the data
public interface BusStopCallback {
    void onResult(int resultLength);
    void onAllBusStops(ArrayList<String> busses);
}
