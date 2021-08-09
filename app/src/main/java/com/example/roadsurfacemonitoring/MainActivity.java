package com.example.roadsurfacemonitoring;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import static android.hardware.Sensor.TYPE_GYROSCOPE;


public class MainActivity extends AppCompatActivity {

    private SensorManager manager;
    private SensorEventListener listener;
    FusedLocationProviderClient fusedLocationProviderClient;
    DateFormat dateFormat;
    String date;
    File[] root;
    FileOutputStream fileOutputStream = null;
    File file;

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    private static double latitude = 0d;
    private static double longitude = 0d;
    private static float speed = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        checkExternalMedia();
        root = getExternalFilesDirs(null);
        Log.i("root", "External file system root: " + root[1]);
        file = new File(root[1], "results.csv");

        dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION
            );
        }
        else {
            startLocationService();
        }

        manager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        listener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor arg0, int arg1) {
            }

            float Aa = 0, Ab = 0, Ac = 0, Ga = 0, Gb = 0, Gc = 0;
            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor sensor = event.sensor;
                String output;

                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    Aa = event.values[0];
                    Ab = event.values[1];
                    Ac = event.values[2];
                }
                if (sensor.getType() == TYPE_GYROSCOPE) {
                    Ga = event.values[0];
                    Gb = event.values[1];
                    Gc = event.values[2];

//                    if (Aa >= 1f || Aa <= -1f || Ab >= 10f || Ab < 9f ||
//                        Ga >= 0.5f || Ga <= -0.5f || Gc >= 0.5f || Gc <= -0.5f) {
                        date = dateFormat.format(Calendar.getInstance().getTime());
                        output = date + "," + LocationService.getCoordinates() + "," + Aa + "," + Ab + "," + Ac + "," + Ga + "," + Gb + "," + Gc + "\n";

                        try {
                            fileOutputStream = new FileOutputStream(file, true);
                            fileOutputStream.write(output.getBytes());
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (fileOutputStream != null) {
                                try {
                                    fileOutputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
//                    }

                    //Log.i("", output);
                }
            }
        };

        manager.registerListener(listener, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(listener, manager.getDefaultSensor(TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
    }

    private void checkExternalMedia(){
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        Log.i("storage", "External Media: readable="+mExternalStorageAvailable+" writable="+mExternalStorageWriteable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startLocationService();
            }
            else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isLocationServiceRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null){
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)){
                if (LocationService.class.getName().equals(service.service.getClassName())){
                    if (service.foreground){
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    private void startLocationService(){
        if (!isLocationServiceRunning()){
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this, "Location Service Started", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationService(){
        if (isLocationServiceRunning()){
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            startService(intent);
        }
    }

//    private String getCurrentLocation() {
//        LocationRequest locationRequest = new LocationRequest();
//        locationRequest.setInterval(4000);
//        locationRequest.setFastestInterval(2000);
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            LocationServices.getFusedLocationProviderClient(MainActivity.this).requestLocationUpdates(
//                    locationRequest, new LocationCallback() {
//                        @Override
//                        public void onLocationResult(@NonNull LocationResult locationResult) {
//                            super.onLocationResult(locationResult);
//                            LocationServices.getFusedLocationProviderClient(MainActivity.this).removeLocationUpdates(this);
//                            if (locationResult != null && locationResult.getLocations().size() > 0){
//                                int latestLocationIndex = locationResult.getLocations().size() - 1;
//                                latitude = locationResult.getLocations().get(latestLocationIndex).getLatitude();
//                                longitude = locationResult.getLocations().get(latestLocationIndex).getLongitude();
//                                speed = locationResult.getLocations().get(latestLocationIndex).getSpeed();
//                            }
//                        }
//                    }, Looper.getMainLooper()
//            );
//            return latitude + "," + longitude + "," + speed;
//        } else {
//        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION); //44
//             return null;
//        }
//    }


    @Override
    protected void onStop() {
        super.onStop();
        stopLocationService();
    }

    public void btn_groapa_onClick(View view) {
        date = dateFormat.format(Calendar.getInstance().getTime());
        String text = date + ",Groapa\n";
        try {
            fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write(text.getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void btn_speedHump_onClick(View view) {
        date = dateFormat.format(Calendar.getInstance().getTime());
        String text = date + ",Limitator de viteza\n";
        try {
            fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write(text.getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void btn_tren_onClick(View view) {
        date = dateFormat.format(Calendar.getInstance().getTime());
        String text = date + ",Cale ferata\n";
        try {
            fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write(text.getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
