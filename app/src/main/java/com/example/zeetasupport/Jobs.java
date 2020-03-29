package com.example.zeetasupport;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.example.zeetasupport.adapters.JobAdapter;
import com.example.zeetasupport.data.JobsInfo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class Jobs extends AppCompatActivity {

    final ArrayList<JobsInfo> jobsList = new ArrayList<JobsInfo>();
    CollectionReference jobsOnCloud = FirebaseFirestore.getInstance()
            .collection("Users")
            .document(FirebaseAuth.getInstance().getUid()).collection("Completed");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jobs);

        jobsOnCloud.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                List<DocumentSnapshot> docList = task.getResult().getDocuments();
                for (int i = 0; i <= docList.size(); i++) {
                    int amountPaid = (int) docList.get(i).get("amountPaid");
                    jobsList.add(new JobsInfo(docList.get(i).get("name").toString(), amountPaid, docList.get(i).getDate("date"), docList.get(i).get("phoneNumber").toString()));
                }
            }
        });

        if (jobsList != null) {

            ListAdapter myAdapter = new JobAdapter(this, jobsList, R.color.lightGrey);
            ListView myListView = (ListView) findViewById(R.id.jobs_completed);
            myListView.setAdapter(myAdapter);
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

    }
}
