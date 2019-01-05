package com.helpyou.itproject;

/*
 * Authors:
 * Uvin Abeysinghe | ID: 789931
 * Tarnvir Grewal  | ID: 838527
 * University of Melbourne
 */


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class volSelectDestWaiting extends BaseActivity {

    private String elderUid;
    private String helperUid;
    private String requestId;
    private String userType;
    private FloatingActionButton chatButton;
    private Button disconnectBttn;
    private String callFrom;
    private DatabaseReference db_disconnect_on_timer;
    static final int disconnectTimeLimit = 60000;
    private CountDownTimer counterTimer;
    boolean callPassed=false;
    private DatabaseReference db;
    private String callState;
    private FloatingActionButton callButton;
    private FloatingActionButton endCallButton;
    private AudioPlayer mAudioPlayer;
    private Call call;
    private String callTo;
    private String mCallId;
    private String callType="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vol_select_dest_waiting);

        Intent preIntent = getIntent();
        db_disconnect_on_timer = FirebaseDatabase.getInstance().getReference().child("help_req");
        elderUid = preIntent.getExtras().get("elderUid").toString();
        helperUid = preIntent.getExtras().get("helperUid").toString();
        userType = preIntent.getStringExtra("userType");
        requestId= preIntent.getStringExtra("requestId");
        chatButton = findViewById(R.id.chatButton);

        if((getIntent().getStringExtra(SinchService.CALL_ID))!=null){
            mCallId=getIntent().getStringExtra(SinchService.CALL_ID);
        }

        //save the current activity in case the user accidently  closes the app.
        if(userType.equals("Helper")){
            DatabaseReference db_eld = FirebaseDatabase.getInstance().getReference("userConnect").child(helperUid);
            db_eld.setValue(new UserConnection("1","volSelectDestWaiting", "Helper", elderUid, requestId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) { }
            });
            DatabaseReference db_eld2 = FirebaseDatabase.getInstance().getReference("userConnect").child(elderUid);
            db_eld2.setValue(new UserConnection("1","SelectDestinationToShare", "Elder", helperUid, requestId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) { }
            });

        } else {
            DatabaseReference db_eld = FirebaseDatabase.getInstance().getReference("userConnect").child(elderUid);
            db_eld.setValue(new UserConnection("1","volSelectDestWaiting", "Elder", helperUid, requestId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) { }
            });
            DatabaseReference db_eld2 = FirebaseDatabase.getInstance().getReference("userConnect").child(helperUid);
            db_eld2.setValue(new UserConnection("1","SelectDestinationToShare", "Helper", elderUid, requestId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) { }
            });
        }


        //Calling code
        /*************************************/

        mAudioPlayer = new AudioPlayer(this);

        db= FirebaseDatabase.getInstance().getReference().child("help_req");

        if (userType.equals("Elder")){
            callTo=helperUid;
            callFrom = elderUid;
        }else{
            callTo=elderUid;
            callFrom = helperUid;
        }

        fakeData();

        callButton = findViewById(R.id.callBttn);
        endCallButton = findViewById(R.id.endCallBttn);

        endCallButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        callButton.setBackgroundTintList(ColorStateList.valueOf(Color.argb(255,42, 153, 55)));
        callState="";

        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fakeData();
                if(getSinchServiceInterface().isStarted()){

                    if(callState.equals("")){
                        //starting a call
                        Map<String, String> headers = new HashMap<String, String>();

                        //type of call
                        headers.put("type", "Helper");
                        headers.put("IMAGE_URL","" );
                        call = getSinchServiceInterface().callUser(callTo, headers);
                        call.addCallListener(new SinchCallListener());
                        callState= "callStarted";
                        callButton.setEnabled(false);
                        endCallButton.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                        callButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                        mAudioPlayer.playProgressTone();
                        callType="outgoing";

                        //speaker
                        getSinchServiceInterface().getSinchClient().getAudioController().enableSpeaker();
                    }else if(callState.equals("incomingCall")){
                        answerClicked();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Client not started yet, press again in few seconds",Toast.LENGTH_LONG).show();
                }
            }
        });

        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                declineClicked();
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("call"));

        /*************************************/

        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMiniChat();
            }
        });


        /********************************************/
        //checking if disconnected from the other end
        FirebaseDatabase.getInstance().getReference("userConnect").child(helperUid).child("isConnected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if((dataSnapshot.getValue().toString()).equals("0")){
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
        FirebaseDatabase.getInstance().getReference("userConnect").child(elderUid).child("isConnected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if((dataSnapshot.getValue().toString()).equals("0")){
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });


        /****************************************/

        //disconnecting
        disconnectBttn = findViewById(R.id.disconnectBtn);
        disconnectBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FirebaseDatabase.getInstance().getReference("userConnect").child(elderUid).child("isConnected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        FirebaseDatabase.getInstance().getReference("userConnect").child(helperUid).child("isConnected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                DatabaseReference db = FirebaseDatabase.getInstance().getReference("help_req");
                                db.child(helperUid).child(requestId).child("connected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        declineClicked();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

        /****************************************/
        if (userType.equals("Elder")){

            db_disconnect_on_timer.child(helperUid).child(requestId).child("isOneAppClose").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if((dataSnapshot.getValue().toString()).equals("1")||(dataSnapshot.getValue().toString()).equals("2")){
                        db_disconnect_on_timer.child(helperUid).child(requestId).child("isOneAppClose").setValue("0");
                        if(counterTimer!=null){
                            counterTimer.cancel();
                        }
                    }
                    else if((dataSnapshot.getValue().toString()).equals("0")){
                        counterTimer= new CountDownTimer(disconnectTimeLimit,disconnectTimeLimit) {
                            @Override
                            public void onTick(long millisUntilFinished) { }

                            @Override
                            public void onFinish() {
                                FirebaseDatabase.getInstance().getReference("userConnect").child(elderUid).child("isConnected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        FirebaseDatabase.getInstance().getReference("userConnect").child(helperUid).child("isConnected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                DatabaseReference db = FirebaseDatabase.getInstance().getReference("help_req");
                                                db.child(helperUid).child(requestId).child("connected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) { }
                                                });

                                                finish();
                                            }
                                        });
                                    }
                                });
                            }
                        }.start();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }
        else if(userType.equals("Helper")){

            db_disconnect_on_timer.child(helperUid).child(requestId).child("isOneAppClose").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if ((dataSnapshot.getValue().toString()).equals("0") || (dataSnapshot.getValue().toString()).equals("2")) {
                        db_disconnect_on_timer.child(helperUid).child(requestId).child("isOneAppClose").setValue("1");
                        if (counterTimer != null) {
                            counterTimer.cancel();
                        }


                    } else if ((dataSnapshot.getValue().toString()).equals("1")) {
                        counterTimer = new CountDownTimer(disconnectTimeLimit, disconnectTimeLimit) {
                            @Override
                            public void onTick(long millisUntilFinished) { }

                            @Override
                            public void onFinish() {

                                FirebaseDatabase.getInstance().getReference("userConnect").child(elderUid).child("isConnected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        FirebaseDatabase.getInstance().getReference("userConnect").child(helperUid).child("isConnected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                DatabaseReference db = FirebaseDatabase.getInstance().getReference("help_req");
                                                db.child(helperUid).child(requestId).child("connected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) { }
                                                });

                                                finish();
                                            }
                                        });
                                    }
                                });
                            }
                        }.start();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }

        /****************************************/

        //elder enters the destination
        db.child(helperUid).child(requestId).child("elderAddedDest").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.getValue().equals("")){
                    String destinationLatLng = dataSnapshot.getValue().toString();

                    String[] latlong =  destinationLatLng.split(",");
                    double latitude = Double.parseDouble(latlong[0]);
                    double longitude = Double.parseDouble(latlong[1]);

                    LatLng location = new LatLng(latitude, longitude);

                    Intent intent = new Intent(volSelectDestWaiting.this, showDestinationRoute.class);
                    intent.putExtra("destinationLatLng", location);
                    intent.putExtra("elderUid", elderUid);
                    intent.putExtra("helperUid", helperUid);
                    intent.putExtra("userType", userType);
                    intent.putExtra("requestId", requestId);

                    //passing on the call
                    if (call!=null){
                        intent.putExtra(SinchService.CALL_ID, call.getCallId());
                    }else{
                        Call call = getSinchServiceInterface().getCall(mCallId);
                        if (call!=null){
                            intent.putExtra(SinchService.CALL_ID, mCallId);
                        }
                    }

                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        //elder wants the volunteer to enter the destination
        db.child(helperUid).child(requestId).child("wantsHelpAddingDest").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if ((dataSnapshot.getValue().toString()).equals("1")){
                    //open select dest to share
                    Intent i = new Intent(volSelectDestWaiting.this, SelectDestinationToShare.class);
                    i.putExtra("elderUid", elderUid);
                    i.putExtra("helperUid",helperUid );
                    i.putExtra("userType", "Helper");
                    i.putExtra("requestId", requestId);

                    //passing on the call
                    if (call!=null ){
                        i.putExtra(SinchService.CALL_ID, call.getCallId());
                        callPassed=true;
                    }else{

                        Call call = getSinchServiceInterface().getCall(mCallId);
                        if (call!=null){
                            i.putExtra(SinchService.CALL_ID, mCallId);
                            callPassed=true;
                        }

                    }

                    startActivity(i);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



    }

    private void openMiniChat(){
        Intent intent = new Intent(volSelectDestWaiting.this,ChatPop.class);
        intent.putExtra("chat_from_uid",callFrom);
        intent.putExtra("chat_to_uid",callTo);
        startActivity(intent);
    }


    /**********************************************************************************************/
    /*Call BroadcastReceiver*/
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            mCallId = intent.getStringExtra(SinchService.CALL_ID);
            gettingCall();



        }
    };

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }



    /**********************************************************************************************/


    /**********************************************************************************************/

    //connect with previous call from the previous activity.
    @Override
    protected void onServiceConnected(){
        if (getSinchServiceInterface().isStarted()){
            call = getSinchServiceInterface().getCall(mCallId);
        }else{
            getSinchServiceInterface().startClient(callFrom);
            call = getSinchServiceInterface().getCall(mCallId);

        }

        if (call != null) {
            call.addCallListener(new SinchCallListener());
            //animation
            YoYo.with(Techniques.Wobble)
                    .duration(700)
                    .repeat(2)
                    .playOn(callButton);
            getSinchServiceInterface().getSinchClient().getAudioController().enableSpeaker();
            callButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            endCallButton.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            callState = call.getState().toString();


        }

    }

    //run when user is getting a call
    private void gettingCall(){

        Call call = getSinchServiceInterface().getCall(mCallId);

        if (call != null) {
            call.addCallListener(new SinchCallListener());
            callState = "incomingCall";
            mAudioPlayer.playRingtone();
            YoYo.with(Techniques.Wobble)
                    .duration(700)
                    .repeat(6)
                    .playOn(callButton);
            endCallButton.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            callButton.setBackgroundTintList(ColorStateList.valueOf(Color.argb(255,42, 153, 55)));
            callType="incoming";
        }
    }

    //user clicks answer
    private void answerClicked() {

        Call call = getSinchServiceInterface().getCall(mCallId);

        if (call != null) {
            callState = call.getState().toString();
            mAudioPlayer.stopRingtone();
            call.answer();
            getSinchServiceInterface().getSinchClient().getAudioController().enableSpeaker();
            callButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            endCallButton.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        }
    }

    //user declines the call
    private void declineClicked() {

        mAudioPlayer.stopRingtone();
        mAudioPlayer.stopProgressTone();
        if (call != null) {
            call.hangup();
            callButton.setEnabled(true);

        }

        if(callType.equals("incoming")){
            Call call = getSinchServiceInterface().getCall(mCallId);
            if (call != null) {
                call.hangup();
                callButton.setEnabled(true);
                endCallButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                callButton.setBackgroundTintList(ColorStateList.valueOf(Color.argb(255,42, 153, 55)));
            }
        }
        callType="";
    }

    //call listener
    private class SinchCallListener implements CallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            callState = "";
            callType="";
            mAudioPlayer.stopRingtone();
            mAudioPlayer.stopProgressTone();
            callButton.setEnabled(true);
            endCallButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            callButton.setBackgroundTintList(ColorStateList.valueOf(Color.argb(255,42, 153, 55)));
        }

        @Override
        public void onCallEstablished(Call call) {
            mAudioPlayer.stopRingtone();
            mAudioPlayer.stopProgressTone();
            callState = call.getState().toString();
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }

        @Override
        public void onCallProgressing(Call call) {
            mAudioPlayer.playProgressTone();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // Send a push through your push provider here, e.g. GCM
        }
    }


    /**************************************************************************************/
    private void fakeData(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("fake");
        ref.setValue(1).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if(!(getSinchServiceInterface().isStarted())){
                    getSinchServiceInterface().startClient(callFrom);
                }
            }
        });

    }

    /**************************************************************************************/
    //call end on back button
    @Override
    public void onBackPressed() {

        mAudioPlayer.stopProgressTone();
        mAudioPlayer.stopRingtone();
        if (call != null) {
            call.hangup();
        }
        Call call = getSinchServiceInterface().getCall(mCallId);

        if (call != null) {
            call.hangup();
        }

        // Otherwise defer to system default behavior.
        super.onBackPressed();
    }

    /**************************************************************************************/

}
