package com.example.zeetasupport;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Objects;

import androidx.annotation.NonNull;

public class RideInformaitonLoader extends AsyncTaskLoader<JourneyInfo> {


    JourneyInfo journeyInfo = null;
    private DocumentReference rideInformation = null;
    private GeoPoint pickupLocation;
    private GeoPoint destination;
    private String customerID;
    private String customerPhoneNumber;
    private Long distanceCovered;
    private Long amount;
    private String accepted;
    private Boolean started;
    private Boolean ended;
    private @ServerTimestamp
    Timestamp timeStamp;

    public RideInformaitonLoader(Context context) {
        super(context);
    }


    public JourneyInfo getRideInformation() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            rideInformation = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).collection("Request").document("ongoing");

        }

        if (rideInformation != null) {
            rideInformation.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        pickupLocation = doc.getGeoPoint("pickupLocation");
                        destination = doc.getGeoPoint("destination");
                        customerID = doc.getString("customerID");
                        distanceCovered = doc.getLong("distanceCovered");
                        customerPhoneNumber = doc.getString("customerPhoneNumber");
                        amount = doc.getLong("amount");
                        started = doc.getBoolean("started");
                        accepted = doc.getString("accepted");
                        ended = doc.getBoolean("ended");
                        timeStamp = doc.getTimestamp("timeStamp");
                        Log.d("data valid", "data:.." + timeStamp);

                        journeyInfo = new JourneyInfo(pickupLocation, destination, customerID, customerPhoneNumber, distanceCovered, timeStamp, (long) amount, accepted, started, ended);


                    }
                }
            });
        }
        if (journeyInfo == null) {
            return getRideInformation();
        } else {
            return journeyInfo;
        }


    }

    @Override
    public JourneyInfo loadInBackground() {
        JourneyInfo journeyInfo = getRideInformation();
        return journeyInfo;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

}
