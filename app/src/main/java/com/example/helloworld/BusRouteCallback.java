package com.example.helloworld;

import java.util.ArrayList;

// Callback to get the bus routes to keep data asynchronous
public interface BusRouteCallback {
    void onResult(ArrayList<String> result);
}
