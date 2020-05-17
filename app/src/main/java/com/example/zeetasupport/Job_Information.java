package com.example.zeetasupport;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zeetasupport.data.Invoice;
import com.example.zeetasupport.data.JobsInfo;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.maps.GeoApiContext;

import java.util.Date;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Job_Information extends AppCompatActivity implements OnMapReadyCallback {

    DocumentReference jobsOnCloud;
    private JobsInfo jobData;
    private GeoPoint geoPoint;
    private GoogleMap mMap;
    DocumentReference hourlyRateOnCloud;
    private double hourlyRate;
    private String profession;
    private String status;
    private boolean started;
    private long hoursWorkedLong;
    private double amountPaidDouble;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 1234;
    Location currentLocation;
    String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean mLocationPermissionGranted = false;
    private GeoApiContext mGeoApiContext;
    private ProgressBar mProgressBar;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job__information);
        mProgressBar = findViewById(R.id.progressBar2);
        mProgressBar.setVisibility(View.INVISIBLE);

        jobData = (JobsInfo) getIntent().getExtras().getParcelable("JobData");
        assert jobData != null;
        started = jobData.isStarted();

        jobsOnCloud = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(FirebaseAuth.getInstance().getUid()).collection("JobData").document(jobData.getClientID());
        jobsOnCloud.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                geoPoint = task.getResult().getGeoPoint("gp");
                Log.d("Testing GP:", "testing user geopoints " + geoPoint);
                if (geoPoint != null) {
                    initMap();
                }
            }
        });

        if (mGeoApiContext == null) {
            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_maps_api_key))
                    .build();
        }

        //initialize and assign variables for the bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.nav_view);
        //set home icon selected
        bottomNavigationView.setSelectedItemId(R.id.jobs_button);
        //perform itemselectedlistener
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.jobs_button:
                        startActivity(new Intent(getApplicationContext(), Jobs.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.home_button:
                        startActivity(new Intent(getApplicationContext(), MapActivity.class));
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

        if (isInternetConnection()) {

        } else {
            Toast.makeText(Job_Information.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
        }
        hourlyRateOnCloud = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(FirebaseAuth.getInstance().getUid());
        hourlyRateOnCloud.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    Long hours = (Long) doc.get("hourlyRate");
                    if (hours != null) {
                        hourlyRate = hours.doubleValue();
                    }
                    profession = (String) doc.get("profession");
                }
            }
        });


        TextView clientName = findViewById(R.id.clients_name);
        clientName.setText(jobData.getName());

        TextView clientPhone = findViewById(R.id.clients_phone);
        clientPhone.setText(jobData.getPhoneNumber());

        TextView dateTemp = findViewById(R.id.clients_date);
        Timestamp tp = jobData.getDateRendered();
        if (tp != null) {
            Date dt = tp.toDate();
            dateTemp.setText("" + dt);
        } else {
            dateTemp.setText("");
        }

        TextView hoursWorked = findViewById(R.id.hours_worked);
        String hours = "" + jobData.getHoursWorked();
        Log.d("HoursWorkedTest", "Testing Hours worked " + jobData.getHoursWorked());
        hoursWorked.setText(hours);

        TextView amountPaid = findViewById(R.id.amount_paid_value);
        amountPaid.setText(" N" + jobData.getAmountPaid());

        Button getDirectionBtn = findViewById(R.id.direction_btn);
        Button minusBtn = findViewById(R.id.minus_hour);
        Button plusBtn = findViewById(R.id.add_hour);
        Button callBtn = findViewById(R.id.call_btn);
        Button startJobBtn = findViewById(R.id.start_job);

        startJobBtn.setText("Start Job");
        TextView reportTxt = findViewById(R.id.report_txt);
        TextView clsJob = findViewById(R.id.close_job);
        TextView creatInvoice = findViewById(R.id.create_invoice);

        status = jobData.getStatus();
        if (status.equalsIgnoreCase("Completed")) {
            getDirectionBtn.setEnabled(false);
            minusBtn.setEnabled(false);
            plusBtn.setEnabled(false);
            callBtn.setEnabled(false);
            startJobBtn.setEnabled(false);
            clsJob.setEnabled(false);
            creatInvoice.setEnabled(false);
            hoursWorked.setEnabled(false);
            reportTxt.setEnabled(false);
            clientPhone.setText("");
        } else if (status.equalsIgnoreCase("Closed")) {
            startJobBtn.setEnabled(false);
            clsJob.setEnabled(false);
        }

        if (started) {
            startJobBtn.setEnabled(false);
        } else {
            startJobBtn.setEnabled(true);
        }

        plusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hoursWorkedLong = hoursWorkedLong + 1;
                String val = "" + (int) hoursWorkedLong;
                hoursWorked.setText(val);
            }
        });

        minusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hoursWorkedLong <= 0) {

                } else {
                    hoursWorkedLong = hoursWorkedLong - 1;
                    String val = "" + (int) hoursWorkedLong;
                    hoursWorked.setText(val);
                }
            }
        });

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            getDeviceLocation();
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSIONS_REQUEST_CODE);
        }


        creatInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status.equalsIgnoreCase("Closed")) {

                    if (hoursWorkedLong >= 1) {
                        // custom dialog
                        final Dialog dialog = new Dialog(Job_Information.this);
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        dialog.setContentView(R.layout.invoice);
                        dialog.setTitle("Invoice");
                        TextView textName = (TextView) dialog.findViewById(R.id.invoiceName);
                        textName.setText(jobData.getName());
                        TextView hoursTxt = (TextView) dialog.findViewById(R.id.invoice_hours);
                        hoursTxt.setText(hoursWorked.getText());

                        TextView textProf = (TextView) dialog.findViewById(R.id.job_done);
                        textProf.setText("Service: " + profession);

                        TextView hoursRateTxt = (TextView) dialog.findViewById(R.id.hours_rate);
                        hoursRateTxt.setText("" + hourlyRate);

                        double total = hourlyRate * Double.valueOf(hoursWorked.getText().toString());
                        hoursWorkedLong = Long.parseLong(hoursWorked.getText().toString());
                        amountPaidDouble = total;
                        TextView totalEarned = (TextView) dialog.findViewById(R.id.total_earned);
                        totalEarned.setText("" + total);

                        Button btnYes = (Button) dialog.findViewById(R.id.send_invoice);
                        Button btnNo = (Button) dialog.findViewById(R.id.cancel_invoice);
                        dialog.show();

                        btnYes.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                sendInvoice();
                                dialog.dismiss();
                            }
                        });

                        btnNo.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });


                    } else {
                        Toast.makeText(Job_Information.this, "Hours worked cannot be Zero (0)", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(Job_Information.this, "You need to close the job first before creating an invoice", Toast.LENGTH_LONG).show();

                }


            }
        });

        // set up listeners for the items on screen
        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", jobData.getPhoneNumber(), null));
                if (ActivityCompat.checkSelfPermission(Job_Information.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
        final String[] status1 = {""};

        clsJob.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                jobsOnCloud.update("status", "Closed").addOnCompleteListener(new OnCompleteListener<Void>() {

                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(Job_Information.this, "Job Closed. Please proceed to create and send invoice", Toast.LENGTH_LONG).show();
                        status = "";
                        status = "Closed";
                        startJobBtn.setEnabled(false);
                        clsJob.setEnabled(false);
                        hideDialog();
                    }
                });

            }

        });


        startJobBtn.setOnClickListener(new View.OnClickListener() {
            private @ServerTimestamp
            Timestamp timeStamp = Timestamp.now();

            @Override
            public void onClick(View v) {
                jobsOnCloud.update("timeStamp", timeStamp).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        jobsOnCloud.update("started", true).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(Job_Information.this, "Job Started!", Toast.LENGTH_LONG).show();
                                startJobBtn.setEnabled(false);
                            }
                        });
                    }
                });
            }
        });

        Button directionBtn = findViewById(R.id.direction_btn);
        directionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?saddr=" + currentLocation.getLatitude() + "," + currentLocation.getLongitude() + "&daddr=" + geoPoint.getLatitude() + "," + geoPoint.getLongitude()));
                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                startActivity(intent);
            }
        });

    }

    private void sendInvoice() {
        showDialog();
        DocumentReference spInvoice = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(FirebaseAuth.getInstance().getUid()).collection("Invoice").document(jobData.getClientID());

        DocumentReference customersInvoice = FirebaseFirestore.getInstance()
                .collection("Customers")
                .document(jobData.getClientID()).collection("Invoice").document(FirebaseAuth.getInstance().getUid());

        Invoice invoice = new Invoice(jobData.getClientID(), profession, hoursWorkedLong, amountPaidDouble, false);

        spInvoice.set(invoice).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                customersInvoice.set(invoice).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(Job_Information.this, "Invoice Sent to Client", Toast.LENGTH_LONG).show();
                        hideDialog();
                    }
                });

            }
        });

        spInvoice.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {

            }
        });

    }

    public void showDialog() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog() {
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(Job_Information.this, Jobs.class));
        overridePendingTransition(0, 0);
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

    private void initMap() {// for initializing the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.JobmapView);
        mapFragment.getMapAsync(Job_Information.this);

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        moveCamera(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()), 14, jobData.getName());
    }

    private void moveCamera(LatLng latlng, float zoom, String title) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
        //create a marker to drop pin at the location
        MarkerOptions options = new MarkerOptions().position(latlng);
        options.title(jobData.getName());
        mMap.addMarker(options);
        initMap();
        //mMap.addMarker(options);
    }


    private void getDeviceLocation() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            if (mLocationPermissionGranted) {// check first to see if the permission is granted
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<android.location.Location>() {
                    @Override
                    public void onComplete(@NonNull Task<android.location.Location> task) {
                        if (task.isSuccessful()) {
                            Location location = task.getResult();
                            currentLocation = location;
                            Log.d("StartingPoint", String.valueOf(currentLocation));
                        }
                    }

                });
            }
        } catch (SecurityException e) {
            Log.d("getDeviceLocaitonExcp", "getDeviceLocation: SecurityException:" + e.getMessage());
        }

    }


}
