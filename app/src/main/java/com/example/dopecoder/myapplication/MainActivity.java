package com.example.dopecoder.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.icu.text.DateFormat;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.socketio.client.IO;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends  AppCompatActivity implements LocationListener {

    private TextView mTextMessage;
    private LocationManager locationManager;
    private String provider;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private boolean MASTER_CONTROL = false;
    private boolean PROXIMITY = false;
    private boolean IS_REGISTERED = false;
    private PreemptionRequest reqObj;
    private TrafficSignal trafficSignal;
    private Double lastKnownLat;
    private Double lastKnownLon;
    private boolean executed = false;
    private boolean ended = false;

    private static String IS_REGISTERED_SAVED_STATE = "IS_REGISTERED_SAVED_STATE";
    private static String MASTER_CONTROL_SAVED_STATE = "MASTER_CONTROL_SAVED_STATE";

    private static String EVENT_ON_AUTHORIZATION = "on_authorization"; //"onAuthorization";
    private static String EVENT_ON_REGISTRATION = "on_registration"; //"onRegistration";
    private static String EVENT_ON_AUTHORIZED = "AUTHORIZED";
    private static String EVENT_ON_CONNECTED = "onConnected";


    private static String EVENT_REGISTER = "register";
    private static String EVENT_AUTHORIZE = "authorize";
    private static String EVENT_LOCATON_CAHANGED = "location_changed";

    private static String API_KEY = "API_KEY";
    private static String API_KEY_VALUE = "9b44dce7ba6862d8576a40525763a973"; //You can add you own Api keys to the server and update here.


    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://192.168.1.119:3033"); //Need to change the ip to a local server or a remote server or a hosted domain
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private Emitter.Listener onConnected = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //JSONObject data = (JSONObject) args[0];
                    Log.i(this.getClass().getSimpleName().toString(), "CONNECTED");
                }
            });
        }
    };

    private Emitter.Listener onAuthorization = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String key;
                    try{
                        key = data.getString("key");
                        String currentDateTimeString = java.text.DateFormat.getDateTimeInstance().format(new Date());
                        reqObj = new PreemptionRequest(getMd5Hash(currentDateTimeString), "0", key, new Loc(0, 0));
                        Gson gson = new Gson();
                        String json = gson.toJson(reqObj);
                        mSocket.emit(EVENT_REGISTER, json);
                    }catch (JSONException e){

                    }

                }
            });
        }
    };

    private Emitter.Listener onRegistration = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    boolean status;
                    try{
                        status = data.getBoolean("status");
                        if(status){
                            IS_REGISTERED = true;
                            MASTER_CONTROL = true;
                        }
                    }catch (JSONException e){
                        Log.e("onRegistration", "JSON error.");
                        e.printStackTrace();
                    }

                }
            });
        }
    };

    private Emitter.Listener onRequestCompletion = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //update View
                    String instruction = "Your request is being served, please follow the traffic";
                    updateOnRequestCompletion(R.color.colorAccentTertiary, instruction);
                    //ProgressBar bar = (ProgressBar) findViewById(R.id.loading_progress_xml);
                    //bar.setVisibility(View.VISIBLE);

                    JSONObject data = (JSONObject) args[0];
                    MASTER_CONTROL = false;
                    try {
                        Log.e("DATA", String.valueOf(data));
                        Log.e("DATA", data.getString("_id"));
                        Log.e("DATA", String.valueOf(data.getInt("no_of_points")));
                        Log.e("DATA", String.valueOf(data.getJSONArray("points")));
                        List<Loc> locs = new ArrayList<Loc>();
                        String id =  data.getString("_id");
                        int no_of_points = data.getInt("no_of_points");
                        JSONArray points = (JSONArray) data.getJSONArray("points");
                        for(int i=0; i<no_of_points; i++){
                            Log.e("DATA e", String.valueOf(points.getJSONArray(i)));
                            JSONArray point = points.getJSONArray(i);
                            Log.e("DATA e", String.valueOf(point.get(0)));
                            locs.add(new Loc((double)point.get(0), (double)point.get(1)));
                        }

                        TrafficSignal ts = new TrafficSignal(id, no_of_points, locs);
                        trafficSignal = ts;
                        //Use ts to find proximity
                        PROXIMITY = true;

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //TrafficSignal signal = new TrafficSignal(data.getString("id"), data.getInt("no_of_points"), new Loc(data.getJSONObject("points")))

                    //start here for proximity and firing an event for response completion to the server

                }
            });
        }
    };

    public static String getMd5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String md5 = number.toString(16);

            while (md5.length() < 32)
                md5 = "0" + md5;

            return md5;
        } catch (NoSuchAlgorithmException e) {
            Log.e("MD5", e.getLocalizedMessage());
            return null;
        }
    }

    private int getLeastValueIndex(List<Double> dists) {
        int smallest_index = 0;
        double smallest_value = dists.get(0);
        for(int i = 1; i<dists.size(); i++){
            if (smallest_value > dists.get(i)){
                smallest_index = i;
                smallest_value = dists.get(i);
            }
        }
        return smallest_index;
    }

    private List<Double> getProximity(Location loc){
        List<Double> distances = new ArrayList<Double>();
        for(int i=0; i<trafficSignal.points.size(); i++){

            distances.add(getDistance(lastKnownLat, lastKnownLon, trafficSignal.points.get(i).latitude, trafficSignal.points.get(i).longitude));
        }
        return distances;
    }


    private Double getDistance(Double src_lat, Double src_lon, Double dest_lat, Double dest_lon){
        Double R = 6371e3; // metres
        double φ1 = Math.toRadians(src_lat);
        double φ2 = Math.toRadians(dest_lat);
        double Δφ = Math.toRadians((dest_lat-src_lat));
        double Δλ = Math.toRadians((dest_lon-src_lon));

        double a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
                Math.cos(φ1) * Math.cos(φ2) *
                        Math.sin(Δλ/2) * Math.sin(Δλ/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        double d = R * c;
        return d;
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //Log.e("EXECUTING", "HERE");

            Log.e("EXECUTING", String.valueOf(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)));

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                //Log.e("EXECUTING", "HERE ALSO");
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission. ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission. ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, this);
                        //locationManager.requestLoca
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.d("LOCATION CHANGED", String.valueOf(location));
        lastKnownLat = location.getLatitude();
        lastKnownLon = location.getLongitude();
        if (MASTER_CONTROL){
            Log.d("LOCATION CHANGED", String.valueOf(location));
            try {
                reqObj.direction_data = new Loc(location.getLatitude(), location.getLongitude()); Gson gson = new Gson();
                String json = gson.toJson(reqObj);
                mSocket.emit(EVENT_LOCATON_CAHANGED, json);
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }

        if(PROXIMITY){
            List<Double> dists= getProximity(location);
            int i = getLeastValueIndex(dists);
            double d = getDistance(location.getLatitude(), location.getLongitude(), trafficSignal.points.get(i).latitude, trafficSignal.points.get(i).longitude);
            Log.e("LAT1", String.valueOf(location.getLatitude()));
            Log.e("LAT1", String.valueOf(location.getLongitude()));

            Log.e("LAT2", String.valueOf(trafficSignal.points.get(i).latitude));
            Log.e("LAT2", String.valueOf(trafficSignal.points.get(i).longitude));
            Log.e("D", String.valueOf(d));
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", trafficSignal.id);
                obj.put("side", i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (d < 500){
                if (!executed){
                    //SEND NEAR SIGNAL REQUEST
                    Log.e("HERE", "SEND NEAR SIGNAL REQUEST");
                    executed=true;
                    mSocket.emit("near_signal", obj);
                }
                if(executed && d < 30 && !ended){
                    Log.e("HERE", "SEND NEAR END REQUEST");
                    ended=true;
                    mSocket.emit("end_signal", obj);
                    updateOnRequestCompletion(R.color.colorAccentTertiary, "Your Resquest is served, Thank you");
                    final ProgressBar bar = (ProgressBar) findViewById(R.id.loading_progress_xml);
                    bar.setVisibility(View.INVISIBLE);

                }

            }

        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void updateOnRequestCompletion(int color, String instructons){
        ConstraintLayout view = (ConstraintLayout) this.getWindow().findViewById(R.id.mainLayout);
        view.setBackgroundResource(color);
        TextView textView = (TextView) this.getWindow().findViewById(R.id.instructionView);
        textView.setText(instructons);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 32);

        //view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 32);
        textView.setTextColor(Color.WHITE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final ProgressBar bar = (ProgressBar) findViewById(R.id.loading_progress_xml);
        bar.setVisibility(View.INVISIBLE);

        final TextView view = (TextView) findViewById(R.id.instructionView);
        final Button toggle = (Button) findViewById(R.id.emergencySwitch);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put(API_KEY, API_KEY_VALUE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mSocket.emit(EVENT_AUTHORIZE, obj.toString());
                ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.mainLayout);
                layout.setBackgroundResource(R.color.colorAccentSecondary);
                bar.setVisibility(View.VISIBLE);
                toggle.setVisibility(View.INVISIBLE);
                view.setText("Please wait!");
                view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 32);
                view.setTextColor(Color.WHITE);
            }
        });


        if (savedInstanceState != null && savedInstanceState.getBoolean(IS_REGISTERED_SAVED_STATE)) {
            this.MASTER_CONTROL = true; //savedInstanceState.getBoolean(MASTER_CONTROL_SAVED_STATE);
            mSocket.on(EVENT_ON_AUTHORIZED, onRequestCompletion);
            mSocket.connect();

        }else{
            mSocket.on(EVENT_ON_CONNECTED, onConnected);
            mSocket.on(EVENT_ON_AUTHORIZATION, onAuthorization);
            mSocket.on(EVENT_ON_REGISTRATION, onRegistration);
            mSocket.on(EVENT_ON_AUTHORIZED, onRequestCompletion);
            mSocket.connect();
            Log.i("SOCKET", mSocket.toString());
        }




        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e("LIFECYCLE", "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("LIFECYCLE", "onResume");

        if (checkLocationPermission()) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission. ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                //Request location updates:
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 1, this);
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("LIFECYCLE", "onPause");
    }



    @Override
    protected void onStop() {
        super.onStop();
        Log.e("LIFECYCLE", "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("LIFECYCLE", "onStop");
        mSocket.off(EVENT_ON_CONNECTED, onConnected);
        mSocket.off(EVENT_ON_REGISTRATION, onAuthorization);
        mSocket.off(EVENT_ON_REGISTRATION, onRegistration);
        mSocket.off(EVENT_ON_AUTHORIZED, onRequestCompletion);
        mSocket.disconnect();

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putBoolean(IS_REGISTERED_SAVED_STATE, IS_REGISTERED);
        savedInstanceState.putBoolean(MASTER_CONTROL_SAVED_STATE, MASTER_CONTROL);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
}

