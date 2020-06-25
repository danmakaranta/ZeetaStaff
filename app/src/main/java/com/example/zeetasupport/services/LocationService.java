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
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
    private final static long UPDATE_INTERVAL = 10000;  /* 10 secs */
    private final static long FASTEST_INTERVAL = 10000; /* 10 sec */
    private String staffLocality = "";
    private GeoFire geoFire;
    private String protemp = "";
    private DatabaseReference ref = null;
    private String staffOccupation;
    private boolean locationCallbackPresent = false;
    private int LOCATION_UPDATE_INTERVAL = 10000;
    private Runnable locationRunnable;
    private Handler locationHandler = new Handler();


    @Override
    public void onCreate() {
        super.onCreate();

        new CountDownTimer(1000, 3000) {
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
                            protemp = getProfession();
                            try {// nothing more but to slow down execution a bit to get results before proceeding
                                Thread.sleep(3000);
                            } catch (InterruptedException excp) {
                                excp.printStackTrace();
                            }
                            if (protemp != null && staffLocality != null) {
                                updateGeolocation();
                            } else {
                                Log.d(TAG, "at this point we believe protem is null.");
                                protemp = getProfession();
                                getBaseOfOperation();
                            }

                            Log.d(TAG, "onLocationResult: location of last known not null.");
                        }
                    }
                },
                Looper.myLooper()
        ); // Looper.myLooper tells this to repeat forever until thread is destroyed
    }

    private void locationsRunnable() {
        if (!locationCallbackPresent) {
            locationHandler.postDelayed(locationRunnable = new Runnable() {
                @Override
                public void run() {
                    updateGeolocation();
                    locationHandler.postDelayed(locationRunnable, LOCATION_UPDATE_INTERVAL);
                }
            }, LOCATION_UPDATE_INTERVAL);
            locationCallbackPresent = true;
        }
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
                        getLocation();
                    }
                }

            });
        }

    }

    public void updateGeolocation() {

        for (; protemp.length() < 2; ) {
            protemp = getProfession();
            if (protemp != null) {
                ref = FirebaseDatabase.getInstance("https://zeeta-6b4c0.firebaseio.com").getReference(staffLocality).child(protemp);
            }
            geoFire = new GeoFire(ref);
            updateGeolocation();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<android.location.Location>() {
            @Override
            public void onComplete(@NonNull Task<android.location.Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    Log.d(TAG, "about to set location and UID to database");
                    ref = FirebaseDatabase.getInstance("https://zeeta-6b4c0.firebaseio.com").getReference(staffLocality).child(protemp);
                    geoFire = new GeoFire(ref);

                    geoFire.setLocation(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()), new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {

                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null) {
                                Log.d(TAG, "there was an error saving location");
                            } else {

                                Log.d(TAG, "Location saved successfully");
                            }
                        }
                    });

                }
            }

        });

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


