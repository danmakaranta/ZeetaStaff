package com.example.zeetasupport;

import android.Manifest;
import android.app.AlertDialog;
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
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zeetasupport.data.GeneralJobData;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.Distance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

public class RidePage extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener, LoaderManager.LoaderCallbacks<GeneralJobData> {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final String TAG = "RIDE_PAGE";
    private static float DEFAULT_ZOOM = 15f;
    public LocationManager locationManager;
    public Criteria criteria;
    public String bestProvider;
    int PERMISSION_ALL = 1;
    Location currentLocation;
    Button pickupRiderBtn;
    private String[] PERMISSIONS = {Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE};
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean mLocationPermissionGranted = true;
    private GeoApiContext mGeoApiContext;
    private ArrayList<PolylineData> mPolyLinesData = new ArrayList<>();
    private ArrayList<Marker> mTripMarkers = new ArrayList<>();
    private LoaderManager loaderManager;
    private Button endRide;
    private GeneralJobData journeyInfo;
    private Button notify_rider;
    private TextView wait_timer;
    private TextView distanceCoveredTxt;
    private Button startRideBtn;
    AlertDialog.Builder endRideDialog;
    private Button callRider;
    private Button cancelRideBtn;
    private DocumentReference rideInformation = null;
    private DatabaseReference ref = null;
    private CountDownTimer waitCountDownTimer;
    private boolean timerRunning = false;
    private long timeInMillis = 300000;
    private DocumentReference clientRideRequest;
    TransactionData transactionData;
    TransactionData transactionDataForCustomers;
    private long waitingFee = 0;
    private Button useGoogleMaps;
    private String serviceProviderName;
    private String serviceProviderPhone;
    private ProgressDialog endingRideProgressDialog;
    private ProgressDialog initialLoadingProgressDialog;
    private AlertDialog alertForRequest;
    private GeoPoint pickUpGeoPoint, destinationGeoPoint;
    private long distanceCovered;
    private GeoApiContext mDirectionApi = new GeoApiContext.Builder()
            .apiKey("AIzaSyB9nZYenhhs6M8MEXs4xqBYDmaiPpMP4mQ")
            .build();
    private boolean startedJourney = false;
    private List<MarkerOptions> driverMarker;

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
        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        loaderManager = getLoaderManager();
        driverMarker = new ArrayList<MarkerOptions>();

        endingRideProgressDialog = new ProgressDialog(this);
        endingRideProgressDialog.setMessage("Please wait...");

        initialLoadingProgressDialog = new ProgressDialog(this);
        initialLoadingProgressDialog.setMessage("Loading...");


        if (loaderManager.getLoader(1) != null) {
            loaderManager.initLoader(1, null, RidePage.this);
        }
        pickupRiderBtn = (Button) findViewById(R.id.pick_up_rider);
        callRider = (Button) findViewById(R.id.call_rider);
        startRideBtn = (Button) findViewById(R.id.start_ride);
        startRideBtn.setEnabled(false);
        useGoogleMaps = findViewById(R.id.use_google_maps);
        useGoogleMaps.setVisibility(View.INVISIBLE);
        endRide = findViewById(R.id.end_ride);
        notify_rider = findViewById(R.id.notify_rider);
        cancelRideBtn = findViewById(R.id.cancel_ride);
        cancelRideBtn.setEnabled(false);//until arrived at pickup location
        wait_timer = findViewById(R.id.wait_timer);
        wait_timer.setVisibility(View.INVISIBLE);


        serviceProviderName = getIntent().getStringExtra("serviceProviderName");
        serviceProviderPhone = getIntent().getStringExtra("serviceProviderPhone");

        startJourneyLoader();

        if (mGeoApiContext == null) {
            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_directions_api_key))
                    .build();
        }

        pickupRiderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?saddr=" + currentLocation.getLatitude()
                        + "," + currentLocation.getLongitude() + "&daddr=" + journeyInfo.getServiceLocation().getLatitude() + ","
                        + journeyInfo.getServiceLocation().getLongitude()));
                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        useGoogleMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?saddr=" + currentLocation.getLatitude()
                        + "," + currentLocation.getLongitude() + "&daddr=" + journeyInfo.getDestination().getLatitude() + ","
                        + journeyInfo.getDestination().getLongitude()));
                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        cancelRideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelRide();
            }
        });

        callRider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!callPermissions(RidePage.this, PERMISSIONS)) {

                    ActivityCompat.requestPermissions(RidePage.this, PERMISSIONS, PERMISSION_ALL);
                }
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", journeyInfo.getPhoneNumber(), null));
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        startRideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startRide();
            }
        });

        endRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endRide();
            }
        });

        notify_rider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyRiderOfArrival();
            }
        });

        clientRideRequest = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .collection("Request").document(Objects.requireNonNull("ongoing"));

        if (clientRideRequest != null) {
            clientRideRequest.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                    if (documentSnapshot.exists()) {
                        boolean canceledRide = documentSnapshot.getBoolean("cancelRide");
                        try {// nothing more but to slow down execution a bit to get results before proceeding
                            Thread.sleep(2000);
                        } catch (InterruptedException excp) {
                            excp.printStackTrace();
                        }
                        if (canceledRide) {
                            cancelRide();
                        }
                    }
                }
            });
        } else {
            cancelRide();
        }

    }

    private void endRide() {

        String paymentType = journeyInfo.getPaymentMethod();
        endingRideProgressDialog.show();

        if (paymentType.equalsIgnoreCase("ATM Card")) {
            //do card withdrawal
        } else if (paymentType.equalsIgnoreCase("Cash")) {
            //notify the driver to collect cash
            collectCashFromCustomer();
        } else if (paymentType.equalsIgnoreCase("Wallet")) {
            //withdraw from customers wallet
            withdrawFromCustomersWallet();
        }

    }

    private void collectCashFromCustomer() {

        DocumentReference updateStatus = FirebaseFirestore.getInstance()
                .collection("Users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));

        DocumentReference serviceProviderJobDataOncloud = FirebaseFirestore.getInstance()
                .collection("Users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .collection("RideData").document(journeyInfo.getServiceID());

        DocumentReference customerJobDataOncloud = FirebaseFirestore.getInstance()
                .collection("Customers").document(journeyInfo.getServiceID())
                .collection("JobData").document(FirebaseAuth.getInstance().getUid());


        endRideDialog = new AlertDialog.Builder(RidePage.this);
        endRideDialog.setMessage("Thank you!")
                .setCancelable(true)
                .setPositiveButton(" End Ride ", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        //do whatever you want down here!!!!
                        serviceProviderJobDataOncloud.set(new GeneralJobData(journeyInfo.getServiceLocation(), journeyInfo.getDestination(), null, journeyInfo.getServiceID(),
                                journeyInfo.getPhoneNumber(), journeyInfo.getName(), distanceCovered / 1000, journeyInfo.getAmountPaid() + waitingFee, "Accepted",
                                true, true, journeyInfo.getServiceRendered(), journeyInfo.getTimeStamp(), "Completed", (long) 0, true, false, "Cash")).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                customerJobDataOncloud.set(new GeneralJobData(journeyInfo.getServiceLocation(), journeyInfo.getDestination(), null, FirebaseAuth.getInstance().getUid(),
                                        serviceProviderPhone, serviceProviderName, distanceCovered / 1000, journeyInfo.getAmountPaid() + waitingFee, "Accepted",
                                        true, true, journeyInfo.getServiceRendered(), journeyInfo.getTimeStamp(), "Completed", (long) 0, true, false, "Cash")).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        updateStatus.update("engaged", false).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Intent intent = new Intent(RidePage.this, MapActivity.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        });

                                    }
                                });
                            }
                        });


                    }
                });

        AlertDialog alertForRequest = endRideDialog.create();
        long amountToDisplay = journeyInfo.getAmountPaid() + waitingFee;
        alertForRequest.setTitle("Collect N" + amountToDisplay + " from your rider");
        alertForRequest.setIcon(R.drawable.zeetaicon);
        alertForRequest.show();

    }

    private void withdrawFromCustomersWallet() {

        DocumentReference withdrawalRefCustomer = FirebaseFirestore.getInstance()
                .collection("Customers")
                .document(journeyInfo.getServiceID()).collection("Wallet").document("ZeetaAccount");

        DocumentReference withdrawalRefServiceProvider = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(FirebaseAuth.getInstance().getUid()).collection("Wallet").document("ZeetaAccount");
        DocumentReference updateStatus = FirebaseFirestore.getInstance()
                .collection("Users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));

        DocumentReference serviceProviderJobDataOncloud = FirebaseFirestore.getInstance()
                .collection("Users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .collection("RideData").document(journeyInfo.getServiceID());

        DocumentReference customerJobDataOncloud = FirebaseFirestore.getInstance()
                .collection("Customers").document(journeyInfo.getServiceID())
                .collection("JobData").document(FirebaseAuth.getInstance().getUid());
        DocumentReference transactionsUpdate = FirebaseFirestore.getInstance()
                .collection("Users").document(FirebaseAuth.getInstance().getUid())
                .collection("Transactions").document();

        DocumentReference transactionsUpdateForCustomer = FirebaseFirestore.getInstance()
                .collection("Customers").document(journeyInfo.getServiceID())
                .collection("Transactions").document();

        Timestamp transacTime = Timestamp.now();
        transactionData = new TransactionData("Wallet deposit from customer", journeyInfo.getServiceID(), true, journeyInfo.getAmountPaid(),
                transacTime, null, "Wallet Credit");

        transactionDataForCustomers = new TransactionData("Wallet debit for " + journeyInfo.getServiceRendered(), FirebaseAuth.getInstance().getUid(), true, journeyInfo.getAmountPaid(),
                transacTime, null, "Wallet Debit");

        endRideDialog = new AlertDialog.Builder(RidePage.this);
        endRideDialog.setCancelable(true)
                .setPositiveButton(" End Ride ", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        //do whatever you want down here!!!!
                        endingRideProgressDialog.show();
                        withdrawalRefCustomer.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                long withdrawalAmount = journeyInfo.getAmountPaid();
                                DocumentSnapshot doc = task.getResult();
                                long walletBalance = (Long) doc.getLong("balance");
                                long temp = walletBalance - withdrawalAmount;
                                try {// nothing more but to slow down execution a bit to get results before proceeding
                                    Thread.sleep(2000);
                                } catch (InterruptedException excp) {
                                    excp.printStackTrace();
                                }
                                withdrawalRefCustomer.update("balance", temp).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            withdrawalRefServiceProvider.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    DocumentSnapshot doc = task.getResult();
                                                    long currentBalance = (Long) doc.getLong("balance");
                                                    long deposit = currentBalance + journeyInfo.getAmountPaid();
                                                    withdrawalRefServiceProvider.update("balance", deposit).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                serviceProviderJobDataOncloud.set(new GeneralJobData(journeyInfo.getServiceLocation(), journeyInfo.getDestination(), null, journeyInfo.getServiceID(),
                                                                        journeyInfo.getPhoneNumber(), journeyInfo.getName(), distanceCovered / 1000, journeyInfo.getAmountPaid() + waitingFee, "Accepted",
                                                                        true, true, journeyInfo.getServiceRendered(), journeyInfo.getTimeStamp(), "Completed", (long) 0, true, false, "Wallet")).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if (task.isSuccessful()) {
                                                                            transactionsUpdate.set(transactionData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    transactionsUpdateForCustomer.set(transactionDataForCustomers).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                        @Override
                                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                                            customerJobDataOncloud.set(new GeneralJobData(journeyInfo.getServiceLocation(), journeyInfo.getDestination(), null, FirebaseAuth.getInstance().getUid(),
                                                                                                    serviceProviderPhone, serviceProviderName, distanceCovered / 1000, journeyInfo.getAmountPaid() + waitingFee, "Accepted",
                                                                                                    true, true, journeyInfo.getServiceRendered(), journeyInfo.getTimeStamp(), "Completed", (long) 0, true, false, "Wallet")).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                @Override
                                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                                    if (task.isSuccessful()) {
                                                                                                        updateStatus.update("engaged", false).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                            @Override
                                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                                updateStatus.update("ContinueOnline", true).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                    @Override
                                                                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                                                                        dialog.dismiss();
                                                                                                                        Intent intent = new Intent(RidePage.this, MapActivity.class);
                                                                                                                        startActivity(intent);
                                                                                                                        finish();
                                                                                                                    }
                                                                                                                });
                                                                                                                endingRideProgressDialog.dismiss();
                                                                                                            }
                                                                                                        });
                                                                                                    }

                                                                                                }
                                                                                            });
                                                                                        }
                                                                                    });

                                                                                }
                                                                            });
                                                                        }

                                                                    }
                                                                });
                                                            }
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        });
                    }
                });

        alertForRequest = endRideDialog.create();
        alertForRequest.setTitle("Your wallet will be credit with N" + journeyInfo.getAmountPaid());
        alertForRequest.setIcon(R.drawable.zeetaicon);
        alertForRequest.show();

    }

    private void notifyRiderOfArrival() {
        //removeTripMarkers();
        mMap.clear();
        new getDeviceLocationAsync().execute();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            rideInformation = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).collection("Request").document("ongoing");

        }
        rideInformation.update("arrived", true).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(RidePage.this, "Your rider has been notified", Toast.LENGTH_SHORT).show();
                notify_rider.setEnabled(false);
                startRideBtn.setEnabled(true);
                cancelRideBtn.setEnabled(true);
                pickupRiderBtn.setEnabled(false);
                startTimer();
            }
        });
    }

    private void startTimer() {
        wait_timer.setVisibility(View.VISIBLE);
        waitCountDownTimer = new CountDownTimer(300000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeInMillis = millisUntilFinished;
                updateTime();
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                timeInMillis = 300000;
                waitingFee = 100;
                wait_timer.setText("Waiting fee added!");
                Toast.makeText(RidePage.this, "A waiting fee has been added!", Toast.LENGTH_LONG).show();
            }
        }.start();
        timerRunning = true;
    }

    private void updateTime() {
        int minutes = (int) (timeInMillis / 1000) / 60;
        int seconds = (int) (timeInMillis / 1000) % 60;

        String timeformated = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        wait_timer.setText(timeformated);

    }


    private void stopTimer() {
        if (timerRunning) {
            waitCountDownTimer.cancel();
            timerRunning = false;
            timeInMillis = 300000;
            //payWaitingFee = false;
        }
        wait_timer.setVisibility(View.INVISIBLE);
    }

    private void cancelRide() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            rideInformation = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).collection("Request").document("ongoing");

        }

        DocumentReference updateStatus = FirebaseFirestore.getInstance()
                .collection("Users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));

        DocumentReference serviceProviderJobDataOncloud = FirebaseFirestore.getInstance()
                .collection("Users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .collection("RideData").document(journeyInfo.getServiceID());

        rideInformation.update("cancelRide", true).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    stopTimer();
                    serviceProviderJobDataOncloud.set(new GeneralJobData(journeyInfo.getServiceLocation(), journeyInfo.getDestination(), null, FirebaseAuth.getInstance().getUid(),
                            serviceProviderPhone, serviceProviderName, distanceCovered, journeyInfo.getAmountPaid() + waitingFee, "Accepted",
                            false, false, journeyInfo.getServiceRendered(), journeyInfo.getTimeStamp(), "Canceled", (long) 0, false, true, journeyInfo.getPaymentMethod())).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                updateStatus.update("engaged", false).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        updateStatus.update("continueOnline", true).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                final AlertDialog.Builder builder = new AlertDialog.Builder(RidePage.this);
                                                builder.setMessage("SORRY,the passenger canceled, please get back to receiving new request?")
                                                        .setCancelable(false);
                                                final AlertDialog alert = builder.create();
                                                new CountDownTimer(3000, 1000) {
                                                    @Override
                                                    public void onTick(long millisUntilFinished) {
                                                        alert.show();
                                                    }

                                                    @Override
                                                    public void onFinish() {
                                                        alert.dismiss();
                                                        Intent intent = new Intent(RidePage.this, MapActivity.class);
                                                        startActivity(intent);
                                                        overridePendingTransition(0, 0);
                                                    }
                                                }.start();

                                            }
                                        });
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

    }


    private void startRide() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            rideInformation = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).collection("Request").document("ongoing");

        }

        rideInformation.update("started", true).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    startedJourney = true;
                    stopTimer();
                    new getDeviceLocationAsync().execute();

                    Toast.makeText(RidePage.this, "Journey Started!", Toast.LENGTH_SHORT).show();

                }
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
        initialLoadingProgressDialog.dismiss();
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

                Marker marker;
                if (startedJourney) {
                    marker = mMap.addMarker(new MarkerOptions()
                            .position(endLocation)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.home))
                            .title("Destination")
                            .snippet("Duration: " + polylineData.getLeg().duration + " away"
                            ));
                } else {
                    marker = mMap.addMarker(new MarkerOptions()
                            .position(endLocation)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.home))
                            .title("Zeeta Rider")
                            .snippet("Duration: " + polylineData.getLeg().duration + " away"
                            ));
                }

                mTripMarkers.add(marker);

                marker.showInfoWindow();
            } else {
                polylineData.getPolyline().setColor(R.color.darkGrey);
                polylineData.getPolyline().setZIndex(0);
            }
        }

    }

    @Override
    public Loader<GeneralJobData> onCreateLoader(int id, Bundle args) {
        return new RideInformationLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<GeneralJobData> loader, GeneralJobData data) {
        Log.d("loading", "rideinformation loading.." + data.getServiceID());
        journeyInfo = data;
        pickUpGeoPoint = new GeoPoint(data.getServiceLocation().getLatitude(), data.getServiceLocation().getLongitude());
        destinationGeoPoint = new GeoPoint(data.getDestination().getLatitude(), data.getDestination().getLongitude());
        distanceCovered = (calculateDistance(pickUpGeoPoint, destinationGeoPoint)) / 1000;
        startedJourney = journeyInfo.getStarted();

        new getDeviceLocationAsync().execute();
    }


    @Override
    public void onLoaderReset(Loader<GeneralJobData> loader) {

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
        googleMap.setTrafficEnabled(true);
        initMap();
    }

    public void checkLocationPermission() {
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
        }
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
                        .position(latLng)).setTitle("You");
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                googleMap.getUiSettings().setZoomControlsEnabled(true);

                // Updates the location and zoom of the MapView
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM);
                googleMap.moveCamera(cameraUpdate);
            }
        });
    }

    private long calculateDistance(GeoPoint pickupL, GeoPoint dest) {
        Log.d("check", "check calculatedistance");

        com.google.maps.model.LatLng destinations = new com.google.maps.model.LatLng(
                dest.getLatitude(),
                dest.getLongitude()
        );

        DirectionsApiRequest directions = new DirectionsApiRequest(mDirectionApi);

        directions.alternatives(false);
        directions.origin(
                new com.google.maps.model.LatLng(
                        pickupL.getLatitude(),
                        pickupL.getLongitude()
                )
        );

        final Distance[] distance = new Distance[1];
        final long[] dista = new long[1];

        directions.destination(destinations).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {

                if (result.routes[0].legs[0].distance.inMeters > 5) {
                    distanceCovered = result.routes[0].legs[0].distance.inMeters;

                }
                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
                Log.d(TAG, "calculateDirections: distanceCVREL:" + distanceCovered);
                Log.d(TAG, "calculateDirections: geocodedWayPointzz: " + result.geocodedWaypoints[0].toString());
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e("CalculateDirectionsETA", "calculateDistance: Failed to get distance: " + e.getMessage());
            }
        });
        if (distanceCovered < 1) {
            Log.d(TAG, "calculateDirections: Recursively:");
            return calculateDistance(pickupL, dest);
        } else {
            Log.d(TAG, "calculateDirections: distanceCVRET:" + distanceCovered);
            return distanceCovered;
        }


    }

    /*private void updateMarkersRunnable() {
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
*/
    private void moveCamera(LatLng latlng, float zoom, String title) {

        //create a marker to drop pin at the location
        MarkerOptions options = new MarkerOptions().position(latlng);
        options.title("You");
        mMap.addMarker(options).setIcon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.car64));

        //driverMarker.add(options);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));

    }

    public class getDeviceLocationAsync extends AsyncTask<String, String, String> {


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

            try {
                if (mLocationPermissionGranted) {// check first to see if the permission is granted
                    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                    mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<android.location.Location>() {
                        @Override
                        public void onComplete(@NonNull Task<android.location.Location> task) {


                            if (task.isSuccessful()) {
                                Location location = task.getResult();
                                currentLocation = location;
                                //move camera to current location on map
                                if (journeyInfo.getServiceLocation() != null && !startedJourney) {
                                    showPointerOnMap(currentLocation.getLatitude(), currentLocation.getLongitude());
                                    calculateDirections(journeyInfo.getServiceLocation());

                                } else if (startedJourney && currentLocation != null) {
                                    showPointerOnMap(currentLocation.getLatitude(), currentLocation.getLongitude());
                                    pickupRiderBtn.setEnabled(false);
                                    DEFAULT_ZOOM = 12f;
                                    calculateDirections(journeyInfo.getDestination());
                                    cancelRideBtn.setVisibility(View.INVISIBLE);
                                    startRideBtn.setVisibility(View.INVISIBLE);
                                    useGoogleMaps.setVisibility(View.VISIBLE);
                                    callRider.setEnabled(false);
                                    notify_rider.setVisibility(View.INVISIBLE);
                                    endRide.setVisibility(View.VISIBLE);
                                    wait_timer.setVisibility(View.INVISIBLE);
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
