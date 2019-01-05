package com.helpyou.itproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthSettings;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.hbb20.CountryCodePicker;
import com.sinch.android.rtc.SinchError;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Login extends BaseActivity implements SinchService.StartFailedListener {

    private EditText phoneText;
    private Button sendButton;
    private Button verifyButton;
    private Button resendButton;
    String number;       //the number entered by the user.
    private EditText codeText;
    private String phoneVerificationId;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks verificationCallbacks;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private FirebaseAuth fbAuth;
    CountryCodePicker ccp;
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    PorterDuffColorFilter greyFilter = new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
    PorterDuffColorFilter indigoFilter = new PorterDuffColorFilter(Color.rgb(63, 81, 181), PorterDuff.Mode.MULTIPLY);
    SharedPreferences sharedPreferences;
    private DatabaseReference db;
    private DatabaseReference db2;


    SharedPreferences.Editor editor;

    private static final String KEY_PHONE = "phone";
    private static final String KEY_USERID = "userID";
    Map<String, Object> payload = new HashMap<String, Object>();

    //will only come to login if we don't have users stored in shared preference.
    private void manageNextActivity(final String user_id, final String phone_number) {

        db = FirebaseDatabase.getInstance().getReference("users");


        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChild(user_id)) {
                    openMainMenuMapActivity(user_id, phone_number);

                } else {

                    openUserInfoActivity(user_id, phone_number);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    //open main menu activity
    private void openMainMenuMapActivity(String userId, String phoneNumber) {
        Intent intent = new Intent(Login.this, Main_Menu_Map.class);


        if (!getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient(userId);


        }
        intent.putExtra("userId", userId);
        intent.putExtra("phone", phoneNumber);
        updateToken(userId);

        startActivity(intent);
        finish();


    }
    //open user info activity to enter user details.
    private void openUserInfoActivity(String userId, String phoneNumber) {
        final String mUserId = userId;
        final String mPhoneNumber = phoneNumber;
        Intent intent = new Intent(Login.this, userInfo.class);

        if (!getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient(mUserId);
        }
        intent.putExtra("phone", mPhoneNumber);
        intent.putExtra("userId", mUserId);

        updateToken(mUserId);

        startActivity(intent);
        startActivity(intent);
        finish();


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = sharedPreferences.edit();


        db = FirebaseDatabase.getInstance().getReference("users");


        phoneText = findViewById(R.id.phoneText);
        resendButton = findViewById(R.id.resendButton2);
        sendButton = findViewById(R.id.sendButton);
        ccp = (CountryCodePicker) findViewById(R.id.ccp);
        ccp.registerCarrierNumberEditText(phoneText);

        fbAuth = FirebaseAuth.getInstance();


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendCode();
            }
        });
        verifyButton = findViewById(R.id.verify);
        codeText = findViewById(R.id.codeText);

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                verifyCode();
            }
        });
        resendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                resendCode();
            }
        });
        phoneText.addTextChangedListener(new TextWatcher() {


            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                sendButton.setEnabled(true);
                sendButton.getBackground().setColorFilter(indigoFilter);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                sendButton.setEnabled(true);
                sendButton.getBackground().setColorFilter(indigoFilter);

            }
        });

    }

    private void verifyCode() {
        String code = codeText.getText().toString();
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(phoneVerificationId, code);

        signInWithPhoneAuthCredential(credential);
    }

    private void sendCode() {
        number = ccp.getFullNumberWithPlus(); //country code is needed to send the number.
        setUpVerificationCallBacks();

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                number,        // Phone number to verify
                60,                 // Ti   meout duration
                TimeUnit.SECONDS,   // Unit of timeout
                Login.this,               // Activity (for callback binding)
                verificationCallbacks);

    }


    private void setUpVerificationCallBacks() {
        verificationCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Toast.makeText(Login.this, "Verification Done", Toast.LENGTH_SHORT).show();

                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
//                Log.w(TAG, "onVerificationFailed", e);
                Toast.makeText(Login.this, e.toString(), Toast.LENGTH_LONG).show();

//                Toast.makeText(Login.this, "Verification Failed",Toast.LENGTH_SHORT).show();


                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // ...
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // ...
                }

                // Show a message and update the UI
                // ...
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                // Save verification ID and resending token so we can use them later
                Toast.makeText(Login.this, "Code has been sent", Toast.LENGTH_SHORT).show();

                phoneVerificationId = verificationId;
                resendToken = token;

                sendButton.getBackground().setColorFilter(greyFilter);
                sendButton.setEnabled(false);
                resendButton.setEnabled(true);
                verifyButton.setEnabled(true);


                // ...
            }
        };

    }

    //resent the one-time code
    private void resendCode() {
        number = ccp.getFullNumberWithPlus();

        setUpVerificationCallBacks();
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                number,
                60,
                TimeUnit.SECONDS,
                this,
                verificationCallbacks,
                resendToken);
    }


    private void signInWithPhoneAuthCredential(final PhoneAuthCredential credential) {
        fbAuth.signInWithCredential(credential).addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information

                    resendButton.setEnabled(false);
                    verifyButton.setEnabled(false);
                    resendButton.getBackground().setColorFilter(greyFilter);
                    verifyButton.getBackground().setColorFilter(greyFilter);
                    FirebaseUser user = task.getResult().getUser();
                    final String phoneNumber = user.getPhoneNumber();
                    final String mUserID = user.getUid();
                    //Start sinch client

                    if (!getSinchServiceInterface().isStarted()) {
                        getSinchServiceInterface().startClient(user.getUid());


                    }
                    //user details will be stored here.
                    managePrefs(mUserID, phoneNumber);


                    manageNextActivity(mUserID, phoneNumber);
                    finish();


                    // ...
                } else {
                    // Sign in failed, display a message and update the UI
                    Toast.makeText(Login.this, "signInWithCredential:failure", Toast.LENGTH_SHORT).show();


                    if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                }
            }
        });
    }

    private void managePrefs(String userId, String phoneNumber) {
        editor.putString(KEY_USERID, userId);
        editor.putString(KEY_PHONE, phoneNumber);

        editor.commit();

    }

    public void testPhoneAutoRetrieve() {
        // [START auth_test_phone_auto]
        // The test phone number and code should be whitelisted in the console.
        String phoneNumber = "+61406255366";
        String smsCode = "123456";
        phoneVerificationId = smsCode;
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseAuthSettings firebaseAuthSettings = firebaseAuth.getFirebaseAuthSettings();

        // Configure faking the auto-retrieval with the whitelisted numbers.
        firebaseAuthSettings.setAutoRetrievedSmsCodeForPhoneNumber(phoneNumber, smsCode);

        PhoneAuthProvider phoneAuthProvider = PhoneAuthProvider.getInstance();
        phoneAuthProvider.verifyPhoneNumber(
                phoneNumber,
                60L,
                TimeUnit.SECONDS,
                this, /* activity */
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(final PhoneAuthCredential credential) {
                        // Instant verification is applied and a credential is directly returned.
                        // ...
                        Toast.makeText(Login.this, "logged in ", Toast.LENGTH_SHORT).show();
                        fbAuth.signInWithCredential(credential).addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = task.getResult().getUser();
                                    String phoneNumber = user.getPhoneNumber();
                                    managePrefs(user.getUid(), phoneNumber);

                                    if (!getSinchServiceInterface().isStarted()) {
                                        getSinchServiceInterface().startClient(user.getUid());


                                    }


                                    manageNextActivity(user.getUid().toString(), phoneNumber);
                                    finish();
                                } else {
                                    // Sign in failed, display a message and update the UI
                                    Toast.makeText(Login.this, "signInWithCredential:failure", Toast.LENGTH_SHORT).show();

                                    if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                        // The verification code entered was invalid
                                    }
                                }
                            }
                        });


                    }

                    // [START_EXCLUDE]
                    @Override
                    public void onVerificationFailed(FirebaseException e) {

                    }
                    // [END_EXCLUDE]
                });
        // [END auth_test_phone_auto]
    }

    protected void onServiceConnected() {

        getSinchServiceInterface().setStartListener(this);

    }

    @Override
    public void onStartFailed(SinchError error) {

    }

    @Override
    public void onStarted() {

    }

    //update the firebase token on firebase
    private void updateToken(String mUserID) {

        final String userId = mUserID;
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {

                db2 = FirebaseDatabase.getInstance().getReference("tokens");
                String deviceToken = instanceIdResult.getToken();
                db2.child(userId).child("device_token").setValue(deviceToken).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                    }
                });


            }
        });
    }
}