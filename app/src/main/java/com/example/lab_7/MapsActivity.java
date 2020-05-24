package com.example.lab_7;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;

//OD TYPA
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;



public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
    GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMapLongClickListener,
    SensorEventListener{
    
    private TextView labelText;
    private Sensor mSensor;
    private SensorManager sensorManager;

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;
    
    List<Marker> markerList;
    List<Pozycje> markerPositions;
    private final String POINTS_JSON_FILE = "markerpoint.json";

    //animation
    ViewGroup vgroup;
    FloatingActionButton hide;
    FloatingActionButton startstop;
    TextView sensor;
    boolean visible;
    boolean isonTop;
    boolean record = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        markerList = new ArrayList<>();
        markerPositions = new ArrayList<>();
        restoreFromJson();

        //animation
        vgroup = findViewById(R.id.mainView);
        hide = findViewById(R.id.Hide);
        startstop = findViewById(R.id.Switchss);
        sensor = findViewById(R.id.labelText);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        labelText = (TextView)findViewById(R.id.labelText);
    }

    private long lastUpdate = -1;
    @Override
    public void onSensorChanged(SensorEvent event) {
        long timeMicro;
        if(lastUpdate == -1) {
            lastUpdate = event.timestamp;
            timeMicro = 0;
        }else{
            timeMicro = (event.timestamp - lastUpdate)/1000L;
            lastUpdate = event.timestamp;
        }
        /*StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Time difference: ").append(timeMicro).append(" \u03bcs\n");
        for(int i =0; i<event.values.length;i++) {
            stringBuilder.append(String.format("Val[%d]=%.4f\n",i,event.values[i]));
        }*/
        labelText.setText("Acceleration: \n");
        labelText.append("X: " + event.values[0]);
        labelText.append(" Y: " + event.values[1]);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
    }

    private void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates()
    {
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private void createLocationCallback()
    {
        locationCallback = new LocationCallback()
        {
            @Override
            public void onLocationResult(LocationResult locationResult)
            {
                if(locationResult != null)
                {
                    if(gpsMarker != null)
                        gpsMarker.remove();

                    Location location = locationResult.getLastLocation();
                    gpsMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                            .alpha(0.8f)
                            .title("Current Location"));
                }
            }
        };
    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> lastLocation = fusedLocationClient.getLastLocation();

        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null && mMap != null)
                {
                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(getString(R.string.last_known_loc_msg)));
                }
            }
        });

        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
    }

    private void stopLocationUpdates()
    {
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void zoomInClick(View v)
    {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v)
    {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }
    
    @Override
    public void onMapLongClick(LatLng latLng) {
        float distance = 0f;
        if(markerList.size() > 0)
        {
            Marker lastMarker = markerList.get(markerList.size() - 1);
            float [] tmpDis = new float[3];

            Location.distanceBetween(lastMarker.getPosition().latitude, lastMarker.getPosition().longitude,
                    latLng.latitude, latLng.latitude, tmpDis);
            distance = tmpDis[0];

            PolylineOptions rectOptions = new PolylineOptions()
                    .add(lastMarker.getPosition())
                    .add(latLng)
                    .width(10)
                    .color(Color.BLUE);
            mMap.addPolyline(rectOptions);
        }
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
                .alpha(0.8f)
                .title(String.format("Position: (%.2f, %.2f) Distance: %.2f", latLng.latitude, latLng.longitude, distance)));
        markerList.add(marker);
        double pomocnicza = marker.getPosition().latitude;
        double pomocnicza2 = marker.getPosition().longitude;
        //pozycjaMarkera.add(new Pozycje(pomocnicza, pomocnicza2)); //tutaj zdaje się powód dlaczego JSON nie działa
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
         //animation
        TransitionManager.beginDelayedTransition(vgroup);
        visible = !visible;

        if(isonTop == false)
        {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            hide.startAnimation(animation);
            startstop.startAnimation(animation);

            hide.setVisibility(View.VISIBLE);
            startstop.setVisibility(View.VISIBLE);

            isonTop = true;
        }
        return false;
    }

    public void clearMap(View view)
    {
        markerList.removeAll(markerList);
        mMap.clear();
        hideThemAll(vgroup);
    }

    public void hideThemAll(View view)
    {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        hide.startAnimation(animation);
        startstop.startAnimation(animation);
        sensor.startAnimation(animation);

        visible = !visible;
        hide.setVisibility(View.GONE);
        startstop.setVisibility(View.GONE);
        sensor.setVisibility(View.GONE);
        isonTop = false;
    }

    public void startStop(View view)
    {
        if(record == false) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            sensor.startAnimation(animation);
            sensor.setVisibility(View.VISIBLE);
            record = true;
        }
        else
        {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            sensor.startAnimation(animation);
            sensor.setVisibility(View.GONE);
            record = false;
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        stopLocationUpdates();
        if(mSensor != null)
            sensorManager.unregisterListener(this, mSensor);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mSensor !=null)
            sensorManager.registerListener(this, mSensor,100000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       savePointToJson();
    }

    private void savePointToJson() {
        Gson gson = new Gson();
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(POINTS_JSON_FILE,MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            gson.toJson(markerPositions,writer);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreFromJson() {
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;
        try {
            inputStream = openFileInput(POINTS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0,n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type to_restore_type = new TypeToken<List<Pozycje>>(){}.getType();
            List<Pozycje> to_restore = gson.fromJson(readJson,to_restore_type);
            if (to_restore != null) {
                for (int i = 0; i < to_restore.size(); i++ ){
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(to_restore.get(i).latitude,to_restore.get(i).longitude))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
                            .alpha(0.8f)
                            .title(String.format("Position: (%.2f, %.2f", to_restore.get(i).latitude,to_restore.get(i).longitude)));
                    markerPositions.add(new Pozycje(to_restore.get(i).latitude,to_restore.get(i).longitude));
                    markerList.add(marker);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}