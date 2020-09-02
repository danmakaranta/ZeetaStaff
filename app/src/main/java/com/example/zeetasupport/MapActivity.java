package com.example.zeetasupport;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zeetasupport.data.GeneralJobData;
import com.example.zeetasupport.models.PolylineData;
import com.example.zeetasupport.models.User;
import com.example.zeetasupport.models.WorkerLocation;
import com.example.zeetasupport.services.LocationService;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
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
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import static com.example.zeetasupport.util.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;
import static com.google.firebase.auth.FirebaseAuth.getInstance;


public class MapActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<GeneralJobData>, LocationListener, OnMapReadyCallback, GoogleMap.OnPolylineClickListener {

    private static final String TAG = "MapActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 17f;
    final String[] employeeName = new String[1];
    final String[] employeeID = new String[1];
    final String[] phoneNum = new String[1];
    final GeoPoint[] clientGp = {null};
    public LocationManager locationManager;
    public Criteria criteria;
    public String bestProvider;
    Location currentLocation;
    Intent serviceIntent;
    Button tempButton;
    GeoFire geoFire;
    TextView connect;
    TextView rating;
    Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    Ringtone ringtone;
    //listen for a request
    DocumentReference clientRequest;
    DocumentReference pushOffline;
    DocumentReference acceptanceStatus;
    boolean requestAccepted = false;
    DocumentReference jobData = null;
    AlertDialog.Builder incomingRequestDialog;
    AlertDialog alertForRequest;
    //lets use Handler and runnable
    private Handler mHandler = new Handler();
    private Handler locationHandler = new Handler();
    private Handler locationHandlerForGeoFire;
    private Runnable locationRunnableForGeoFire;
    private ArrayList<PolylineData> mPolyLinesData = new ArrayList<>();
    private Marker mSelectedMarker = null;
    private boolean mLocationPermissionGranted = false;
    private GoogleMap mMap;
    private WorkerLocation mWorkerLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean markerPinned;
    private ArrayList<Marker> mTripMarkers = new ArrayList<>();
    //widget sections
    private EditText mSearchText;
    //vars
    private FirebaseFirestore mDb;
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
    private String locality = "StateNotFound";
    private boolean engaged = false;
    private Handler handler;
    private int connects;
    private int numConnect;
    private String protemp = "";
    private int connectRate;
    private DatabaseReference ref = null;
    private @ServerTimestamp
    Timestamp timeStamp;
    private JourneyInfo rideData;
    private LoaderManager loaderManager;
    private DocumentReference serviceProviderData;
    private boolean backFromARide = false;
    private boolean incomingRequest;
    private ProgressDialog loadingProgressDialog;
    private ProgressDialog initializationProgressDialog;
    private ProgressDialog redirectingProgDialog;
    private boolean locationCallbackPresent = false;
    private boolean callbackPresent = false;
    private Runnable mRunnable;
    private Runnable locationRunnable;
    private ProgressBar connectP;
    private boolean mStoragePermissionGranted;
    private Bundle stateMachine;
    private double waletBalance;
    private TextView walletOptionTxt;
    private ImageView walletIcon;
    private Dialog connectDialog;
    private int minPurchaseValue;
    private int temp;
    private int selectedNumberOfConnects;
    private ProgressDialog transactionProgressDialog;
    private String serviceProviderRating;
    private Boolean suspended = false;
    private String suspensionMessage;
    private String serviceProviderPhone;
    private String serviceProviderName;
    private MarkerOptions options;
    private boolean isMarkerRotating = false;

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    //for adding a custom marker, in Zeeta's case its a sign of a worker going in the direction
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {

        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        initializationProgressDialog.show();
        Log.d(TAG, "onMapReady: map is ready here");
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);


        mMap = googleMap;
        if (mLocationPermissionGranted) {
            //new getDeviceLocationAsync().execute();
        }

        mMap.setOnPolylineClickListener(this);
    }

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

    private TextView walletBalancetxt;


    private void listenForJobRequest() {
        stopListenningForRequest();

        clientRequest.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {

                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                String acceptedAlready;
                assert documentSnapshot != null;
                if (documentSnapshot.exists()) {
                    acceptedAlready = documentSnapshot.getString("accepted");
                } else {
                    acceptedAlready = "";
                }
                try {// nothing more but to slow down execution a bit to get results before proceeding
                    Thread.sleep(2000);
                } catch (InterruptedException excp) {
                    excp.printStackTrace();
                }

                if (documentSnapshot.exists() && online_status && acceptedAlready.equalsIgnoreCase("Awaiting")) {
                    Log.d(TAG, "Current data: " + documentSnapshot.getData());
                    Log.d(TAG, "A change has been effected on this doc");
                    String change = documentSnapshot.get("accepted").toString();
                    boolean canceledRequest = documentSnapshot.getBoolean("cancelRide");

                    if (change.equalsIgnoreCase("awaiting")) {
                        incomingRequest = true;
                        ringtone.play();
                        incomingRequestDialog = new AlertDialog.Builder(MapActivity.this);
                        incomingRequestDialog.setMessage("You have an incoming request. Do you want to accept it?")
                                .setCancelable(true)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                        //do whatever you want down here!!!!
                                        dialog.dismiss();
                                        acceptRequest();
                                        ringtone.stop();
                                        incomingRequest = false;
                                        alertForRequest.dismiss();
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                        incomingRequest = false;
                                        alertForRequest.dismiss();
                                        declineRequest();
                                        ringtone.stop();
                                        dialog.dismiss();

                                    }
                                });

                        alertForRequest = incomingRequestDialog.create();
                        alertForRequest.setTitle("Incoming request");
                        alertForRequest.setIcon(R.drawable.zeetaicon);

                        /*if (alertForRequest.isShowing()) {
                            alertForRequest.dismiss();
                        }*/
                        alertForRequest.show();

                        if (canceledRequest) {
                            incomingRequest = false;
                            alertForRequest.dismiss();
                            clientRequest.delete();
                            ringtone.stop();
                        }

                    }

                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }

    /*private void setRideData() {
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

    }*/


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
                        String nameTemp = task.getResult().get("username").toString();
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
                .document(FirebaseAuth.getInstance().getUid()).collection("JobData").document(employeeID[0]);

        //reset the variables for next request
        jobData = null;
        employeeName[0] = null;
        employeeID[0] = null;
        phoneNum[0] = null;
    }

    private void acceptRequest() {
        initializationProgressDialog.show();

        acceptanceStatus = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(getInstance().getUid())).collection("Request").document("ongoing");
        DocumentReference updateConnect = FirebaseFirestore.getInstance()
                .collection("Users").document(FirebaseAuth.getInstance().getUid());
        updateConnect.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        long connectLong = (long) doc.getLong("connects");
                        long newValue = connectLong - 1;
                        String msg = "Connects: " + newValue;
                        connect.setText(msg);
                        connects = safeLongToInt(connectLong);
                        numConnect = connects;
                        updateConnect.update("connects", newValue).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    acceptanceStatus.update("accepted", "Accepted").addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            serviceProviderData.update("engaged", true).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    //deleteOnlinePresence();
                                                    if (protemp.equalsIgnoreCase("taxi") || protemp.equalsIgnoreCase("Trycycle(Keke)")) {
                                                        startJourneyLoader();
                                                    } else {
                                                        new CountDownTimer(1000, 3000) {
                                                            @Override
                                                            public void onTick(long millisUntilFinished) {

                                                            }

                                                            @Override
                                                            public void onFinish() {
                                                                initializationProgressDialog.dismiss();
                                                                startActivity(new Intent(MapActivity.this, Jobs.class));
                                                            }
                                                        }.start();
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            }
        });

    }

    private void declineRequest() {
        acceptanceStatus = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(getInstance().getUid())).collection("Request").document("ongoing");
        acceptanceStatus.update("accepted", "Declined");
    }

    private void deleteOnlinePresence() {
        ref = FirebaseDatabase.getInstance("https://zeeta-6b4c0.firebaseio.com").getReference(locality).child(protemp);
        Log.d("ref", "refff" + ref.toString());
        geoFire = new GeoFire(ref);
        geoFire.removeLocation(FirebaseAuth.getInstance().getUid());
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        connect = findViewById(R.id.connect_view);

        loadingProgressDialog = new ProgressDialog(this);
        loadingProgressDialog.setMessage("Connecting...");
        initializationProgressDialog = new ProgressDialog(this);
        initializationProgressDialog.setMessage("Updating...");

        redirectingProgDialog = new ProgressDialog(this);
        redirectingProgDialog.setMessage("On a ride. Redirecting...");

        transactionProgressDialog = new ProgressDialog(this);
        transactionProgressDialog.setMessage("Please wait...");


        incomingRequest = false;

        connect.setVisibility(View.VISIBLE);

        loaderManager = getLoaderManager();

        if (loaderManager.getLoader(1) != null) {

            loaderManager.initLoader(1, null, MapActivity.this);
        }

        pushOffline = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(getInstance().getUid()));


        clientRequest = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).collection("Request").document("ongoing");

        new CountDownTimer(1000, 2000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (stateMachine != null) {

                    currentLocation.setLatitude(stateMachine.getDouble("latitude"));
                    currentLocation.setLongitude(stateMachine.getDouble("longitude"));
                    protemp = stateMachine.getString("protemp");
                    locality = stateMachine.getString("locality");
                    connects = stateMachine.getInt("connects");
                    moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My location");
                    // updateMarkersRunnable();
                    if (connects >= 1) {
                        tempButton.performClick();
                    }

                } else {

                    new getDeviceLocationAsync2().execute();
                }

            }

            @Override
            public void onFinish() {

            }

        }.start();

        online_status = false;

        tempButton = findViewById(R.id.change_btn);

        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);

        handler = new Handler();

        serviceIntent = new Intent(MapActivity.this, LocationService.class);

        mDb = FirebaseFirestore.getInstance();

        ringtone.setStreamType(AudioManager.STREAM_RING);

        incomingRequestDialog = new AlertDialog.Builder(MapActivity.this);
        alertForRequest = incomingRequestDialog.create();


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
                        Intent jobIntent = new Intent(MapActivity.this, Jobs.class);
                        jobIntent.putExtra("protemp", protemp);
                        jobIntent.putExtra("walletBalance", waletBalance);
                        jobIntent.putExtra("connects", connects);
                        startActivity(jobIntent);
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.dashboard_button:
                        if (protemp.equalsIgnoreCase("fashion designer")) {
                            Intent dashIntent = new Intent(getApplicationContext(), FashionDesignerDashboard.class).putExtra("walletBalance", waletBalance);
                            dashIntent.putExtra("connects", connects);
                            dashIntent.putExtra("rating", serviceProviderRating);
                            startActivity(dashIntent);
                        } else {
                            Intent dashIntent = new Intent(getApplicationContext(), DashBoard.class).putExtra("walletBalance", waletBalance);
                            dashIntent.putExtra("connects", connects);
                            dashIntent.putExtra("rating", serviceProviderRating);
                            startActivity(dashIntent);
                        }
                        //overridePendingTransition(0, 0);
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
                loadingProgressDialog.dismiss();
                if (isInternetConnection()) {
                    if (online_status) {
                        online_status = false;
                        tempButton.setText("Go online");
                        tempButton.invalidate();
                        connect.setVisibility(View.VISIBLE);
                        connect.setText("Connects: " + numConnect);

                        if (Looper.myLooper().isCurrentThread() || Looper.getMainLooper().isCurrentThread()) {
                            stopService(serviceIntent);
                            //stopLocationUpdates();
                            Toast.makeText(MapActivity.this, "You are now offline and will not be able to get orders", Toast.LENGTH_SHORT).show();
                            deleteOnlinePresence();
                            numConnect = getConnect();
                            int colorStatus = ContextCompat.getColor(getApplicationContext(), R.color.White);
                            tempButton.setTextColor(colorStatus);
                            String msg = "Connects: " + numConnect;
                            connect.setText(msg);
                            connect.invalidate();
                            stopListenningForRequest();
                            if (locationHandlerForGeoFire != null) {
                                locationHandlerForGeoFire.removeCallbacks(locationRunnableForGeoFire);
                            }

                        }


                    } else {

                        if (numConnect >= 1) {

                            if (suspended) {
                                showSuspensionMessage(suspensionMessage);
                            } else {
                                loadingProgressDialog.show();
                                startLocationService();
                                listenForPushOffline();
                                createOnlinePresence();
                            }

                        } else {

                            final AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

                            builder.setMessage("You have 0 connects. Do you want to buy connects and try again?")
                                    .setCancelable(true)
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                            connectPurchaseOptions();
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
                            alert.show();
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please check your internet connection!", Toast.LENGTH_LONG).show();
                }
            }
        });


    }

    private void listenForPushOffline() {
        if (pushOffline == null) {
            pushOffline = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(getInstance().getUid()));
        }
        pushOffline.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {

                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                boolean pushOfflineBoolean = documentSnapshot.getBoolean("pushOffline");
                try {// nothing more but to slow down execution a bit to get results before proceeding
                    Thread.sleep(2000);
                } catch (InterruptedException excp) {
                    excp.printStackTrace();
                }

                if (documentSnapshot.exists() && pushOfflineBoolean && isLocationServiceRunning()) {
                    Toast.makeText(MapActivity.this, "You have been pushed offline!", Toast.LENGTH_SHORT).show();
                    tempButton.performClick();
                }
            }

        });


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

    private void buyWithCard() {
        connectDialog.dismiss();
        startActivity(new Intent(getApplicationContext(), CreditCardLayout.class));
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


    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean isInternetConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        assert connectivityManager != null;
        if ((connectivityManager.getActiveNetworkInfo()) != null) {
            return (Objects.requireNonNull(connectivityManager.getActiveNetworkInfo())).isConnected();
        } else {
            return false;
        }

    }

    private void moveCamera(LatLng latlng, float zoom, String title) {

        //create a marker to drop pin at the location
        options = new MarkerOptions().position(latlng);
        options.title("You");

        if (protemp.equalsIgnoreCase("Taxi") || protemp.equalsIgnoreCase("Trycycle(Keke)")) {

            mMap.addMarker(options).setIcon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.newtopdown64));
            //staffMarker.showInfoWindow();

        } else {
            mMap.addMarker(options).setIcon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_directions_walk_black_24dp));
        }
        if (initializationProgressDialog.isShowing()) {
            initializationProgressDialog.dismiss();
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));

        if (suspended) {
            showSuspensionMessage(suspensionMessage);
        }

        Random r = new Random();
        float min = (float) (currentLocation.getLatitude() - 15);
        float max = (float) (currentLocation.getLatitude() + 15);
        float random = min + r.nextFloat() * (max - min);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                options.rotation(70);

                Log.d(TAG, "rotate marker runnable" + options.getRotation());
            }
        }, 2000);

    }


    private void initMap() {// for initializing the map
        Log.d("initMap", "initializing map");
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

    @Override
    protected void onResume() {
        super.onResume();
        incomingRequestDialog = new AlertDialog.Builder(MapActivity.this);
        alertForRequest = incomingRequestDialog.create();
        alertForRequest.setTitle("Incoming request");
        alertForRequest.setIcon(R.drawable.zeetaicon);


        if (isLocationServiceRunning()) {
            tempButton.setText(R.string.online_status);
            int colorStatus = ContextCompat.getColor(getApplicationContext(), R.color.red3);
            tempButton.setTextColor(colorStatus);
            connect.setVisibility(View.GONE);
            online_status = true;

        }
        if (incomingRequest) {
            if (!alertForRequest.isShowing()) {
                alertForRequest.show();
            }            //incomingRequestDialog.show();
        }


        if (checkMapServices()) {
            if (mLocationPermissionGranted) {

            } else {
                getLocationPermission();
            }
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
                    ref = FirebaseDatabase.getInstance("https://zeeta-6b4c0.firebaseio.com").getReference(locality).child(protemp);
                    geoFire = new GeoFire(ref);
                    geoFire.setLocation(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()), new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {

                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null) {
                                Log.d(TAG, "there was an error saving location");
                            } else {
                                online_status = true;
                                listenForJobRequest();
                                connect.setVisibility(View.GONE);
                                loadingProgressDialog.dismiss();
                                tempButton.setText("Go offline");
                                int colorStatus = ContextCompat.getColor(getApplicationContext(), R.color.red3);
                                tempButton.setTextColor(colorStatus);
                                Toast.makeText(MapActivity.this, "You are now online, your service may be requested", Toast.LENGTH_SHORT).show();

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

    private void startLocationService() {

        if (!isLocationServiceRunning()) {
            serviceIntent.putExtra("protemp", protemp);
            startService(serviceIntent);
            locationHandlerForGeoFire = new Handler();
            locationHandlerForGeoFire.postDelayed(locationRunnableForGeoFire = new Runnable() {
                @Override
                public void run() {
                    updateGeolocation();
                    locationHandlerForGeoFire.postDelayed(locationRunnableForGeoFire, 3000);
                }
            }, 3000);

        } else {
            stopService(serviceIntent);// stop location updates from here
        }
    }

    public int getConnect() {
        Log.d(TAG, "getConnect called");

        DocumentReference connectref = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

        }
        connectref = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(getInstance().getUid()));

        connectref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    Long connectLong = (Long) doc.get("connects");
                    serviceProviderRating = doc.getString("rating");
                    serviceProviderPhone = doc.getString("phoneNumber");
                    serviceProviderName = doc.getString("name");
                    if (connectLong == null) {
                        Log.d(TAG, "No data found ");
                    } else {
                        String msg = "Connects: " + connectLong.toString();
                        connect.setText(msg);
                        connects = safeLongToInt(connectLong);
                        numConnect = connects;
                        if (transactionProgressDialog.isShowing()) {
                            transactionProgressDialog.dismiss();
                        }

                    }
                }
            }

        });

        String message = "Connects: " + connects;
        connect.setText(message);

        return connects;
    }

    public String getProfession() {

        serviceProviderData = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(getInstance().getUid()));

        serviceProviderData.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    String aiki = (String) doc.get("profession");
                    Long connectLong = (Long) doc.get("connects");
                    locality = (String) doc.get("state");
                    driverphoneNumber = (String) doc.get("phoneNumber");
                    driverName = (String) doc.get("name");
                    boolean staffEngaged = (boolean) doc.get("engaged");
                    boolean backOnline = (boolean) doc.get("ContinueOnline");
                    staffID = doc.getString("user_Id");
                    connects = safeLongToInt(connectLong);
                    suspended = doc.getBoolean("suspended");
                    try {// nothing more but to slow down execution a bit to get results before proceeding
                        Thread.sleep(2000);
                    } catch (InterruptedException excp) {
                        excp.printStackTrace();
                    }
                    String message = "Connects: " + connects;
                    connect.setText(message);
                    if (suspended) {
                        suspensionMessage = doc.getString("suspensionMessage");
                        loadingProgressDialog.dismiss();

                    } else {
                        if (aiki == null) {
                            Log.d(TAG, "No data found ");
                        } else {
                            Log.d(TAG, aiki);
                            staffOccupation = aiki;
                            //move camera to current location on map
                            protemp = aiki;
                            //moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My location");
                            loadingProgressDialog.dismiss();
                            if (staffEngaged) {
                                redirectingProgDialog.show();

                                if (aiki.equalsIgnoreCase("Taxi") || aiki.equalsIgnoreCase("Trycycle(Keke)")) {
                                    startJourneyLoader();
                                } else {
                                    Toast.makeText(MapActivity.this, "In order for you to get more Job request, You need to complete your current Job!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }
            }

        });

        return staffOccupation;

    }

    private void showSuspensionMessage(String msg) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

        builder.setMessage(msg)
                .setCancelable(true)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog alert = builder.create();
        alert.setTitle("Suspension!");
        alert.setIcon(R.drawable.zeetaicon);
        alert.show();

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


            } else {
                polylineData.getPolyline().setColor(R.color.darkGrey);
                polylineData.getPolyline().setZIndex(0);
            }
        }

    }

    private boolean onAJob() {

        return false;
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

    public void updateConnectRate() {

        DocumentReference connectRateRef = FirebaseFirestore.getInstance()
                .collection("Rate")
                .document("connectRate");
        connectRateRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    long rate = doc.getLong("value");
                    long minValue = doc.getLong("minPurchase");
                    minPurchaseValue = (int) minValue;
                    connectRate = (int) rate;
                    Log.d(TAG, "CONNECT RATE:" + connectRate);
                }

            }
        });

    }

    @Override
    public Loader<GeneralJobData> onCreateLoader(int id, Bundle args) {
        return new RideInformationLoader(this);
    }

    @Override
    public void onLoaderReset(Loader<GeneralJobData> loader) {

    }


    public void startJourneyLoader() {
        loaderManager.initLoader(1, null, this);
    }

    public void clearLoaderManager() {
        loaderManager.destroyLoader(1);
    }

    @Override
    public void onLoadFinished(Loader<GeneralJobData> loader, GeneralJobData data) {

        Intent intent = new Intent(MapActivity.this, RidePage.class).putExtra("RideData", (Parcelable) data);

        intent.putExtra("RideData", data);
        intent.putExtra("servicePLongitude", data.getServiceProviderLocation().getLongitude());
        intent.putExtra("servicePLatitude", data.getServiceProviderLocation().getLatitude());
        intent.putExtra("pickupLongitude", data.getServiceLocation().getLongitude());
        intent.putExtra("pickupLatitude", data.getServiceLocation().getLatitude());
        intent.putExtra("paymentType", data.getPaymentMethod());
        intent.putExtra("destinationLongitude", data.getDestination().getLongitude());
        intent.putExtra("destinationLatitude", data.getDestination().getLatitude());
        intent.putExtra("serviceProviderPhone", serviceProviderPhone);
        intent.putExtra("serviceProviderName", serviceProviderName);
        startActivity(intent);
        //finish();
    }


    private void updateMarkersRunnable() {
        Log.d(TAG, "startUserLocationsRunnable: starting runnable for retrieving updated locations.");
        if (!callbackPresent) {
            locationHandler.postDelayed(locationRunnable = new Runnable() {
                @Override
                public void run() {
                    updateMarker();
                    locationHandler.postDelayed(locationRunnable, 10000);
                }
            }, 10000);
            callbackPresent = true;
        }

    }

    private void updateMarker() {

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
                    currentLocation = task.getResult();
                    moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "Me!");
                }
            }
        });
    }

    private void backFromARide() {

        DocumentReference backFromARideStatus = null;

        backFromARideStatus = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));

        DocumentReference finalBackFromARideStatus = backFromARideStatus;
        backFromARideStatus.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    boolean backfRide = doc.getBoolean("ContinueOnline");
                    Log.d(TAG, "BackOnline called: " + backfRide);
                    try {// nothing more but to slow down execution a bit to get results before proceeding
                        Thread.sleep(1000);
                    } catch (InterruptedException excp) {
                        excp.printStackTrace();
                    }
                    if (backfRide && !isLocationServiceRunning()) {
                        finalBackFromARideStatus.update("ContinueOnline", false).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    locality = doc.getString("state");
                                    protemp = doc.getString("profession");
                                    numConnect = safeLongToInt(doc.getLong("connects"));
                                    connects = numConnect;
                                    ref = FirebaseDatabase.getInstance("https://zeeta-6b4c0.firebaseio.com").getReference(locality).child(protemp);
                                    if (numConnect >= 1) {
                                        tempButton.callOnClick();
                                    }
                                }

                            }
                        });

                    }

                }
            }
        });
    }


    private void connectPurchaseOptions() {
        connectDialog = new Dialog(this);
        connectDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        connectDialog.setTitle("Purchase connect?");
        connectDialog.setContentView(R.layout.connect_purchase_options);
        walletIcon = connectDialog.findViewById(R.id.walletIcon);
        TextView cardOptionTxt = connectDialog.findViewById(R.id.cardOptionTxt);
        walletOptionTxt = connectDialog.findViewById(R.id.walletOptionTxt);
        walletBalancetxt = connectDialog.findViewById(R.id.waletBalance);
        ImageView creditCIcon = connectDialog.findViewById(R.id.creditCIcon);
        walletIcon = connectDialog.findViewById(R.id.walletIcon);
        walletBalancetxt.setEnabled(false);
        walletIcon.setEnabled(false);

        walletOptionTxt.setOnClickListener(v -> buyWithWallet());
        walletIcon.setOnClickListener(v -> buyWithWallet());
        cardOptionTxt.setOnClickListener(v -> buyWithCard());
        creditCIcon.setOnClickListener(v -> buyWithCard());

        connectDialog.show();
        walletBalanceUpdate();

    }

    private void buyWithWallet() {
        connectDialog.dismiss();
        double tempBalance = waletBalance;
        Dialog payWithWaletDialog = new Dialog(MapActivity.this);
        payWithWaletDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        payWithWaletDialog.setContentView(R.layout.connects_amount);
        TextView balanceForWalet = payWithWaletDialog.findViewById(R.id.balanceForAmount);
        balanceForWalet.setText("N" + tempBalance);
        EditText connectsInputET = payWithWaletDialog.findViewById(R.id.connects_input);
        TextView totalPurchased = payWithWaletDialog.findViewById(R.id.total_purchased);
        Button minusConnectBtn = payWithWaletDialog.findViewById(R.id.minus_connects);
        Button addConnectBtn = payWithWaletDialog.findViewById(R.id.add_connects);
        Button walletPayBtn = payWithWaletDialog.findViewById(R.id.payWithWallet);
        Log.d(TAG, "selected number of connects: Init " + selectedNumberOfConnects);


        totalPurchased.setText("N" + (Integer.parseInt(connectsInputET.getText().toString())) * connectRate);


        minusConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNumberOfConnects = minPurchaseValue;
                String tempVal = connectsInputET.getText().toString();
                if (tempVal.length() > 0) {
                    int connectInput = Integer.parseInt(connectsInputET.getText().toString());
                    if (connectInput <= minPurchaseValue) {
                        connectsInputET.setText("" + minPurchaseValue);
                        selectedNumberOfConnects = minPurchaseValue;
                    } else {
                        temp = 0;
                        int val = (Integer.parseInt(connectsInputET.getText().toString())) - 1;
                        selectedNumberOfConnects = val;
                        connectsInputET.setText("" + val);
                        temp = val * connectRate;
                        totalPurchased.setText("N" + temp);
                    }
                } else {
                    Toast.makeText(MapActivity.this, "Number of connects can't be Zero(0)", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "selected number of connects: Minus " + selectedNumberOfConnects);
            }
        });

        addConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNumberOfConnects = minPurchaseValue;

                int connectInput = Integer.parseInt(connectsInputET.getText().toString());
                if (connectInput < minPurchaseValue) {
                    connectsInputET.setText("" + minPurchaseValue);
                    selectedNumberOfConnects = minPurchaseValue;
                } else {
                    temp = 0;
                    int val = (Integer.parseInt(connectsInputET.getText().toString())) + 1;
                    selectedNumberOfConnects = val;
                    connectsInputET.setText("" + val);
                    temp = val * connectRate;
                    totalPurchased.setText("N" + temp);
                }
                Log.d(TAG, "selected number of connects: Plus " + selectedNumberOfConnects);

            }
        });


        walletPayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int connectInput = Integer.parseInt(connectsInputET.getText().toString());
                if (connectInput < minPurchaseValue) {
                    Toast.makeText(MapActivity.this, minPurchaseValue + " is the minimum number of connects you can buy", Toast.LENGTH_LONG).show();
                } else {
                    selectedNumberOfConnects = connectInput;
                    double amountToPurchase = (double) (selectedNumberOfConnects * connectRate);
                    totalPurchased.setText("N" + amountToPurchase);

                    if (amountToPurchase <= waletBalance) {
                        Log.d(TAG, "PASS VALIDATION: the total amount to buy" + amountToPurchase);
                        TransactionData transactionData;
                        Timestamp transacTime = Timestamp.now();
                        String detail = selectedNumberOfConnects + " Connects purchase";
                        transactionProgressDialog.show();
                        transactionData = new TransactionData(detail, FirebaseAuth.getInstance().getUid(), true, (long) amountToPurchase,
                                transacTime, null, "Wallet Debit");
                        DocumentReference newPurchase = FirebaseFirestore.getInstance()
                                .collection("ConnectPurchase").document("newRequest")
                                .collection(FirebaseAuth.getInstance().getUid()).document();
                        DocumentReference walletUpdate = FirebaseFirestore.getInstance()
                                .collection("Users").document(FirebaseAuth.getInstance().getUid())
                                .collection("Wallet").document("ZeetaAccount");
                        DocumentReference transactionsUpdate = FirebaseFirestore.getInstance()
                                .collection("Users").document(FirebaseAuth.getInstance().getUid())
                                .collection("Transactions").document();
                        DocumentReference connectref = FirebaseFirestore.getInstance()
                                .collection("Users")
                                .document(Objects.requireNonNull(getInstance().getUid()));

                        newPurchase.set(transactionData).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                connect.setText("Connects: " + selectedNumberOfConnects);// just for testing purposes
                                waletBalance = waletBalance - amountToPurchase;

                                walletUpdate.update("balance", waletBalance).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        connectref.update("connects", numConnect + selectedNumberOfConnects).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                transactionsUpdate.set(transactionData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            payWithWaletDialog.dismiss();
                                                            getConnect();
                                                        }
                                                    }
                                                });
                                            }
                                        });

                                    }
                                });
                            }
                        });

                    } else {
                        Toast.makeText(MapActivity.this, "Your wallet balance is insuffucient for this purchase", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        connectsInputET.setText("" + minPurchaseValue);
        totalPurchased.setText("N" + minPurchaseValue * connectRate);

        payWithWaletDialog.show();

    }

    private void walletBalanceUpdate() {

        DocumentReference waletref = null;
        waletref = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(FirebaseAuth.getInstance().getUid()).collection("Wallet").document("ZeetaAccount");

        waletref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        Long tempBalance = (Long) doc.getLong("balance");
                        if (tempBalance != null) {
                            waletBalance = tempBalance.doubleValue();
                            int tempPurchaseValue = connectRate * minPurchaseValue;
                            if (walletBalancetxt != null) {
                                walletBalancetxt.setText("N" + waletBalance);
                                if (waletBalance < tempPurchaseValue) {
                                    walletBalancetxt.setEnabled(false);
                                    walletIcon.setEnabled(false);
                                } else {
                                    walletBalancetxt.setEnabled(true);
                                    walletIcon.setEnabled(true);
                                }
                            }

                        }
                    }
                }
            }
        });

    }

    @SuppressLint("StaticFieldLeak")
    public class getDeviceLocationAsync2 extends AsyncTask<String, String, String> {

        public LocationManager mLocationManager;

        @Override
        protected void onPreExecute() {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        @Override
        protected void onPostExecute(String s) {

            staffID = FirebaseAuth.getInstance().getUid();
            //move camera to current location on map
            if (currentLocation != null) {
                loadingProgressDialog.dismiss();
                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My location");
                updateMarkersRunnable();
                if (suspended) {
                    tempButton.setEnabled(false);
                }
            }

        }

        @Override
        protected void onProgressUpdate(String... values) {

        }

        @Override
        protected String doInBackground(String... strings) {
            locationManager = (LocationManager) MapActivity.this.getSystemService(Context.LOCATION_SERVICE);
            criteria = new Criteria();
            bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();
            updateConnectRate();

            Log.d(TAG, "getDeviceLocation: getting the device current location");

            try {
                if (mLocationPermissionGranted) {// check first to see if the permission is granted
                    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                    mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<android.location.Location>() {
                        @Override
                        public void onComplete(@NonNull Task<android.location.Location> task) {
                            if (task.isComplete()) {
                                Location location = task.getResult();
                                currentLocation = location;
                                //protemp = getProfession();
                                staffID = FirebaseAuth.getInstance().getUid();
                                numConnect = getConnect();

                                new CountDownTimer(3000, 1000) {
                                    @Override
                                    public void onTick(long millisUntilFinished) {
                                        protemp = getProfession();
                                        staffID = FirebaseAuth.getInstance().getUid();
                                        numConnect = getConnect();
                                        walletBalanceUpdate();
                                        listenForPushOffline();
                                    }

                                    @Override
                                    public void onFinish() {
                                        //move camera to current location on map
                                        moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My location");
                                        backFromARide();
                                    }
                                }.start();

                            }
                        }
                    });
                }
            } catch (SecurityException e) {
                Log.d(TAG, "getDeviceLocation: SecurityException:" + e.getMessage());
            }

            return null;
        }
    }


    public void updateGeolocation() {

        geoFire = new GeoFire(ref);

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
                    ref = FirebaseDatabase.getInstance("https://zeeta-6b4c0.firebaseio.com").getReference(locality).child(protemp);
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


}
