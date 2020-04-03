package com.example.zeetasupport.data;

import com.google.firebase.Timestamp;

public class CompletedJobs {

    private String name;
    private Double amountPaid;
    private Timestamp dateRendered;
    private String phoneNumber;

    public CompletedJobs(String name, Double amountPaid, Timestamp dateRendered, String phoneNumber) {
        this.name = name;
        this.amountPaid = amountPaid;
        this.dateRendered = dateRendered;
        this.phoneNumber = phoneNumber;
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

}
