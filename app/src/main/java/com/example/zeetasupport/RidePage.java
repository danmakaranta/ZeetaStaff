package com.example.zeetasupport;

import android.Manifest;
import android.app.AlertDialog;
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
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.zeetasupport.models.PolylineData;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.GeoPoint;
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
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

public class RidePage extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener, LoaderManager.LoaderCallbacks<JourneyInfo> {

    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 1234;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String TAG = "RIDE_PAGE";
    private static final float DEFAULT_ZOOM = 14f;
    public LocationManager locationManager;
    public Criteria criteria;
    public String bestProvider;
    int PERMISSION_ALL = 1;
    String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE};
    private FusedLocationProviderClient mFusedLocationProviderClient;
    Location currentLocation;
    private String[] PERMISSIONS = {Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE};
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean mLocationPermissionGranted = true;
    private GeoApiContext mGeoApiContext;
    private ArrayList<PolylineData> mPolyLinesData = new ArrayList<>();
    private ArrayList<Marker> mTripMarkers = new ArrayList<>();
    private JourneyInfo journeyInfo;
    private LoaderManager loaderManager;

    public static boolean callPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_page);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        loaderManager = getLoaderManager();

        if (loaderManager.getLoader(1) != null) {
            loaderManager.initLoader(1, null, RidePage.this);
        }
        Button getDirectionBtn = (Button) findViewById(R.id.pick_up_rider);
        Button callRider = (Button) findViewById(R.id.call_rider);

        startJourneyLoader();

        // journeyInfo = getIntent().getExtras().getParcelable("RideData");


        if (mGeoApiContext == null) {
            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_directions_api_key))
                    .build();
        }

        getDirectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?saddr=" + currentLocation.getLatitude()
                        + "," + currentLocation.getLongitude() + "&daddr=" + journeyInfo.getPickupLocation().getLatitude() + ","
                        + journeyInfo.getPickupLocation().getLongitude()));
                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        callRider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!callPermissions(RidePage.this, PERMISSIONS)) {
                    Log.d("testing call", "testing calling...");
                    ActivityCompat.requestPermissions(RidePage.this, PERMISSIONS, PERMISSION_ALL);
                }
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", journeyInfo.getCustomerPhoneNumber(), null));
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

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

    private void initMap() {// for initializing the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.rideMap);
        assert mapFragment != null;
        mapFragment.getMapAsync(RidePage.this);
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

    private void removeTripMarkers() {
        for (Marker marker : mTripMarkers) {
            marker.remove();
        }
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
                        .title("Zeeta Rider" + index)
                        .snippet("Duration: " + polylineData.getLeg().duration + " away"
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
    public Loader<JourneyInfo> onCreateLoader(int id, Bundle args) {
        return new RideInformaitonLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<JourneyInfo> loader, JourneyInfo data) {
        Log.d("loading", "rideinformation loading.." + data.getCustomerID());
        Log.d("loading", "rideinformation loading.." + data.getPickupLocation());
        journeyInfo = data;
        new getDeviceLocationAsync().execute();

    }

    @Override
    public void onLoaderReset(Loader<JourneyInfo> loader) {

    }

    public void startJourneyLoader() {
        loaderManager.initLoader(1, null, this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnPolylineClickListener(this);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        initMap();
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Request For Location Permission")
                        .setMessage("This app is requesting for a Location permission. Allow?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(RidePage.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    private void moveCamera(LatLng latlng, float zoom, String title) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
        //create a marker to drop pin at the location
        MarkerOptions options = new MarkerOptions().position(latlng);
        options.title("Driver");
        mMap.addMarker(options);

    }

    private void showPointerOnMap(final double latitude, final double longitude) {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.rideMap);
        assert mapFragment != null;
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                LatLng latLng = new LatLng(latitude, longitude);
                mMap = googleMap;
                googleMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car64))
                        .anchor(0.0f, 1.0f)
                        .position(latLng));
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                googleMap.getUiSettings().setZoomControlsEnabled(true);

                // Updates the location and zoom of the MapView
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                googleMap.moveCamera(cameraUpdate);
            }
        });
    }

    public class getDeviceLocationAsync extends AsyncTask<String, String, String> {


        public double lati = 0.0;
        public double longi = 0.0;

        public LocationManager mLocationManager;

        @Override
        protected void onPreExecute() {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            checkLocationPermission();
        }

        @Override
        protected void onPostExecute(String s) {
            //move camera to current location on map
            if (currentLocation != null) {
                Log.d("ridepage", "ridepage location not null");
                // moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My location");
                showPointerOnMap(currentLocation.getLatitude(), currentLocation.getLongitude());

            }

        }

        @Override
        protected void onProgressUpdate(String... values) {
        }

        @Override
        protected String doInBackground(String... strings) {
            locationManager = (LocationManager) RidePage.this.getSystemService(Context.LOCATION_SERVICE);
            criteria = new Criteria();
            bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();

            Log.d(TAG, "RidePage: getting the device current location 1");


            try {
                if (mLocationPermissionGranted) {// check first to see if the permission is granted
                    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                    mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<android.location.Location>() {
                        @Override
                        public void onComplete(@NonNull Task<android.location.Location> task) {
                            Log.d(TAG, "RidePage: getting the device current location 2");

                            if (task.isSuccessful()) {
                                Location location = task.getResult();
                                currentLocation = location;
                                Log.d(TAG, "RidePage: getting the device current location 3" + location);

                                //move camera to current location on map
                                if (journeyInfo.getPickupLocation() != null) {
                                    // moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My location");
                                    showPointerOnMap(currentLocation.getLatitude(), currentLocation.getLongitude());
                                    Log.d(TAG, "checking pick up: " + journeyInfo.getPickupLocation());
                                    calculateDirections(journeyInfo.getPickupLocation());
                                }

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
