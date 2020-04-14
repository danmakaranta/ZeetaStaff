package com.example.zeetasupport;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zeetasupport.data.JobsInfo;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class Job_Information extends AppCompatActivity implements OnMapReadyCallback {

    DocumentReference jobsOnCloud;
    private JobsInfo jobData;
    private GeoPoint geoPoint;
    private GoogleMap mMap;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job__information);

        jobData = (JobsInfo) getIntent().getExtras().getParcelable("JobData");
        assert jobData != null;
        Log.d("JobInfor:", "job info testing" + jobData.getName());

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

        TextView clientName = findViewById(R.id.clients_name);
        clientName.setText(jobData.getName());

        TextView clientPhone = findViewById(R.id.clients_phone);
        clientPhone.setText(jobData.getPhoneNumber());

        TextView dateTemp = findViewById(R.id.clients_date);
        Timestamp tp = jobData.getDateRendered();
        Date dt = tp.toDate();
        dateTemp.setText("" + dt);

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
        TextView clsJob = findViewById(R.id.close_job);
        TextView creatInvoice = findViewById(R.id.create_invoice);

        String status = jobData.getStatus();
        if (status.equalsIgnoreCase("Completed")) {
            getDirectionBtn.setEnabled(false);
            minusBtn.setEnabled(false);
            plusBtn.setEnabled(false);
            callBtn.setEnabled(false);
            startJobBtn.setEnabled(false);
            clsJob.setEnabled(false);
            creatInvoice.setEnabled(false);
            hoursWorked.setEnabled(false);
        } else if (status.equalsIgnoreCase("Closed")) {

            startJobBtn.setEnabled(false);
            clsJob.setEnabled(false);
        }

        // set up listeners for the items on screen
        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", jobData.getPhoneNumber(), null));
                if (ActivityCompat.checkSelfPermission(Job_Information.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        clsJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jobsOnCloud.update("status", "Closed").addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(Job_Information.this, "Job Closed. Please proceed to create and send invoice", Toast.LENGTH_LONG).show();
                        startJobBtn.setEnabled(false);
                        clsJob.setEnabled(false);
                    }
                });
            }
        });

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
        return connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting();

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
        Log.d("MoveCamera", "moveCamera: moving camera to current latitude:" + latlng.latitude + " longitude" + latlng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
        //create a marker to drop pin at the location
        MarkerOptions options = new MarkerOptions().position(latlng);
        options.title(jobData.getName());

        mMap.addMarker(options);
        initMap();
        //mMap.addMarker(options.position(latlng)).setIcon(bitmapDescriptorFromVector(getApplicationContext(),R.drawable.ic_directions_walk_black_24dp));
        mMap.addMarker(options);

    }

}
