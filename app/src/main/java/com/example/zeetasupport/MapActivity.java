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
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import static com.example.zeetasupport.util.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;


public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener {

    private static final String TAG = "MapActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 17f;
    private static final int LOCATION_UPDATE_INTERVAL = 4000;
    private final static long FASTEST_INTERVAL = 2000; /* 2 sec */
    private static final int LOCATION_UPDATE_INTERVAL2 = 10000;
    //lets use Handler and runnable
    private Handler mHandler = new Handler();
    private Runnable mRunnable;

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
    private ClientLocation mClientPosition;
    private LatLngBounds mMapBoundary;
    private ArrayList<WorkerLocation> mUserLocations = new ArrayList<>();
    private GeoApiContext mGeoApiContext;

    private String staffOccupation = "";
    //online status
    private boolean online_status;
    private FusedLocationProviderClient locationProviderClient;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: map is ready here");
        //Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        mMap = googleMap;

        if (mLocationPermissionGranted) {
            //getDeviceLocation();
          /*  mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);// remove the set location button from the screen*/
            init();
        }

        mMap.setOnPolylineClickListener(this);

    }

    //for adding a custom marker, in Zeeta's case its a sign of a worker going in the direction
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId){

        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0,0,vectorDrawable.getIntrinsicWidth(),vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),vectorDrawable.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getProfession();
        staffID = FirebaseAuth.getInstance().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getProfession()).child("ONLINE");

        geoFire = new GeoFire(ref);

        online_status = false;
        tempButton = findViewById(R.id.change_btn);
        connect = findViewById(R.id.connect_view);

        serviceIntent = new Intent(MapActivity.this, LocationService.class);



        mDb = FirebaseFirestore.getInstance();

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
                        //getUserLocations();// used it to test if the directions method: getuserlocations actually worked.
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
            @Override
            public void onClick(View v) {
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

                if (online_status) {
                    online_status = false;
                    tempButton.setText("Go online");
                    tempButton.setBackgroundColor(getResources().getColor(R.color.green1));
                    connect.setVisibility(View.VISIBLE);
                    stopLocationUpdates();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        if (Looper.myLooper().isCurrentThread() || Looper.getMainLooper().isCurrentThread()) {
                            // stopService(serviceIntent);
                            stopLocationUpdates();
                            Toast.makeText(MapActivity.this, "You are now offline and will not be able to get orders", Toast.LENGTH_SHORT).show();
                            deleteOnlinePresence(FirebaseAuth.getInstance().getUid());
                        }
                    }

                } else {
                    Toast.makeText(MapActivity.this, "You are now online, your service may be requested", Toast.LENGTH_SHORT).show();
                    //since user has chosen to be online, track current location
                    startUserLocationsRunnable();
                    createOnlinePresence(getProfession());
                    // startLocationService();
                    online_status = true;
                    connect.setVisibility(View.GONE);
                    tempButton.setText("Go offline");
                    tempButton.setBackgroundColor(getResources().getColor(R.color.red3));
                }

            }
        });

        // init();

    }

    private void deleteOnlinePresence(String id) {
        Log.d(TAG, "deleteOnlinePresence: called.");
        Log.d(TAG, id);
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
        //mLocationRequestHighAccuracy.setFastestInterval(FASTEST_INTERVAL);


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


    private void saveUserLocation(final WorkerLocation userLocation) {

        try {
            DocumentReference locationRef = FirebaseFirestore.getInstance()
                    .collection("AbujaOnline")
                    .document(FirebaseAuth.getInstance().getUid());

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
                    .document(FirebaseAuth.getInstance().getUid());

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
            tempButton.setBackgroundColor(getResources().getColor(R.color.red3));
            connect.setVisibility(View.GONE);
            online_status = true;
        }


        if (checkMapServices()) {
            if (mLocationPermissionGranted) {
                //getWorkerDetails();
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
            Log.d(TAG, "isServicesOk: an error occured but we can fix it");

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

    private void geolocate() {
        String searchString = "hmedix";
        // create a geocoder object
        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.d(TAG, "geolocate: input was wrong");
        }

        if (list.size() > 0) {
            Address address = list.get(0);
            //now move the camera to the location
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM, address.getAddressLine(0));
        }

    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the device current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionGranted) {// check first to see if the permission is granted
                Task<Location> location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: Location found");
                            currentLocation = task.getResult();
                            //move camera to current location on map
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My location");
                        } else {
                            Log.d(TAG, "onComplete: current location null");
                            Toast.makeText(MapActivity.this, "Could not get current location, make sure location is enagbled", Toast.LENGTH_SHORT).show();

                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: SecurityException:" + e.getMessage());
        }

    }

    private void moveCamera(LatLng latlng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving camera to current latitude:" + latlng.latitude + " longitude" + latlng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
        //create a marker to drop pin at the location
        MarkerOptions options = new MarkerOptions().position(latlng);


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

    public void createOnlinePresence(final String servicez) {

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
                                Log.d(TAG, "there was an error saving location to geofire server");
                            } else {
                                Log.d(TAG, "Location saved successfully to geofire server");
                            }
                        }
                    });

                }
            }


        });

    }

    public String getProfession() {


        DocumentReference proffession = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            proffession = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));
        }

        proffession.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    String aiki = (String) doc.get("profession");
                    if (aiki == null) {
                        Log.d(TAG, "No data found ");
                    } else {
                        Log.d(TAG, aiki);
                        staffOccupation = aiki;
                    }
                }
            }

        });
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
}
