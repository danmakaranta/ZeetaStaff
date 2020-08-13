package com.example.zeetasupport;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.zeetasupport.adapters.TransactionsAdapter;
import com.example.zeetasupport.data.Card;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class DashBoard extends AppCompatActivity {

    CollectionReference myTransactions = FirebaseFirestore.getInstance()
            .collection("Users")
            .document(FirebaseAuth.getInstance().getUid()).collection("Transactions");
    private double waletBalance;
    private int connects;
    private TextView dashBoardWallet;
    private TextView dashBoardConnect;
    private TextView dashBoardRating;
    private ArrayList<TransactionData> transactionsList = new ArrayList<TransactionData>();
    private String protemp, serviceProviderRating;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);
        protemp = getIntent().getStringExtra("protemp");
        serviceProviderRating = getIntent().getStringExtra("rating");
        waletBalance = getIntent().getDoubleExtra("walletBalance", 0.0);
        connects = getIntent().getIntExtra("connects", 0);
        String myRating = getIntent().getStringExtra("rating");
        dashBoardConnect = findViewById(R.id.dashBoardConnect);
        dashBoardWallet = findViewById(R.id.dashboardWallet);
        dashBoardRating = findViewById(R.id.ratingDashBoard);
        dashBoardWallet.setText("Wallet :N" + waletBalance);
        dashBoardConnect.setText("Connects :" + connects);
        dashBoardRating.setText("Rating :" + myRating);

        ImageView pic = findViewById(R.id.serViceProviderImage);
        Button logout = findViewById(R.id.logout_btn);

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), Signin.class));
                overridePendingTransition(0, 0);
            }
        });

        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("serviceproviderpictures");
        storageReference.child(FirebaseAuth.getInstance().getUid() + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Got the download URL for 'profile pic'
                // Picasso.with(DashBoard.this).load(uri).fit().placeholder(R.drawable.zeetamax).into(pic);
                Picasso.get().load(uri).fit().placeholder(R.drawable.zeetamax).into(pic);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });

        populateTransactionsList();


        //initialize and assign variables for the bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.nav_view);
        //set home icon selected
        bottomNavigationView.setSelectedItemId(R.id.dashboard_button);
        //perform itemselectedlistener
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.dashboard_button:
                        return true;
                    case R.id.jobs_button:
                        if (protemp.equalsIgnoreCase("fashion designer")) {
                            Intent dashIntent = new Intent(getApplicationContext(), Jobs.class).putExtra("walletBalance", waletBalance);
                            dashIntent.putExtra("connects", connects);
                            dashIntent.putExtra("rating", serviceProviderRating);
                            startActivity(dashIntent);
                        } else {
                            Intent dashIntent = new Intent(getApplicationContext(), Jobs.class).putExtra("walletBalance", waletBalance);
                            dashIntent.putExtra("connects", connects);
                            dashIntent.putExtra("rating", serviceProviderRating);
                            startActivity(dashIntent);
                        }
                        return true;
                    case R.id.home_button:
                        startActivity(new Intent(getApplicationContext(), MapActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                }
                return false;
            }
        });

    }


    private void populateTransactionsList() {
        myTransactions.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<DocumentSnapshot> docList = task.getResult().getDocuments();

                    if (docList.size() >= 1) {

                        for (QueryDocumentSnapshot document : task.getResult()) {

                            String details = document.getData().get("detail").toString();
                            Timestamp date = (Timestamp) document.getData().get("date");
                            Long amount = (Long) document.getData().get("amountPaid");
                            String typeOfTransaction = document.getData().get("type").toString();
                            Card card = (Card) document.getData().get("card");
                            assert amount != null;
                            Double amountPaid = amount.doubleValue();
                            String customerID = document.getData().get("customerID").toString();
                            boolean paidArtisan = document.getBoolean("paidArtisan");
                            transactionsList.add(new TransactionData(details, customerID, paidArtisan, amountPaid.longValue(), date, card, typeOfTransaction));
                            ListAdapter transactionsAdapter = new TransactionsAdapter(DashBoard.this, transactionsList, 1);
                            ListView myListView = (ListView) findViewById(R.id.transactions_list);

                            myListView.setAdapter(transactionsAdapter);

                        }

                    } else {

                    }
                }
            }
        });

    }

}
