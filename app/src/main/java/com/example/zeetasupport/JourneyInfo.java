package com.example.zeetasupport;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class JourneyInfo implements Parcelable {

    private GeoPoint pickupLocation;
    private GeoPoint destination;
    public static final Creator<JourneyInfo> CREATOR = new Creator<JourneyInfo>() {
        @Override
        public JourneyInfo createFromParcel(Parcel in) {
            return new JourneyInfo(in);
        }

        @Override
        public JourneyInfo[] newArray(int size) {
            return new JourneyInfo[size];
        }
    };
    private String driverName;
    private Long distanceCovered;
    private String driverPhoneNumber;
    private @ServerTimestamp
    Date journeyTime;
    private @ServerTimestamp
    Timestamp timeStamp;
    private String serviceProviderID;
    private Long amount;
    private Boolean accepted;
    private Boolean started;
    private Boolean ended;

    protected JourneyInfo(Parcel in) {
        driverName = in.readString();
        driverPhoneNumber = in.readString();
        if (in.readByte() == 0) {
            distanceCovered = null;
        } else {
            distanceCovered = in.readLong();
        }
        timeStamp = in.readParcelable(Timestamp.class.getClassLoader());
        serviceProviderID = in.readString();
        if (in.readByte() == 0) {
            amount = null;
        } else {
            amount = in.readLong();
        }
        byte tmpAccepted = in.readByte();
        accepted = tmpAccepted == 0 ? null : tmpAccepted == 1;
        byte tmpStarted = in.readByte();
        started = tmpStarted == 0 ? null : tmpStarted == 1;
        byte tmpEnded = in.readByte();
        ended = tmpEnded == 0 ? null : tmpEnded == 1;
    }

    public JourneyInfo(GeoPoint pickupLocation, GeoPoint destination, String driverName, String driverPhoneNumber, Long distanceCovered, Timestamp timeStamp, String serviceProviderID, Long amount, Boolean accepted, Boolean started, Boolean ended) {
        this.pickupLocation = pickupLocation;
        this.destination = destination;
        this.driverName = driverName;
        this.driverPhoneNumber = driverPhoneNumber;
        this.distanceCovered = distanceCovered;
        this.timeStamp = timeStamp;
        this.serviceProviderID = serviceProviderID;
        this.amount = amount;
        this.accepted = accepted;
        this.started = started;
        this.ended = ended;
    }

    public JourneyInfo(GeoPoint pickupLocation, GeoPoint destination, String driverName, String driverPhoneNumber, Long distanceCovered, Date journeyTime, String serviceProviderID, Long amount, Boolean accepted, Boolean started, Boolean ended) {
        this.pickupLocation = pickupLocation;
        this.destination = destination;
        this.driverName = driverName;
        this.driverPhoneNumber = driverPhoneNumber;
        this.distanceCovered = distanceCovered;
        this.journeyTime = journeyTime;
        this.serviceProviderID = serviceProviderID;
        this.amount = amount;
        this.accepted = accepted;
        this.started = started;
        this.ended = ended;
    }

    public JourneyInfo(GeoPoint pickupLocation, GeoPoint destination, String driverName, String driverPhoneNumber, Long distanceCovered, Date journeyTime) {
        this.pickupLocation = pickupLocation;
        this.destination = destination;
        this.driverName = driverName;
        this.driverPhoneNumber = driverPhoneNumber;
        this.distanceCovered = distanceCovered;
        this.journeyTime = journeyTime;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(driverName);
        dest.writeString(driverPhoneNumber);
        if (distanceCovered == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(distanceCovered);
        }
        dest.writeParcelable(timeStamp, flags);
        dest.writeString(serviceProviderID);
        if (amount == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(amount);
        }
        dest.writeByte((byte) (accepted == null ? 0 : accepted ? 1 : 2));
        dest.writeByte((byte) (started == null ? 0 : started ? 1 : 2));
        dest.writeByte((byte) (ended == null ? 0 : ended ? 1 : 2));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public Timestamp getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Timestamp timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getServiceProviderID() {
        return serviceProviderID;
    }

    public void setServiceProviderID(String serviceProviderID) {
        this.serviceProviderID = serviceProviderID;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Boolean getStarted() {
        return started;
    }

    public void setStarted(Boolean started) {
        this.started = started;
    }

    public Boolean getEnded() {
        return ended;
    }

    public void setEnded(Boolean ended) {
        this.ended = ended;
    }

    public GeoPoint getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(GeoPoint pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public GeoPoint getDestination() {
        return destination;
    }

    public void setDestination(GeoPoint destination) {
        this.destination = destination;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhoneNumber() {
        return driverPhoneNumber;
    }

    public void setDriverPhoneNumber(String driverPhoneNumber) {
        this.driverPhoneNumber = driverPhoneNumber;
    }

    public Long getDistanceCovered() {
        return distanceCovered;
    }

    public void setDistanceCovered(Long distanceCovered) {
        this.distanceCovered = distanceCovered;
    }

    public Date getJourneyTime() {
        return journeyTime;
    }

    public void setJourneyTime(Date journeyTime) {
        this.journeyTime = journeyTime;
    }


}
