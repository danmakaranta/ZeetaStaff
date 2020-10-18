package com.example.zeetasupport;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.braintreepayments.cardform.view.CardForm;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

    //payment variables
    CardForm cardForm;
    Button buyBtn;
    Card card;
    AlertDialog.Builder paymentAlertBuilder;
    private AlertDialog paymentAlertDialog;
    private int connectRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_card_layout);
        connectRate = getIntent().getIntExtra("connectRate", 5);
        PaystackSdk.initialize(getApplicationContext());
        cardForm = findViewById(R.id.card_form2);
        buyBtn = findViewById(R.id.payNow);
        cardForm.cardRequired(true)
                .expirationRequired(true)
                .cvvRequired(true)
                .mobileNumberRequired(true)
                .mobileNumberExplanation("This number is required for SMS")
                .setup(CreditCardLayout.this);
        cardForm.getCvvEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);


        buyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                                //Toast.makeText(CreditCardLayout.this, "Card is Valid!", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(CreditCardLayout.this, "Please complete the form", Toast.LENGTH_LONG).show();
                }

            }
        });

    }

    private void performCharge() {

        //create a Charge object
        Charge charge = new Charge();
        charge.setCard(card); //sets the card to charge

        //call this method if you set a plan
        //charge.setPlan("One_off");

        //reference for firebase for authentication
        DocumentReference transactionRef = FirebaseFirestore.getInstance()
                .collection("Transactions").document("ConnectPurchase");

        charge.setReference(FirebaseAuth.getInstance().getUid());

        charge.setEmail("kwadagonigerialtd@gmail.com"); //dummy email address

        charge.setAmount(100); //test amount

        PaystackSdk.chargeCard(CreditCardLayout.this, charge, new Paystack.TransactionCallback() {
            @Override
            public void onSuccess(Transaction transaction) {
                transactionRef.set(transaction).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(CreditCardLayout.this, "Charge successful", Toast.LENGTH_SHORT).show();
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

        Dialog payWithWaletDialog = new Dialog(CreditCardLayout.this);
        payWithWaletDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        payWithWaletDialog.setContentView(R.layout.connects_amount);
        TextView balanceForWalet = payWithWaletDialog.findViewById(R.id.balanceForAmount);
        EditText connectsInputET = payWithWaletDialog.findViewById(R.id.connects_input);
        TextView totalPurchased = payWithWaletDialog.findViewById(R.id.total_purchased);
        Button minusConnectBtn = payWithWaletDialog.findViewById(R.id.minus_connects);
        Button addConnectBtn = payWithWaletDialog.findViewById(R.id.add_connects);
        Button walletPayBtn = payWithWaletDialog.findViewById(R.id.payWithWallet);

        totalPurchased.setText("N" + (Integer.parseInt(connectsInputET.getText().toString())) * connectRate);


    }

}