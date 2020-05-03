package com.example.zeetasupport;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zeetasupport.data.JobData;
import com.example.zeetasupport.models.PolylineData;
import com.example.zeetasupport.models.User;
import com.example.zeetasupport.models.WorkerLocation;
import com.example.zeetasupport.services.LocationService;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import static com.example.zeetasupport.util.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;
import static com.google.firebase.auth.FirebaseAuth.getInstance;


public class MapActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback, GoogleMap.OnPolylineClickListener {

    private static final String TAG = "MapActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 17f;
    private static final int LOCATION_UPDATE_INTERVAL = 4000;
    private final static long FASTEST_INTERVAL = 10000; /* 2 sec */
    private static final int LOCATION_UPDATE_INTERVAL2 = 10000;
    //lets use Handler and runnable
    private Handler mHandler = new Handler();
    private Runnable mRunnable;

    final String[] employeeName = new String[1];
    final String[] employeeID = new String[1];
    final String[] phoneNum = new String[1];

    Location currentLocation;
    Intent serviceIntent;
    Button tempButton;
    private ArrayList<PolylineData> mPolyLinesData = new ArrayList<>();
    private Marker mSelectedMarker = null;

    private boolean mLocationPermissionGranted = false;
    private GoogleMap mMap;
    GeoFire geoFire;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private WorkerLocation mWorkerLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean markerPinned;
    private ArrayList<Marker> mTripMarkers = new ArrayList<>();
    //widget sections
    private EditText mSearchText;
    //vars
    private FirebaseFirestore mDb;
    TextView connect;
    TextView rating;
    private String staffID; // AUTHENTICATED ID
    private ArrayList<WorkerLocation> mUserLocations = new ArrayList<>();
    private GeoApiContext mGeoApiContext;

    //for a taxi or trycycle driver
    private GeoPoint pickUplocation, destination;
    private long distanceCovered;
    private String customerID, driverphoneNumber, serviceproviderid, driverName;
    private double amountForJourney;
    private boolean acceptedRideRequest;


    private String staffOccupation = "";
    //online status
    private boolean online_status;
    private FusedLocationProviderClient locationProviderClient;

    Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    Ringtone ringtone;

    private String locality = "StateNotFound";

    //listen for a request
    DocumentReference clientRequest;
    DocumentReference acceptanceStatus;
    private Handler handler;
    boolean requestAccepted = false;
    private int connects;
    private int numConnect;


    //for adding a custom marker, in Zeeta's case its a sign of a worker going in the direction
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {

        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setPadding(0, 0, 0, 16);

        if (mLocationPermissionGranted) {
            getDeviceLocation();
            init();
        }

        mMap.setOnPolylineClickListener(this);

    }
    final GeoPoint[] clientGp = {null};
    public LocationManager locationManager;
    public Criteria criteria;

    private void stopListenningForRequest() {
        //not an option for now
        clientRequest.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.w(TAG, "Stop listening and clear any outstanding data.");
            }
        });
        requestAccepted = false;
    }
    public String bestProvider;
    DocumentReference jobData = null;
    private String protemp = null;
    private String uID;
    private DatabaseReference ref = null;
    private @ServerTimestamp
    Timestamp timeStamp;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        if (isInternetConnection()) {
            new CountDownTimer(1000, 4000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // do nothing 4s
                    getDeviceLocation();
                    staffID = getInstance().getUid();
                    numConnect = getConnect();

                }

                @Override
                public void onFinish() {
                    // do something end times 1s
                }

            }.start();

        } else {
            Toast.makeText(this, "Please check that you are connected to the internet!", Toast.LENGTH_SHORT).show();
        }


        connect = findViewById(R.id.connect_view);
        online_status = false;


        tempButton = findViewById(R.id.change_btn);

        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
        handler = new Handler();

        serviceIntent = new Intent(MapActivity.this, LocationService.class);

        mDb = FirebaseFirestore.getInstance();

        ringtone.setStreamType(AudioManager.STREAM_RING);
        protemp = getProfession();


        markerPinned = false;
        if (mGeoApiContext == null) {
            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_maps_api_key))
                    .build();
        }



        //initialize and assign variables for the bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.nav_view);
        //set home icon selected
        bottomNavigationView.setSelectedItemId(R.id.home_button);
        //perform itemselectedlistener
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.home_button:
                        return true;
                    case R.id.jobs_button:
                        startActivity(new Intent(getApplicationContext(), Jobs.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.dashboard_button:
                        startActivity(new Intent(getApplicationContext(), DashBoard.class));
                        overridePendingTransition(0, 0);
                        return true;
                }
                return false;
            }
        });

        tempButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

                if (isInternetConnection()) {
                    if (online_status) {
                        online_status = false;
                        tempButton.setText("Go online");
                        tempButton.setBackgroundColor(R.drawable.custom_button);
                        tempButton.invalidate();
                        connect.setVisibility(View.VISIBLE);
                        connect.setText("Connects: " + numConnect);
                        //stopLocationUpdates();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                            if (Looper.myLooper().isCurrentThread() || Looper.getMainLooper().isCurrentThread()) {
                                stopService(serviceIntent);
                                //stopLocationUpdates();
                                Toast.makeText(MapActivity.this, "You are now offline and will not be able to get orders", Toast.LENGTH_SHORT).show();
                                deleteOnlinePresence(FirebaseAuth.getInstance().getUid());
                                numConnect = getConnect();
                                tempButton.setBackgroundColor(R.drawable.custom_button);
                                connect.findViewById(R.id.connect_view);
                                String msg = "Connects: " + numConnect;
                                connect.setText(msg);
                                connect.invalidate();
                                stopListenningForRequest();

                            }
                        }

                    } else {

                        if (numConnect >= 1) {
                            startLocationService();
                            createOnlinePresence();
                            online_status = true;
                            if (getProfession().equalsIgnoreCase("Taxi") || getProfession().equalsIgnoreCase("Trycycle(Keke)")) {
                                listenForRideRequest();
                            } else {
                                listenForJobRequest();
                            }

                            connect.setVisibility(View.GONE);
                            tempButton.setText("Go offline");
                            tempButton.setBackgroundColor(R.drawable.online_custom_button);
                            Toast.makeText(MapActivity.this, "You are now online, your service may be requested", Toast.LENGTH_SHORT).show();
                            tempButton.invalidate();
                        } else {

                            final AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

                            builder.setMessage("You have 0 connects, you need to purchase connect and try again, Are you ready to buy?")
                                    .setCancelable(true)
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                            buyConnect();
                                            dialog.dismiss();
                                        }
                                    })
                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {

                                            dialog.dismiss();
                                        }
                                    });

                            final AlertDialog alert = builder.create();
                            alert.setTitle("Low Connect!");
                            alert.setIcon(R.drawable.zeetaicon);
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please check your internet connection!", Toast.LENGTH_LONG).show();
                }
            }
        });


    }

    private void listenForRideRequest() {

        clientRequest.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {

                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists() && online_status) {
                    documentSnapshot.getReference();

                    boolean accepted = (boolean) documentSnapshot.get("accepted");
                    if (!accepted) {
                        ringtone.play();
                        final AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

                        builder.setMessage("You have an incoming request. Do you want to accept it?")
                                .setCancelable(true)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                        //collect data and accept request
                                        customerID = documentSnapshot.getString("customerID");

                                        pickUplocation = documentSnapshot.getGeoPoint("pickupLocation");
                                        destination = documentSnapshot.getGeoPoint("destination");
                                        distanceCovered = documentSnapshot.getLong("distanceCovered");
                                        amountForJourney = documentSnapshot.getDouble("amount");

                                        acceptRideRequest();
                                        ringtone.stop();
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {

                                        declineRideRequest();
                                        ringtone.stop();
                                        dialog.dismiss();
                                    }
                                });

                        final AlertDialog alert = builder.create();
                        alert.setTitle("Incoming request");
                        alert.setIcon(R.drawable.zeetaicon);

                        alert.show();

                    }
                }

            }
        });

    }

    private void listenForJobRequest() {

        stopListenningForRequest();

        clientRequest.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {

                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists() && online_status) {
                    Log.d(TAG, "Current data: " + documentSnapshot.getData());
                    Log.d(TAG, "A change has been effected on this doc");
                    String change = documentSnapshot.get("accepted").toString();
                    if (change.equalsIgnoreCase("awaiting")) {
                        ringtone.play();
                        final AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

                        builder.setMessage("You have an incoming request. Do you want to accept it?")
                                .setCancelable(true)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                        //do whatever you want down here!!!!
                                        acceptRequest();
                                        ringtone.stop();
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {

                                        declineRequest();
                                        ringtone.stop();
                                        dialog.dismiss();
                                    }
                                });

                        final AlertDialog alert = builder.create();
                        alert.setTitle("Incoming request");
                        alert.setIcon(R.drawable.zeetaicon);

                        alert.show();

                    }

                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }

    private void setRideData() {
        timeStamp = Timestamp.now();
        acceptanceStatus = FirebaseFirestore.getInstance()
                .collection("Customers")
                .document(customerID).collection("RideData").document("ongoing");
        JourneyInfo journeyInfo = new JourneyInfo(pickUplocation, destination, driverName, driverphoneNumber, (long) 0, timeStamp, FirebaseAuth.getInstance().getUid(), (long) amountForJourney, true, false, false);

        acceptanceStatus.set(journeyInfo).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

            }
        });

    }


    private void setJobData() {
        final boolean[] result = new boolean[1];

        acceptanceStatus = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(getInstance().getUid())).collection("Request").document("ongoing");

        acceptanceStatus.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                String ID = task.getResult().get("id").toString();
                clientGp[0] = task.getResult().getGeoPoint("geoPoint");
                employeeID[0] = ID;

                DocumentReference employeeN = FirebaseFirestore.getInstance()
                        .collection("Customers")
                        .document(employeeID[0]);
                employeeN.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        String nameTemp = task.getResult().get("name").toString();
                        phoneNum[0] = task.getResult().get("phoneNumber").toString();
                        employeeName[0] = nameTemp;
                        setJobDataOnCloud();
                        result[0] = true;
                    }
                });

            }
        });
    }

    private void setJobDataOnCloud() {
        jobData = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(getInstance().getUid()).collection("JobData").document(employeeID[0]);
        jobData.set(new JobData(employeeID[0], employeeName[0], phoneNum[0], "Ongoing", (long) 0, null, clientGp[0], (long) 0, false)).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d("setJobData", "Job data set");
            }
        });
        //reset the variables for next request
        jobData = null;
        employeeName[0] = null;
        employeeID[0] = null;
        phoneNum[0] = null;
    }

    private void acceptRideRequest() {
        setRideData();
        acceptanceStatus = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(getInstance().getUid())).collection("RideData").document("ongoing");
        acceptanceStatus.update("accepted", true);

    }

    private void acceptRequest() {
        setJobData();
        acceptanceStatus = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(getInstance().getUid())).collection("Request").document("ongoing");
        acceptanceStatus.update("accepted", true);

    }

    private void declineRideRequest() {
        acceptanceStatus = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(getInstance().getUid())).collection("RideData").document("ongoing");
        acceptanceStatus.update("accepted", false);
    }

    private void declineRequest() {
        acceptanceStatus = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(getInstance().getUid())).collection("Request").document("ongoing");
        acceptanceStatus.update("accepted", "Declined");
    }


    private void deleteOnlinePresence(String id) {
        geoFire.removeLocation(id);
    }

    private void startUserLocationsRunnable() {
        Log.d(TAG, "startUserLocationsRunnable: starting runnable for retrieving updated locations.");
        mHandler.postDelayed(mRunnable = new Runnable() {
            @Override
            public void run() {
                getLocation();
                mHandler.postDelayed(mRunnable, LOCATION_UPDATE_INTERVAL2);
            }
        }, LOCATION_UPDATE_INTERVAL2);
    }


    private void getLocation() {

        // ---------------------------------- LocationRequest ------------------------------------
        // Create the location request to start receiving updates
        LocationRequest mLocationRequestHighAccuracy = new LocationRequest();
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestHighAccuracy.setInterval(LOCATION_UPDATE_INTERVAL2);


        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "getLocation: stopping the location service.");
            stopLocationUpdates();
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
                            Log.d(TAG, "onLocationResult: location of last known not null.");
                        }
                    }
                },
                Looper.myLooper()
        ); // Looper.myLooper tells this to repeat forever until thread is destroyed
    }

    private void buyConnect() {
    }


    private void saveUserLocation(final WorkerLocation userLocation) {

        try {
            DocumentReference locationRef = FirebaseFirestore.getInstance()
                    .collection("AbujaOnline")
                    .document(getInstance().getUid());

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
        }

    }


    private void stopLocationUpdates() {

        mHandler.removeCallbacks(mRunnable);
    }


    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation: called.");


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<android.location.Location>() {
            @Override
            public void onComplete(@NonNull Task<android.location.Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

                    mWorkerLocation.setGeoPoint(geoPoint);
                    Log.d(TAG, "geopoint set.");
                    mWorkerLocation.setTimeStamp(null);
                    //saveWokerLocation();

                }
            }
        });

    }

    private void getWorkerDetails() {

        if (mWorkerLocation == null) {
            mWorkerLocation = new WorkerLocation();
            DocumentReference userRef = mDb.collection("Worker location")
                    .document(getInstance().getUid());

            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: successfully set the user client.");
                        User user = task.getResult().toObject(User.class);
                        mWorkerLocation.setUser(user);
                        ((UserClient) getApplicationContext()).setUser(user);
                        getLastKnownLocation();
                    }
                }
            });
        } else {
            getLastKnownLocation();
        }

    }

    private void getUserLocations() {

        DocumentReference clientL = FirebaseFirestore.getInstance()
                .collection("Client location")
                .document("GCN7ON2GAMuL7JyUY9wX"); // testing with an already dummy location data

        clientL.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "getUserLocationss: successful at accessing the client location.");
                    DocumentSnapshot doc = task.getResult();
                    if (doc != null) {
                        GeoPoint geoPoint = doc.getGeoPoint("location");
                        //check to see if we have a latitude and longitude of the client for the cloud database
                        Log.d(TAG, "Latitude " + geoPoint.getLatitude());
                        Log.d(TAG, "Latitude " + geoPoint.getLongitude());

                       /* MarkerOptions options = new MarkerOptions().position((new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude())));
                       // mMap.addMarker(options.position((new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()))));
*/
                        calculateDirections(geoPoint);


                    } else {
                        Log.d(TAG, "Document is null for location ");
                    }


                }
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "getUserLocationss: unsuccessful at accessing the client location.");
                    }
                });

    }


    private void calculateDirections(GeoPoint gp) {
        Log.d(TAG, "calculateDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                gp.getLatitude(),
                gp.getLongitude()
        );
        Log.d(TAG, "calculateDirections: finished calculating directions.");
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);


        directions.alternatives(true);
        directions.origin(
                new com.google.maps.model.LatLng(
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude()
                )
        );


        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());

                Log.d(TAG, "onResult: successfully retrieved directions.");
                addPolylinesToMap(result);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage());
            }
        });
    }

    private void addPolylinesToMap(final DirectionsResult result) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);
                if (mPolyLinesData.size() > 0) {
                    for (PolylineData polylineData : mPolyLinesData) {
                        polylineData.getPolyline().remove();
                    }
                    mPolyLinesData.clear();
                    mPolyLinesData = new ArrayList<>();
                }

                double duration = 999999999;
                for (DirectionsRoute route : result.routes) {
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    // This loops through all the LatLng coordinates of ONE polyline.
                    for (com.google.maps.model.LatLng latLng : decodedPath) {

//                        Log.d(TAG, "run: latlng: " + latLng.toString());

                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(R.color.blue2);
                    polyline.setClickable(true);
                    mPolyLinesData.add(new PolylineData(polyline, route.legs[0]));

                    // highlight the fastest route and adjust camera
                    double tempDuration = route.legs[0].duration.inSeconds;
                    if (tempDuration < duration) {
                        duration = tempDuration;
                        onPolylineClick(polyline);
                    }


                }
            }
        });
    }




    @Override
    protected void onResume() {
        super.onResume();
        if (isLocationServiceRunning()) {
            tempButton.setText("Go offline");
            tempButton.setBackgroundColor(R.drawable.online_custom_button);
            connect.setVisibility(View.GONE);
            online_status = true;
        }


        if (checkMapServices()) {
            if (mLocationPermissionGranted) {

            } else {
                getLocationPermission();
            }
        }
    }

    private boolean checkMapServices() {
        if (isServicesOk()) {
            if (isMapsEnabled()) {
                return true;
            }
        }
        return false;
    }

    public boolean isMapsEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }


    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    public boolean isServicesOk() {
        Log.d(TAG, "isServicesOk: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MapActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            //everything is ok and the user can make a map request
            Log.d(TAG, "isServicesOk: Google play services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOk: an error occurred but we can fix it");

            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MapActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make map request", Toast.LENGTH_SHORT).show();
        }
        return false;
    }


    private void init() {
        geolocateInit();
    }

    private void geolocateInit() {
        getDeviceLocation();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean isInternetConnection() {

        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting();

    }

    private void moveCamera(LatLng latlng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving camera to current latitude:" + latlng.latitude + " longitude" + latlng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
        //create a marker to drop pin at the location
        MarkerOptions options = new MarkerOptions().position(latlng);
        options.title("You");

        if (markerPinned) {
            mMap.addMarker(options.position(latlng)).setIcon(bitmapDescriptorFromVector(getApplicationContext(),R.drawable.ic_directions_walk_black_24dp));
        } else {
            initMap();
            //mMap.addMarker(options.position(latlng)).setIcon(bitmapDescriptorFromVector(getApplicationContext(),R.drawable.ic_directions_walk_black_24dp));
            mMap.addMarker(options).setIcon(bitmapDescriptorFromVector(getApplicationContext(),R.drawable.ic_directions_walk_black_24dp));
            markerPinned = true;
        }

    }

    private void resetSelectedMarker() {
        if (mSelectedMarker != null) {
            mSelectedMarker.setVisible(true);
            mSelectedMarker = null;
            removeTripMarkers();
        }
    }

    private void removeTripMarkers() {
        for (Marker marker : mTripMarkers) {
            marker.remove();
        }
    }


    private void initMap() {// for initializing the map
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;

                initMap();// if the location permission is granted
            } else {
                Log.d(TAG, "getLocationPermission: Location permission failed");
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSIONS_REQUEST_CODE);
            }

        } else {
            Log.d(TAG, "getLocationPermission: Location permission failed");
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSIONS_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0) {// that means some kind of permission was granted
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = false;
                            Log.d(TAG, "onRequestPermission: permission request failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermission: permission granted");
                    mLocationPermissionGranted = true;
                    //initialize our map
                    initMap();

                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if (mLocationPermissionGranted) {
                    // getWorkerDetails();
                } else {
                    getLocationPermission();
                }
            }
        }

    }


    private void startLocationService() {
        Log.d(TAG, "startLocationService: Start of location service method");

        if (!isLocationServiceRunning()) {
            startService(serviceIntent);

        } else {
            stopService(serviceIntent);// stop location updates from here
        }
    }


    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.example.zeetasupport.services.LocationService".equals(service.service.getClassName())) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running.");
        return false;
    }


    public void createOnlinePresence() {

        for (; protemp.length() < 2; ) {
            protemp = getProfession();
            ref = FirebaseDatabase.getInstance().getReference(locality).child(protemp);
            geoFire = new GeoFire(ref);
        }

        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<android.location.Location>() {
            @Override
            public void onComplete(@NonNull Task<android.location.Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    Log.d(TAG, "about to set location and UID to database");
                    geoFire.setLocation(staffID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {

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

    private String getState() {
        return "";
    }

    private void getDeviceLocation() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();

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
        locationManager.requestLocationUpdates(bestProvider, 10000, 0, this);

        Log.d(TAG, "getDeviceLocation: getting the device current location");


        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionGranted) {// check first to see if the permission is granted
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<android.location.Location>() {
                    @Override
                    public void onComplete(@NonNull Task<android.location.Location> task) {
                        if (task.isSuccessful()) {
                            Location location = task.getResult();
                            currentLocation = location;
                            Log.d(TAG, "about to set location and UID to database");
                            //move camera to current location on map
                            if (currentLocation != null) {
                                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My location");
                            }

                        }
                    }

                });
            }
        } catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: SecurityException:" + e.getMessage());
        }

    }

    public int getConnect() {

        DocumentReference connectref = null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            connectref = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(staffID);
        }

        connectref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    Long connectLong = (Long) doc.get("connects");
                    if (connectLong == null) {
                        Log.d(TAG, "No data found ");
                    } else {
                        String msg = "Connects: " + connectLong.toString();
                        connect.setText(msg);

                        connects = safeLongToInt(connectLong);
                        numConnect = connects;
                        Log.d(TAG, "Number of connects found: " + connects);
                    }
                }
            }

        });
        String message = "Connects: " + connects;

        // connect.setText(message);
        return connects;
    }

    public String getProfession() {

        DocumentReference proffession = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            proffession = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(getInstance().getUid()));
        }


        proffession.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    String aiki = (String) doc.get("profession");
                    locality = (String) doc.get("state");
                    driverphoneNumber = (String) doc.get("phoneNumber");
                    driverName = (String) doc.get("name");
                    if (aiki == null) {
                        Log.d(TAG, "No data found ");
                    } else {
                        Log.d(TAG, aiki);
                        staffOccupation = aiki;
                    }
                }
            }

        });

        if (staffOccupation.equalsIgnoreCase("Taxi") || staffOccupation.equalsIgnoreCase("Tycycle(Keke)")) {
            clientRequest = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).collection("RideData").document("ongoing");

        } else {
            clientRequest = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).collection("Request").document("ongoing");
        }

        return staffOccupation;

    }


    @Override
    public void onPolylineClick(Polyline polyline) {

        int index = 0;
        for (PolylineData polylineData : mPolyLinesData) {
            index++;
            Log.d(TAG, "onPolylineClick: toString: " + polylineData.toString());
            if (polyline.getId().equals(polylineData.getPolyline().getId())) {

                polylineData.getPolyline().setColor(ContextCompat.getColor(getApplicationContext(), R.color.blue2));
                polylineData.getPolyline().setZIndex(1);

                LatLng endLocation = new LatLng(
                        polylineData.getLeg().endLocation.lat,
                        polylineData.getLeg().endLocation.lng
                );

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(endLocation)
                        .title("Route #" + index)
                        .snippet("Duration: " + polylineData.getLeg().duration
                        ));

                mTripMarkers.add(marker);

                marker.showInfoWindow();
            } else {
                polylineData.getPolyline().setColor(R.color.darkGrey);
                polylineData.getPolyline().setZIndex(0);
            }
        }

    }

    @Override
    public void onLocationChanged(Location location) {

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
}
