package com.example.zeetasupport;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class Jobs extends AppCompatActivity {

    final ArrayList<JobsInfo> jobsList = new ArrayList<JobsInfo>();
    final ArrayList<CompletedJobs> completedjobsList = new ArrayList<CompletedJobs>();
    CollectionReference jobsOnCloud = FirebaseFirestore.getInstance()
            .collection("Users")
            .document(FirebaseAuth.getInstance().getUid()).collection("Completed");

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

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

    private void populateJobList() {
        jobsOnCloud.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<DocumentSnapshot> docList = task.getResult().getDocuments();

                    for (QueryDocumentSnapshot document : task.getResult()) {

                        String name = document.getData().get("name").toString();
                        Timestamp date = (Timestamp) document.getData().get("date");
                        Long amount = (Long) document.getData().get("amountPaid");
                        assert amount != null;
                        Double amountPaid = amount.doubleValue();
                        String phoneNumber = Objects.requireNonNull(document.getData().get("phoneNumber")).toString();
                        Log.d("JobsActivity", document.getId() + " => " + name);
                        Log.d("JobsActivity", document.getId() + " => " + date);
                        Log.d("JobsActivity", document.getId() + " => " + amountPaid);
                        Log.d("JobsActivity", document.getId() + " => " + phoneNumber);
                        jobsList.add(new JobsInfo(name, amountPaid, date, phoneNumber));
                        completedjobsList.add(new CompletedJobs(name, amountPaid, date, phoneNumber));
                        ListAdapter myAdapter = new JobAdapter(Jobs.this, jobsList, 1);
                        ListView myListView = (ListView) findViewById(R.id.jobs_completed2);
                        myListView.setAdapter(myAdapter);
                    }
                    if (docList.size() >= 1) {

                    } else {
                        Toast.makeText(Jobs.this, "You do not have previously executed jobs", Toast.LENGTH_LONG).show();
                    }
                }

            }
        });

    }

    public boolean isInternetConnection() {

        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting();
    }

}
