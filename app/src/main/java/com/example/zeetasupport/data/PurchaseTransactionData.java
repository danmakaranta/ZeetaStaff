package com.example.zeetasupport.data;

import com.google.firebase.Timestamp;

import co.paystack.android.model.Card;

public class PurchaseTransactionData {
    private String id;
    private long amountPaid;
    private long connects;
    private Card card;
    private Timestamp transactionDate;
    private boolean successful;
    private String transactionType;

    public PurchaseTransactionData(String id, long amountPaid, long connects, Card card, Timestamp transactionDate, boolean successful, String transactionType) {
        this.id = id;
        this.amountPaid = amountPaid;
        this.connects = connects;
        this.card = card;
        this.transactionDate = transactionDate;
        this.successful = successful;
        this.transactionType = transactionType;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(long amountPaid) {
        this.amountPaid = amountPaid;
    }

    public long getConnects() {
        return connects;
    }

    public void setConnects(long connects) {
        this.connects = connects;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public Timestamp getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Timestamp transactionDate) {
        this.transactionDate = transactionDate;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
