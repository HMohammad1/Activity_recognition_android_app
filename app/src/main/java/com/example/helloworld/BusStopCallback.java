package com.example.helloworld;

import java.util.ArrayList;

public interface BusStopCallback {
    void onResult(int resultLength);
    void onAllBusStops(ArrayList<String> busses);
}
