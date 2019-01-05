package com.helpyou.itproject;

/*
 * Author: Uvin Abeysinghe
 * Student Id : 789931
 * University of Melbourne
 */

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class IncomingCallScreenActivity extends BaseActivity {

    static final String TAG = IncomingCallScreenActivity.class.getSimpleName();
    private String mCallId;
    private String mImageUrl;
    private AudioPlayer mAudioPlayer;
    private DatabaseReference imgDb;
    private ImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.incoming_pop_up);
        profileImage = findViewById(R.id.profileImage);
      Button answer = (Button) findViewById(R.id.answerButton);
        answer.setOnClickListener(mClickListener);
       Button decline = (Button) findViewById(R.id.declineButton);
        decline.setOnClickListener(mClickListener);

        mAudioPlayer = new AudioPlayer(this);
        mAudioPlayer.playRingtone();
        mCallId = getIntent().getStringExtra(SinchService.CALL_ID);
       mImageUrl = getIntent().getStringExtra(SinchService.IMAGE_URL);
       imgDb = FirebaseDatabase.getInstance().getReference("users");






    }
    //display profile photo
    private void  uploadTheImageToImageView(){

        if(!mImageUrl.isEmpty()) {
            Glide.with(getApplicationContext()).load(mImageUrl).into(profileImage);
        }




    }

    //passing the call if incoming call
    @Override
    protected void onServiceConnected() {
        Call call = getSinchServiceInterface().getCall(mCallId);


        if (call != null) {
            call.addCallListener(new SinchCallListener());
            TextView remoteUser = (TextView) findViewById(R.id.remoteUser);
            FirebaseDatabase.getInstance().getReference("users").child(call.getRemoteUserId()).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    remoteUser.setText(dataSnapshot.getValue().toString());


                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            uploadTheImageToImageView();


        } else {
            Log.e(TAG, "Started with invalid callId, aborting");
            finish();
        }
    }
    //answer clicked
    private void answerClicked() {
        mAudioPlayer.stopRingtone();
        Call call = getSinchServiceInterface().getCall(mCallId);


        if (call != null) {
            call.answer();
            Intent intent = new Intent(this, CallScreenActivity.class);
            intent.putExtra(SinchService.CALL_ID, mCallId);
            startActivity(intent);
        } else {
            finish();
        }
    }
    //decline clicked
    private void declineClicked() {
        mAudioPlayer.stopRingtone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.hangup();
        }
        finish();

    }

    //call listener
    private class SinchCallListener implements CallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended, cause: " + cause.toString());
            mAudioPlayer.stopRingtone();
            finish();
        }

        @Override
        public void onCallEstablished(Call call) {

            Log.d(TAG, "Call established");
        }

        @Override
        public void onCallProgressing(Call call) {


            Log.d(TAG, "Call progressing");
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // Send a push through your push provider here, e.g. GCM
        }
    }

    private OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.answerButton:
                    answerClicked();
                    break;
                case R.id.declineButton:
                    declineClicked();
                    break;
            }
        }
    };
}
