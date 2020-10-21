package com.example.zeetasupport;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.braintreepayments.cardform.view.CardForm;
import com.example.zeetasupport.data.PurchaseTransactionData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import co.paystack.android.Paystack;
import co.paystack.android.PaystackSdk;
import co.paystack.android.Transaction;
import co.paystack.android.model.Card;
import co.paystack.android.model.Charge;

public class CreditCardLayout extends AppCompatActivity {

    private static final String TAG = "CreditCardLayout :";
    //payment variables
    CardForm cardForm;
    Button buyBtn;
    Card card;
    AlertDialog.Builder paymentAlertBuilder;
    private AlertDialog paymentAlertDialog;
    private int connectRate;
    private int selectedNumberOfConnects;
    private int temp;
    private int minPurchaseValue;
    private double amountToPurchase = 0;
    private ProgressDialog transactionProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_card_layout);

        transactionProgressDialog = new ProgressDialog(this);
        transactionProgressDialog.setMessage("Please wait...");

        connectRate = getIntent().getIntExtra("connectRate", 5);
        minPurchaseValue = getIntent().getIntExtra("minPurchaseValue", 10);

        PaystackSdk.initialize(getApplicationContext());
        cardForm = findViewById(R.id.card_form2);

        cardForm.cardRequired(true)
                .expirationRequired(true)
                .cvvRequired(true)
                .mobileNumberRequired(true)
                .mobileNumberExplanation("This number is required for SMS")
                .setup(CreditCardLayout.this);
        cardForm.getCvvEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        getAmountToCharge();

    }

    private void performCharge() {

        //create a Charge object
        Charge charge = new Charge();
        charge.setCard(card); //sets the card to charge
        charge.setEmail("kwadagonigerialtd@gmail.com"); //dummy email address
        charge.setAmount((int) (100 * amountToPurchase)); //test amount

        //call this method if you set a plan
        //charge.setPlan("One_off");


        DocumentReference newPurchase = FirebaseFirestore.getInstance()
                .collection("ConnectPurchase").document("newRequest")
                .collection(FirebaseAuth.getInstance().getUid()).document();

        Timestamp transacTime = Timestamp.now();

        final PurchaseTransactionData[] purchaseTransactionData = new PurchaseTransactionData[1];

        PaystackSdk.chargeCard(CreditCardLayout.this, charge, new Paystack.TransactionCallback() {
            @Override
            public void onSuccess(Transaction transaction) {
                purchaseTransactionData[0] = new PurchaseTransactionData(FirebaseAuth.getInstance().getUid(), charge.getAmount() / 100, (charge.getAmount() / 100) / connectRate, card, transacTime,
                        true, "Card Debit");
                newPurchase.set(purchaseTransactionData[0]).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        transactionProgressDialog.dismiss();
                        Toast.makeText(CreditCardLayout.this, "Charge successful", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

            }

            @Override
            public void beforeValidate(Transaction transaction) {

            }

            @Override
            public void onError(Throwable error, Transaction transaction) {
                Toast.makeText(CreditCardLayout.this, "Charge Unsuccessful: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void getAmountToCharge() {

        EditText connectsInputET = findViewById(R.id.cardConnects_input);
        TextView totalPurchased = findViewById(R.id.cardTotal_purchased);
        Button minusConnectBtn = findViewById(R.id.cardMinus_connects);
        Button addConnectBtn = findViewById(R.id.cardAdd_connects);
        Button payBtn = findViewById(R.id.cardPayNow);

        totalPurchased.setText("N" + (Integer.parseInt(connectsInputET.getText().toString())) * connectRate);

        minusConnectBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                selectedNumberOfConnects = minPurchaseValue;
                String tempVal = connectsInputET.getText().toString();
                if (tempVal.length() > 0) {
                    int connectInput = Integer.parseInt(connectsInputET.getText().toString());
                    if (connectInput <= minPurchaseValue) {
                        connectsInputET.setText("" + minPurchaseValue);
                        selectedNumberOfConnects = minPurchaseValue;

                    } else {
                        temp = 0;
                        int val = (Integer.parseInt(connectsInputET.getText().toString())) - 1;
                        selectedNumberOfConnects = val;
                        connectsInputET.setText("" + val);
                        temp = val * connectRate;
                        totalPurchased.setText("N" + temp);
                    }
                } else {
                    Toast.makeText(CreditCardLayout.this, "Number of connects can't be Zero(0)", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "selected number of connects: Minus " + selectedNumberOfConnects);
            }
        });

        addConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNumberOfConnects = minPurchaseValue;

                int connectInput = Integer.parseInt(connectsInputET.getText().toString());
                if (connectInput < minPurchaseValue) {
                    connectsInputET.setText("" + minPurchaseValue);
                    selectedNumberOfConnects = minPurchaseValue;
                } else {
                    temp = 0;
                    int val = (Integer.parseInt(connectsInputET.getText().toString())) + 1;
                    selectedNumberOfConnects = val;
                    connectsInputET.setText("" + val);
                    temp = val * connectRate;
                    totalPurchased.setText("N" + temp);
                }
                Log.d(TAG, "selected number of connects: Plus " + selectedNumberOfConnects);

            }
        });

        payBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int connectInput = Integer.parseInt(connectsInputET.getText().toString());
                if (connectInput < minPurchaseValue) {
                    Toast.makeText(CreditCardLayout.this, minPurchaseValue + " is the minimum number of connects you can buy", Toast.LENGTH_LONG).show();
                } else {
                    selectedNumberOfConnects = connectInput;
                    amountToPurchase = (double) (selectedNumberOfConnects * connectRate);
                    totalPurchased.setText("N" + amountToPurchase);
                    if (cardForm.isValid()) {
                        paymentAlertBuilder = new AlertDialog.Builder(CreditCardLayout.this);
                        paymentAlertBuilder.setTitle("Confirm before purchase");
                        paymentAlertBuilder.setMessage("Purchase of Connects from your card: " + cardForm.getCardNumber() + " ?");


                        paymentAlertBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int expM = Integer.parseInt(cardForm.getExpirationMonth());
                                int expY = Integer.parseInt(cardForm.getExpirationYear());

                                card = new Card(cardForm.getCardNumber(), expM, expY, cardForm.getCvv());

                                if (card.isValid()) {
                                    transactionProgressDialog.show();
                                    performCharge();
                                } else {
                                    Toast.makeText(CreditCardLayout.this, "Card is inValid!", Toast.LENGTH_LONG).show();
                                }
                            /*dialogInterface.dismiss();
                            Toast.makeText(CreditCardLayout.this, "Thank you for your purchase", Toast.LENGTH_LONG).show();
                            finish();*/
                            }
                        });
                        paymentAlertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        AlertDialog alertDialog = paymentAlertBuilder.create();
                        alertDialog.show();
                    } else {
                        Toast.makeText(CreditCardLayout.this, "Please complete the payment form", Toast.LENGTH_LONG).show();
                    }

                }
            }
        });


    }

}