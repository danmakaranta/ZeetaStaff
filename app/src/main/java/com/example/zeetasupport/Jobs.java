package com.example.zeetasupport;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.zeetasupport.adapters.JobAdapter;
import com.example.zeetasupport.data.CompletedJobs;
import com.example.zeetasupport.data.JobsInfo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class Jobs extends AppCompatActivity {

    final ArrayList<JobsInfo> jobsList = new ArrayList<JobsInfo>();
    final ArrayList<CompletedJobs> completedjobsList = new ArrayList<CompletedJobs>();
    CollectionReference jobsOnCloud = FirebaseFirestore.getInstance()
            .collection("Users")
            .document(FirebaseAuth.getInstance().getUid()).collection("JobData");
    private String status;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jobs);

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
            populateJobList();
        } else {
            Toast.makeText(Jobs.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(Jobs.this, MapActivity.class));
        overridePendingTransition(0, 0);
    }

    private void populateRideList() {

    }

    private void populateJobList() {
        jobsOnCloud.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<DocumentSnapshot> docList = task.getResult().getDocuments();

                    for (QueryDocumentSnapshot document : task.getResult()) {

                        String name = document.getData().get("clientName").toString();
                        Timestamp date = (Timestamp) document.getData().get("timeStamp");
                        Long amount = (Long) document.getData().get("amountPaid");
                        status = document.getData().get("status").toString();
                        assert amount != null;
                        Double amountPaid = amount.doubleValue();
                        String phoneNumber = Objects.requireNonNull(document.getData().get("clientPhone")).toString();
                        Long hoursWorked = (Long) document.getData().get("hoursWorked");
                        GeoPoint gp = (GeoPoint) document.getData().get("gp");
                        String clientID = document.getData().get("clientID").toString();
                        String startedStr = Objects.requireNonNull(document.getData().get("started")).toString();
                        boolean started = Boolean.parseBoolean(startedStr);
                        jobsList.add(new JobsInfo(name, amountPaid, date, phoneNumber, clientID, status, hoursWorked, gp, started));
                        ListAdapter myAdapter = new JobAdapter(Jobs.this, jobsList, 1);
                        ListView myListView = (ListView) findViewById(R.id.jobs_completed2);

                        myListView.setAdapter(myAdapter);

                        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                JobsInfo tempjobsList = (JobsInfo) myListView.getItemAtPosition(position);

                                Intent intent = new Intent(Jobs.this, Job_Information.class).putExtra("JobData", (Parcelable) tempjobsList);
                                startActivity(intent);
                                overridePendingTransition(0, 0);
                            }
                        });
                    }
                    if (docList.size() >= 1) {

                    } else {
                        Toast.makeText(Jobs.this, "You do not have previously executed jobs", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean isInternetConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        return Objects.requireNonNull(connectivityManager.getActiveNetworkInfo()).isConnectedOrConnecting();
    }

}
