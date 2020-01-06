package com.example.zeetasupport;

import android.app.Application;

import com.google.firebase.firestore.auth.User;

public class UserClient extends Application {

    private User user = null;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
