package com.example.zeetasupport.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class JobData implements Parcelable {
    public static final Creator<JobData> CREATOR = new Creator<JobData>() {
        @Override
        public JobData createFromParcel(Parcel in) {
            return new JobData(in);
        }

        @Override
        public JobData[] newArray(int size) {
            return new JobData[size];
        }
    };
    String clientID;
    String clientName;
    String clientPhone;
    String status;
    Long amountPaid;
    GeoPoint gp;
    Long hoursWorked;
    private @ServerTimestamp
    Date timeStamp;

    public JobData(String clientID, String clientName, String clientPhone, String status, Long amountPaid, Date timeStamp, GeoPoint gp, Long hoursWorked) {
        this.clientID = clientID;
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.status = status;
        this.amountPaid = amountPaid;
        this.gp = gp;
        this.hoursWorked = hoursWorked;
        this.timeStamp = timeStamp;
    }

    public JobData(String clientID, String clientName, String clientPhone, String status, Long amountPaid, Date timeStamp, GeoPoint gp) {
        this.clientID = clientID;
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.status = status;
        this.amountPaid = amountPaid;
        this.gp = gp;
        this.timeStamp = timeStamp;
    }

    public JobData(String clientID, String clientName, String clientPhone, String status, Long amountPaid, Date timeStamp) {
        this.clientID = clientID;
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.status = status;
        this.amountPaid = amountPaid;
        this.timeStamp = timeStamp;
    }

    protected JobData(Parcel in) {
    }

    public static Creator<JobData> getCREATOR() {
        return CREATOR;
    }

    public Long getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(Long hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public GeoPoint getGp() {
        return gp;
    }

    public void setGp(GeoPoint gp) {
        this.gp = gp;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Long amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
