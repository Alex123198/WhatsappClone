package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import java.util.concurrent.TimeUnit;

public class PhoneLoginActivity extends AppCompatActivity {

    private Button SendVerificationCode,VerifyButton,WrongNumberButton;
    private EditText InputPhoneNumber;
    private EditText InputVerificationCode;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private String mVerificationId;
    private ProgressDialog loadingBar;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);
        WrongNumberButton = (Button) findViewById(R.id.wrong_number_entered);
        SendVerificationCode = (Button) findViewById(R.id.send_ver_code_button);
        VerifyButton = (Button) findViewById(R.id.verify_button);
        InputPhoneNumber = (EditText) findViewById(R.id.phone_number_input);
        InputVerificationCode = (EditText) findViewById(R.id.verification_code_input);
        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);

        SendVerificationCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String userPhoneNumber = InputPhoneNumber.getText().toString();
                if(TextUtils.isEmpty(userPhoneNumber)){ // In caz ca user-ul nu introduce un numar de telefon

                    Toast.makeText(PhoneLoginActivity.this, "Enter a phone number first", Toast.LENGTH_SHORT).show();
                }
                else {

                    loadingBar.setTitle("Phone verification");
                    loadingBar.setMessage("Please wait...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthProvider.getInstance().verifyPhoneNumber( // Aceasta bucata de cod a fost luata din documentatia celor de la Google pentru FireBase
                            userPhoneNumber,
                            60,
                            TimeUnit.SECONDS,
                            PhoneLoginActivity.this,
                            callbacks);
                }
            }
        });

        VerifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SendVerificationCode.setVisibility(View.INVISIBLE); // Ascund butonul de trimitere a codului deoarece a fost apasat si nu mai este nevoie de acesta
                InputPhoneNumber.setVisibility(View.INVISIBLE); // Mai ascund si input-ul pentru input phone number deoarece acesta nu mai e necesar odata trimis
                String verificationCode = InputVerificationCode.getText().toString();
                if (TextUtils.isEmpty(verificationCode)) { // Daca codul de verificare nu este introdus primim o eroare

                    Toast.makeText(PhoneLoginActivity.this, "Please write the verification code first!", Toast.LENGTH_SHORT).show();
                }
                else {

                    loadingBar.setTitle("Code verification");
                    loadingBar.setMessage("Please wait...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId,verificationCode);
                    signInWithPhoneAuthCredential(credential);
                }
            }
        });

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {

                loadingBar.dismiss();
                Toast.makeText(PhoneLoginActivity.this, "Invalid phone number,please check the phone number you've just entered.", Toast.LENGTH_SHORT).show();

                SendVerificationCode.setVisibility(View.VISIBLE);
                InputPhoneNumber.setVisibility(View.VISIBLE);

                VerifyButton.setVisibility(View.INVISIBLE);
                InputVerificationCode.setVisibility(View.INVISIBLE);
                WrongNumberButton.setVisibility(View.INVISIBLE);
            }

            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {


                mVerificationId = verificationId;
                mResendToken = token;
                loadingBar.dismiss();
                Toast.makeText(PhoneLoginActivity.this, "The Code has been sent succesfully!", Toast.LENGTH_SHORT).show();

                SendVerificationCode.setVisibility(View.GONE);
                InputPhoneNumber.setVisibility(View.INVISIBLE);

                VerifyButton.setVisibility(View.VISIBLE);
                InputVerificationCode.setVisibility(View.VISIBLE);
                WrongNumberButton.setVisibility(View.VISIBLE);
            }
        };

        WrongNumberButton.setOnClickListener(new View.OnClickListener() { // Daca utilizatorul crede ca a introdus un numar de telefon gresit atunci poate sa retrimita codul
            @Override
            public void onClick(View v) {

                Toast.makeText(PhoneLoginActivity.this, "Be careful next time and enter the number that you actually want", Toast.LENGTH_LONG).show();
                SendVerificationCode.setVisibility(View.VISIBLE);
                InputPhoneNumber.setVisibility(View.VISIBLE);
                WrongNumberButton.setVisibility(View.INVISIBLE);
                VerifyButton.setVisibility(View.INVISIBLE);
                InputPhoneNumber.setText("");
                InputVerificationCode.setVisibility(View.GONE);
            }
        });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) { // Codul de verificare este verificat aici daca corespunde cu cel trimis de aplicatie,in caz afirmativ
                                                                                 // Veti primi un mesaj cum ca v-ati logat cu succes si veti fi trimis la activitatea main
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            loadingBar.dismiss();
                            Toast.makeText(PhoneLoginActivity.this, "You've logged in succesfully ", Toast.LENGTH_SHORT).show();
                            SendUserToMainActivity();

                        } else {

                            String message = task.getException().toString();
                            Toast.makeText(PhoneLoginActivity.this, "Error " + message, Toast.LENGTH_SHORT).show();

                            }
                        }
                });
    }

    private void SendUserToMainActivity(){

        Intent mainIntent = new Intent(PhoneLoginActivity.this,MainActivity.class);
        startActivity(mainIntent);
        finish();
    }
}