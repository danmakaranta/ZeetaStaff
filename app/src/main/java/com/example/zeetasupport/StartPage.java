package com.example.zeetasupport;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.widget.Toast;

import java.util.Objects;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class StartPage extends AppCompatActivity {

    //lets use Handler and runnable
    private Handler mHandler = new Handler();
    private Runnable mRunnable;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_page);
        if (isInternetConnection()) {
            new CountDownTimer(3000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // do nothing 1s
                }

                @Override
                public void onFinish() {
                    // do something end times 5s
                    Intent intent = new Intent(StartPage.this, Signin.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }

            }.start();

        } else {
            Toast.makeText(this, "Please check that you are connected to the internet!", Toast.LENGTH_SHORT).show();
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean isInternetConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        return Objects.requireNonNull(connectivityManager.getActiveNetworkInfo()).isConnectedOrConnecting();
    }

}
