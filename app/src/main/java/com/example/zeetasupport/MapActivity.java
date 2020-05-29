package com.example.zeetasupport;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
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
import android.widget.Button;
import android.widget.EditText;
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
    private static final int LOCATION_UPDATE_INTERVAL2 = 4000;
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
    DocumentReference acceptanceStatus;
    boolean requestAccepted = false;
    DocumentReference jobData = null;
    //lets use Handler and runnable
    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private ArrayList<PolylineData> mPolyLinesData = new ArrayList<>();
    private Marker mSelectedMarker = null;
    private boolean mLocationPermissionGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
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
    AlertDialog.Builder incomingRequestDialog;
    private Handler handler;
    private int connects;
    private int numConnect;
    private String protemp = "";
    private DatabaseReference ref = null;
    private @ServerTimestamp
    Timestamp timeStamp;
    private JourneyInfo rideData;
    private LoaderManager loaderManager;
    private DocumentReference serviceProviderData;
    AlertDialog alertForRequest;
    private boolean backFromARide = false;
    private boolean incomingRequest;

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

        Log.d(TAG, "onMapReady: map is ready here");
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        mMap = googleMap;
        if (mLocationPermissionGranted) {

            new getDeviceLocationAsync().execute();

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        connect = findViewById(R.id.connect_view);

        incomingRequest = false;

        loaderManager = getLoaderManager();

        if (loaderManager.getLoader(1) != null) {

            loaderManager.initLoader(1, null, MapActivity.this);
        }


        clientRequest = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).collection("Request").document("ongoing");
        if (isInternetConnection()) {

            new CountDownTimer(1000, 3000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    protemp = getProfession();
                }
                @Override
                public void onFinish() {
                    new getDeviceLocationAsync().execute();

                }

            }.start();

        } else {
            Toast.makeText(this, "Please check that you are connected to the internet!", Toast.LENGTH_SHORT).show();
        }

        online_status = false;

        tempButton = findViewById(R.id.change_btn);

        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
        handler = new Handler();

        serviceIntent = new Intent(MapActivity.this, LocationService.class);

        mDb = FirebaseFirestore.getInstance();

        ringtone.setStreamType(AudioManager.STREAM_RING);

        incomingRequestDialog = new AlertDialog.Builder(MapActivity.this);

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
                        tempButton.invalidate();
                        connect.setVisibility(View.VISIBLE);
                        connect.setText("Connects: " + numConnect);
                        //stopLocationUpdates();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

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
                            }
                        }

                    } else {

                        if (numConnect >= 1) {
                            startLocationService();
                            createOnlinePresence();
                            online_status = true;
                            listenForJobRequest();
                            connect.setVisibility(View.GONE);
                            tempButton.setText("Go offline");
                            // tempButton.setBackgroundColor(R.drawable.online_custom_button);
                            int colorStatus = ContextCompat.getColor(getApplicationContext(), R.color.red3);
                            tempButton.setTextColor(colorStatus);
                            Toast.makeText(MapActivity.this, "You are now online, your service may be requested", Toast.LENGTH_SHORT).show();

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
                            alert.show();
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please check your internet connection!", Toast.LENGTH_LONG).show();
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
                        incomingRequest = true;
                        ringtone.play();

                        incomingRequestDialog.setMessage("You have an incoming request. Do you want to accept it?")
                                .setCancelable(true)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                        //do whatever you want down here!!!!
                                        acceptRequest();
                                        ringtone.stop();
                                        dialog.dismiss();
                                        incomingRequest = false;
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {

                                        incomingRequest = false;
                                        declineRequest();
                                        ringtone.stop();
                                        dialog.dismiss();
                                    }
                                });

                        alertForRequest = incomingRequestDialog.create();
                        alertForRequest.setTitle("Incoming request");
                        alertForRequest.setIcon(R.drawable.zeetaicon);
                        alertForRequest.show();
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


    private void buyConnect() {
    }


    private void saveUserLocation(final WorkerLocation userLocation) {

        try {
            DocumentReference locationRef = FirebaseFirestore.getInstance()
                    .collection("AbujaOnline")
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
                        //calculateDirections(geoPoint);


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
            tempButton.setText(R.string.online_status);
            int colorStatus = ContextCompat.getColor(getApplicationContext(), R.color.red3);
            tempButton.setTextColor(colorStatus);
            connect.setVisibility(View.GONE);
            online_status = true;
        }


        if (checkMapServices()) {
            if (mLocationPermissionGranted) {

            } else {
                getLocationPermission();
            }
        }
        if (incomingRequest) {
            if (alertForRequest.isShowing()) {

            } else {
                alertForRequest.show();
            }
            //incomingRequestDialog.show();
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
        mMap.clear();
        //create a marker to drop pin at the location
        MarkerOptions options = new MarkerOptions().position(latlng);
        options.title("You");

        if (protemp.equalsIgnoreCase("Taxi") || protemp.equalsIgnoreCase("Trycycle(Keke)")) {

            mMap.addMarker(options).setIcon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.car64));
            //staffMarker.showInfoWindow();

        } else {
            mMap.addMarker(options).setIcon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_directions_walk_black_24dp));
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));

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
            ref = FirebaseDatabase.getInstance("https://zeeta-6b4c0.firebaseio.com").getReference(locality).child(protemp);
            Log.d("ref", "refff" + ref.toString());
            geoFire = new GeoFire(ref);
            createOnlinePresence();
        }


        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<android.location.Location>() {
            @Override
            public void onComplete(@NonNull Task<android.location.Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    Log.d(TAG, "about to set location and UID to database");
                    if (geoFire == null) {
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
                    } else {
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
            }

        });

    }

    private String getState() {
        return "";
    }


    public int getConnect() {
        Log.d(TAG, "getConnect called");

        DocumentReference connectref = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            connectref = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(getInstance().getUid()));
        }

        if (connectref != null) {
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

                        }
                    }
                }

            });
        }

        String message = "Connects: " + connects;
        connect.setText(message);
        return connects;
    }

    public String getProfession() {

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
                        locality = (String) doc.get("state");
                        driverphoneNumber = (String) doc.get("phoneNumber");
                        driverName = (String) doc.get("name");
                        boolean staffEngaged = (boolean) doc.get("engaged");
                        staffID = doc.getString("user_Id");
                        Long connectLong = (Long) doc.get("connects");
                        connects = safeLongToInt(connectLong);
                        String message = "Connects: " + connects;
                        connect.setText(message);
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
                            if (staffEngaged) {
                                Log.d("engaged", "engaged called " + staffEngaged);
                                if (aiki.equalsIgnoreCase("Taxi") || aiki.equalsIgnoreCase("Trycycle(Keke)")) {
                                    startJourneyLoader();
                                } else {
                                    Toast.makeText(MapActivity.this, "In order for you to get more Job request, You need to complete your current Job!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }

            });
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

    @Override
    public Loader<GeneralJobData> onCreateLoader(int id, Bundle args) {
        return new RideInformaitonLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<GeneralJobData> loader, GeneralJobData data) {

        Intent intent = new Intent(MapActivity.this, RidePage.class).putExtra("RideData", (Parcelable) data);

        startActivity(intent);
        overridePendingTransition(0, 0);

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

    private void backFromARide() {
        Log.d(TAG, "BackOnlinecalled");
        DocumentReference backFromARideStatus = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            backFromARideStatus = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));
        }

        if (backFromARideStatus != null) {
            DocumentReference finalBackFromARideStatus = backFromARideStatus;
            backFromARideStatus.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        boolean backfRide = doc.getBoolean("continueOnline");
                        Log.d(TAG, "BackOnline called: " + backfRide);
                        try {// nothing more but to slow down execution a bit to get results before proceeding
                            Thread.sleep(2000);
                        } catch (InterruptedException excp) {
                            excp.printStackTrace();
                        }
                        if (backfRide) {
                            finalBackFromARideStatus.update("continueOnline", false).addOnCompleteListener(new OnCompleteListener<Void>() {
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
    }

    public class getDeviceLocationAsync extends AsyncTask<String, String, String> {

        public LocationManager mLocationManager;

        @Override
        protected void onPreExecute() {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        }

        @Override
        protected void onPostExecute(String s) {
            protemp = getProfession();
            staffID = FirebaseAuth.getInstance().getUid();

            if (currentLocation != null) {
                // updateMarkersRunnable();

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

            Log.d(TAG, "getDeviceLocation: getting the device current location");

            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapActivity.this);

            try {
                if (mLocationPermissionGranted) {// check first to see if the permission is granted
                    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                    mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<android.location.Location>() {
                        @Override
                        public void onComplete(@NonNull Task<android.location.Location> task) {
                            if (task.isSuccessful()) {
                                Location location = task.getResult();
                                currentLocation = location;
                                protemp = getProfession();
                                staffID = FirebaseAuth.getInstance().getUid();
                                numConnect = getConnect();
                                //move camera to current location on map
                                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My location");

                                backFromARide();
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


}
