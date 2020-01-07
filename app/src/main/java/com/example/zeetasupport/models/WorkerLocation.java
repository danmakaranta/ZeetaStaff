package com.example.zeetasupport.models;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class WorkerLocation {

    private GeoPoint geoPoint;
    private @ServerTimestamp Date timeStamp;
    private User user;



    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public WorkerLocation() {

    }

    @Override
    public String toString() {
        return "WorkerLocation{" +
                "geoPoint=" + geoPoint +
                ", timeStamp='" + timeStamp + '\'' +
                ", user=" + user +
                '}';
    }
}
