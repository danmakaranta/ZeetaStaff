package com.example.zeetasupport.data;

import java.util.Date;

public class JobsInfo {

    private String name;
    private int amountPaid;
    private Date dateRendered;
    private String phoneNumber;

    public JobsInfo(String name, int amountPaid, Date dateRendered, String phoneNumber) {
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

    public void setAmountPaid(int amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Date getDateRendered() {
        return dateRendered;
    }

    public void setDateRendered(Date dateRendered) {
        this.dateRendered = dateRendered;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
