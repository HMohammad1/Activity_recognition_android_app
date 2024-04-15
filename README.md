# Dissertation_Android_App
First, make sure the Django server is running properly. 
This has been set up to read data from the TXT files and used via the Android Studio emulator. After opening the repository, launch the app inside the emulator and click the "Load" button, then "Not running." The data can be viewed using logcat inside Android Studio. Make sure to change the URL on line 24 in APIConnections.java with the new NGROK URL from the Django server. 
To change what file is played back, change the file name on line 269 in MainActivity.java. 

For buses, the file names include:
- bus-19-02-23-1-1.txt
- bus-19-02-23-1-2.txt
- bus-19-02-23-2-1.txt
- bus-19-02-23-2-2.txt
- bus-19-02-23-3-1.txt
- bus-19-02-23-3-2.txt
- bus-19-02-23-4-1.txt
- bus-19-02-23-4-2.txt
- bus-19-02-23-5-1.txt
- bus-19-02-23-5-2.txt

For cars, the file names include:
- car-19-02-23.txt
- car-19-02-23-1.txt
- car-19-02-23-2.txt
- car-16-02-23.txt
- car-16-02-23-1.txt
- car-16-02-23-2.txt
- car-16-02-23-3.txt
- car-16-02-23-4.txt
- car-16-02-23-5.txt
- car-16-02-23-6.txt
- car-16-02-23-7.txt
- car-16-02-23-8.txt
- car-16-02-23-9.txt

The -copy version of these files includes results from testing via the emulator. These files are located in scr/main/assets. 

## Using app outdoors
If the Django server is up and running:
1. Comment line 630
2. Uncomment lines 634 - 659
3. Comment lines 665 - 689
4. Uncomment line 695
5. Uncomment lines 717 - 730
6. Comment lines 737 - 753
7. Uncomment lines 905 - 906
8. Comment lines 915 - 925
9. Connect the mobile to Android Studio by enabling debugging mode on the phone. Then, under emulators, select pair devices using Wi-Fi.
10. Run and it should open on device
 

## Files
- Callbacks
> BusRouteCallback - callback for checking if on a bus route
> BusStopCallback - callback for getting the bus number
> PredictionCallback - callback for getting the prediction (deprecated)
- StepCounter.java
> Gets the step counter sensor
- SensorAdapter.java
> Retrieves all the sensors the phone has
- OrientationSensor.java
> Sensor for getting the orientation
- LocationService.java
> Gets the user's location
- MyForegroundService.java
> Service to get pings every 10 seconds so the data can be posted to the server / written to the file
- BarometerSensor,java
> Gets the barometer sensor
- AcceleromterSensor
> Gets the acceleromter
- APIConnections.java
> It makes all the API connections and needs the base URL provided by the Django server after setting up NGROK.
- MainActivity.java
> The central part of the code that launches the app and controls the broadcast receiver / ActivityRecognition API
-MyApplication.java
> Uses ARCA (https://www.acra.ch/docs/Setup) to get crash logs
