package com.example.zeetasupport.data;

import com.google.firebase.Timestamp;

public class Message {

    String content;
    String sender;
    String receiver;
    Timestamp timeStamp;

    public Message(String content, String sender, String receiver, Timestamp timeStamp) {
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
        this.timeStamp = timeStamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public Timestamp getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Timestamp timeStamp) {
        this.timeStamp = timeStamp;
    }
}
