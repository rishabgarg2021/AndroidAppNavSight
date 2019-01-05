package com.helpyou.itproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.sinch.android.rtc.SinchError;


public class MainActivity extends BaseActivity implements  SinchService.StartFailedListener{

    private static final String KEY_PHONE = "phone";
    private static final String KEY_USERID = "userID";
    private DatabaseReference db;
    private DatabaseReference db2;

    //starts the sinch listener
    protected void onServiceConnected() {

        getSinchServiceInterface().setStartListener(this);

    }

    @Override
    public void onStartFailed(SinchError error) {

    }

    @Override
    public void onStarted() {

    }


    //checks of the user has previously log in using this saved data
    private void manageNextActivity() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());


        final String phone = sp.getString(KEY_PHONE, null);
        final String userId = sp.getString(KEY_USERID, null);


        db = FirebaseDatabase.getInstance().getReference("users");


        if (phone != null && userId != null) {
            db.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(userId)) {
                        openMainMenuMapActivity(userId, phone);

                    } else {

                        openUserInfoActivity(userId, phone);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


        }
        else{
            Intent i = new Intent(MainActivity.this,Login.class);

            startActivity(i);
            finish();

        }


    }

    //open main menu activity
    private void openMainMenuMapActivity(String userId, String phoneNumber) {
        Intent intent = new Intent(MainActivity.this, Main_Menu_Map.class);

        //start sinch of not started
        if (!getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient(userId);


        }
        intent.putExtra("userId", userId);
        intent.putExtra("phone", phoneNumber);
        updateToken(userId);

        startActivity(intent);
        finish();


    }

    //opens the user info activity
    private void openUserInfoActivity(String userId, String phoneNumber) {
        final String mUserId = userId;
        final String mPhoneNumber = phoneNumber;
        Intent intent = new Intent(MainActivity.this, userInfo.class);

        if (!getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient(mUserId);
        }
        intent.putExtra("phone", mPhoneNumber);
        intent.putExtra("userId", mUserId);

        updateToken(mUserId);

        startActivity(intent);
        finish();


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Fresco.initialize(this);





        manageNextActivity();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);

    }

    //updates the firebase token on firebase
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
