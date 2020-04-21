package com.example.zeetasupport.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;


public class JobsInfo implements Parcelable {

    private String name;
    private Double amountPaid;
    private Timestamp dateRendered;
    private String phoneNumber;

    private String clientID;
    private String status;
    private Long hoursWorked;
    private GeoPoint geoPoint;
    public static final Creator<JobsInfo> CREATOR = new Creator<JobsInfo>() {
        @Override
        public JobsInfo createFromParcel(Parcel in) {
            return new JobsInfo(in);
        }

        @Override
        public JobsInfo[] newArray(int size) {
            return new JobsInfo[size];
        }
    };
    private boolean started;

    protected JobsInfo(Parcel in) {
        name = in.readString();
        if (in.readByte() == 0) {
            amountPaid = null;
        } else {
            amountPaid = in.readDouble();
        }
        dateRendered = in.readParcelable(Timestamp.class.getClassLoader());
        phoneNumber = in.readString();
        clientID = in.readString();
        status = in.readString();
        if (in.readByte() == 0) {
            hoursWorked = null;
        } else {
            hoursWorked = in.readLong();
        }
        started = in.readByte() != 0;
    }

    public JobsInfo(String name, Double amountPaid, Timestamp dateRendered, String phoneNumber, String clientID, String status, Long hoursWorked, GeoPoint geoPoint, boolean started) {
        this.name = name;
        this.amountPaid = amountPaid;
        this.dateRendered = dateRendered;
        this.phoneNumber = phoneNumber;
        this.clientID = clientID;
        this.status = status;
        this.hoursWorked = hoursWorked;
        this.geoPoint = geoPoint;
        this.started = started;
    }

    public JobsInfo(String name, Double amountPaid, Timestamp dateRendered, String phoneNumber, String clientID, String status, Long hoursWorked, GeoPoint geoPoint) {
        this.name = name;
        this.amountPaid = amountPaid;
        this.dateRendered = dateRendered;
        this.phoneNumber = phoneNumber;
        this.clientID = clientID;
        this.status = status;
        this.hoursWorked = hoursWorked;
        this.geoPoint = geoPoint;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }



    public JobsInfo(String name, Double amountPaid, Timestamp dateRendered, String phoneNumber, String clientID, String status) {
        this.name = name;
        this.amountPaid = amountPaid;
        this.dateRendered = dateRendered;
        this.phoneNumber = phoneNumber;
        this.clientID = clientID;
        this.status = status;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public Long getHoursWorked() {
        return hoursWorked;
    }

    public JobsInfo(String name, Double amountPaid, Timestamp dateRendered, String phoneNumber) {
        this.name = name;
        this.amountPaid = amountPaid;
        this.dateRendered = dateRendered;
        this.phoneNumber = phoneNumber;
    }

    public void setHoursWorked(Long hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Timestamp getDateRendered() {
        return dateRendered;
    }

    public void setDateRendered(Timestamp dateRendered) {
        this.dateRendered = dateRendered;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        if (amountPaid == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(amountPaid);
        }
        dest.writeParcelable(dateRendered, flags);
        dest.writeString(phoneNumber);
        dest.writeString(clientID);
        dest.writeString(status);
        if (hoursWorked == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(hoursWorked);
        }
        dest.writeByte((byte) (started ? 1 : 0));
    }
}
