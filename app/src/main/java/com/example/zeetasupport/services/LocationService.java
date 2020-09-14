package com.example.zeetasupport.services;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.example.zeetasupport.UserClient;
import com.example.zeetasupport.models.User;
import com.example.zeetasupport.models.WorkerLocation;
import com.firebase.geofire.GeoFire;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import static com.google.firebase.auth.FirebaseAuth.getInstance;


public class LocationService extends Service {

    private static final String TAG = "LocationService";

    private FusedLocationProviderClient mFusedLocationClient;
    private final static long UPDATE_INTERVAL = 5000;  /* 3 secs */
    private final static long FASTEST_INTERVAL = 5000; /* 3 sec */
    private String staffLocality = "";
    private GeoFire geoFire;
    private String protemp = "";
    private DatabaseReference ref = null;
    private String staffOccupation;
    private boolean locationCallbackPresent = false;
    private int LOCATION_UPDATE_INTERVAL = 3000;
    private Runnable locationRunnable;
    private Handler locationHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();

        new CountDownTimer(1000, 2000) {
            @Override
            public void onTick(long millisUntilFinished) {
                protemp = getProfession();
            }

            @Override
            public void onFinish() {

            }

        }.start();


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "My Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            //startForeground(1, notification);

        }
        getBaseOfOperation();

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: called.");
        // getLocation();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void getLocation() {

        // ---------------------------------- LocationRequest ------------------------------------
        // Create the location request to start receiving updates
        LocationRequest mLocationRequestHighAccuracy = new LocationRequest();
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestHighAccuracy.setInterval(UPDATE_INTERVAL);
        mLocationRequestHighAccuracy.setFastestInterval(FASTEST_INTERVAL);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "getLocation: stopping the location service.");
            stopSelf();
            return;
        }
        //mFusedLocationClient.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, new MyLocationListener());
        Log.d(TAG, "getLocation: getting location information.");
        mFusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {

                        Log.d(TAG, "onLocationResult: got location result.");

                        Location location = locationResult.getLastLocation();

                        if (location != null) {
                            User user = ((UserClient) (getApplicationContext())).getUser();
                            GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            WorkerLocation userLocation = new WorkerLocation(user, geoPoint, null);
                            saveUserLocation(userLocation);
                        }
                    }
                },
                Looper.myLooper()
        ); // Looper.myLooper tells this to repeat forever until thread is destroyed
    }


    private void saveUserLocation(final WorkerLocation userLocation) {

        if (staffLocality != null && staffLocality.length() <= 0) {
            getBaseOfOperation();

        } else {

            try {
                DocumentReference locationRef = FirebaseFirestore.getInstance()
                        .collection(staffLocality)
                        .document(Objects.requireNonNull(getInstance().getUid()));

                locationRef.set(userLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: \ninserted user location into database." +
                                    "\n latitude: " + userLocation.getGeoPoint().getLatitude() +
                                    "\n longitude: " + userLocation.getGeoPoint().getLongitude());
                        } else {
                            Log.e(TAG, "saveUserLocation: could'nt insert");
                        }
                    }
                });
            } catch (NullPointerException e) {
                Log.e(TAG, "saveUserLocation: User instance is null, stopping location service.");
                Log.e(TAG, "saveUserLocation: NullPointerException: " + e.getMessage());
                stopSelf();
            }
        }

    }


    public void getBaseOfOperation() {

        DocumentReference baseOfOperation = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            baseOfOperation = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(getInstance().getUid()));
        }
        if (baseOfOperation != null) {
            baseOfOperation.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        staffLocality = (String) doc.get("state");
                        protemp = (String) doc.get("profession");
                        getLocation();
                    }
                }

            });
        }

    }


    public String getProfession() {

        DocumentReference serviceProviderData = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            serviceProviderData = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(getInstance().getUid()));
        }
        if (serviceProviderData != null) {
            serviceProviderData.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        String aiki = (String) doc.get("profession");
                        staffLocality = (String) doc.get("state");
                        try {// nothing more but to slow down execution a bit to get results before proceeding
                            Thread.sleep(2000);
                        } catch (InterruptedException excp) {
                            excp.printStackTrace();
                        }
                        if (aiki == null) {
                            Log.d(TAG, "No data found ");
                        } else {
                            Log.d(TAG, aiki);
                            staffOccupation = aiki;

                        }
                    }
                }

            });
        }

        return staffOccupation;

    }


}


