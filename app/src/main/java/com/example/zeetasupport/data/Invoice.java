package com.example.zeetasupport.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Invoice implements Parcelable {
    public static final Creator<Invoice> CREATOR = new Creator<Invoice>() {
        @Override
        public Invoice createFromParcel(Parcel in) {
            return new Invoice(in);
        }

        @Override
        public Invoice[] newArray(int size) {
            return new Invoice[size];
        }
    };
    private String clientID;
    private String serviceProvided;
    private long hoursWorked;
    private double amount;
    private boolean paid;

    protected Invoice(Parcel in) {
        clientID = in.readString();
        serviceProvided = in.readString();
        hoursWorked = in.readLong();
        amount = in.readDouble();
        paid = in.readByte() != 0;
    }

    public Invoice(String clientID, String serviceProvided, long hoursWorked, double amount, boolean paid) {
        this.clientID = clientID;
        this.serviceProvided = serviceProvided;
        this.hoursWorked = hoursWorked;
        this.amount = amount;
        this.paid = paid;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getServiceProvided() {
        return serviceProvided;
    }

    public void setServiceProvided(String serviceProvided) {
        this.serviceProvided = serviceProvided;
    }

    public long getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(long hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(clientID);
        dest.writeString(serviceProvided);
        dest.writeLong(hoursWorked);
        dest.writeDouble(amount);
        dest.writeByte((byte) (paid ? 1 : 0));
    }
}
