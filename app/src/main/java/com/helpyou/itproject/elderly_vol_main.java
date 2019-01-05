package com.helpyou.itproject;

/*
 * Authors:
 * Uvin Abeysinghe | ID: 789931
 * Tarnvir Grewal  | ID: 838527
 * University of Melbourne
 */


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Landing page for the elderly person when connected with the volunteer.
 */

public class elderly_vol_main extends BaseActivity {

    static final String TAG = elderly_vol_main.class.getSimpleName();

    static final String GOSMWHR="1";
    static final String MEETUP="2";

    static final int disconnectTimeLimit = 60000;



    private Button chatButton;
    private Button callButton;
    private Button meetButton;
    private Button goSmwhrButton;
    private Button disconnectButton;
    private String myUid;
    private String volUid;
    private String mCallId;
    private AudioPlayer mAudioPlayer;
    private Call call;
    private String userType;
    private Button speakerButton;
    private Button endCallButton;
    private Boolean speakerOn;
    private String requestId;
    private DatabaseReference db;
    private CountDownTimer counterTimer;
    boolean callPassed=false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elderly_vol_main);
        chatButton = (Button) findViewById(R.id.chatBtn);
        callButton = (Button) findViewById(R.id.callBtn);
        meetButton = (Button) findViewById(R.id.meetBtn);
        goSmwhrButton = (Button) findViewById(R.id.goSmwhrBtn);
        disconnectButton = (Button) findViewById(R.id.disconnectBtn);
        speakerButton =(Button) findViewById(R.id.speakerBttn);
        endCallButton = (Button) findViewById(R.id.endCallBttn);

        Intent previousIntent =getIntent();
        myUid=previousIntent.getStringExtra("elderUid");
        volUid=previousIntent.getStringExtra("helperUid");
        userType=previousIntent.getStringExtra("userType")   ;
        requestId=previousIntent.getStringExtra("requestId");

        mAudioPlayer = new AudioPlayer(this);
        speakerOn=false;



        db = FirebaseDatabase.getInstance().getReference("help_req");

        //asks the permission from user to record or make calls.
        if (ContextCompat.checkSelfPermission(elderly_vol_main.this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                elderly_vol_main.this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(elderly_vol_main.this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE},
                    1);
        }


        fakeData();

        //Checks if still connected
        db.child(volUid).child(requestId).child("connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if((dataSnapshot.getValue().toString()).equals("0")){

                    finish();

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fakeData();
                if (getSinchServiceInterface().isStarted()){
                    if (callButton.getText().equals("Answer")){
                        answerClicked();
                    }
                    else if (callButton.getText().equals("Call")){
                        Map<String, String> headers = new HashMap<String, String>();
                        //type of call
                        headers.put("type","Helper");
                        headers.put("IMAGE_URL","" );

                        call = getSinchServiceInterface().callUser(volUid, headers);
                        call.addCallListener(new SinchCallListener());


                        callButton.setText(call.getState().toString());
                        mAudioPlayer.playProgressTone();

                        endCallButton.setVisibility(View.VISIBLE);
                        // do the calling
                    }

                }else{
                    Toast.makeText(getApplicationContext(), "Client not started yet, press again in few seconds",Toast.LENGTH_LONG).show();

                }


            }
        });

        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(elderly_vol_main.this, ChatPop.class);

                intent.putExtra("chat_from_uid",myUid);
                intent.putExtra("chat_to_uid",volUid);
                startActivity(intent);






            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectUser();
                FirebaseDatabase.getInstance().getReference("userConnect").child(myUid).child("isConnected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        FirebaseDatabase.getInstance().getReference("userConnect").child(volUid).child("isConnected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

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
                                finish();

                            }
                        });

                    }
                });

            }
        });

        //check if one of the apps is closed
        db.child(volUid).child(requestId).child("isOneAppClose").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if((dataSnapshot.getValue().toString()).equals("1")||(dataSnapshot.getValue().toString()).equals("2")){
                    db.child(volUid).child(requestId).child("isOneAppClose").setValue("0");
                    if(counterTimer!=null){
                        counterTimer.cancel();
                    }




                }
                else if((dataSnapshot.getValue().toString()).equals("0")){
                    counterTimer= new CountDownTimer(disconnectTimeLimit,disconnectTimeLimit) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            finish();
                        }
                    }.start();





                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        meetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.child(volUid).child(requestId).child("action").setValue(MEETUP).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        ProgressBar progess = findViewById(R.id.progressBar7);
                        progess.setVisibility(View.VISIBLE);

                        TextView waitingText = findViewById(R.id.waitingForResponseText);
                        waitingText.setVisibility(View.VISIBLE);
                    }
                });



            }
        });

        //check if the volunteer accepted to meet, if so , create the new activity and finish this,if not reset the values.
        db.child(volUid).child(requestId).child("meetAccepted").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if((dataSnapshot.getValue().toString()).equals("1")){

                    ProgressBar progess = findViewById(R.id.progressBar7);
                    progess.setVisibility(View.INVISIBLE);

                    TextView waitingText = findViewById(R.id.waitingForResponseText);
                    waitingText.setVisibility(View.INVISIBLE);

                    Intent newIntent = new Intent(elderly_vol_main.this, showVolunteerRoute.class);
                    newIntent.putExtra("elderUid", myUid);//testing
                    newIntent.putExtra("helperUid", volUid);
                    newIntent.putExtra("userType", "Elder");
                    newIntent.putExtra("requestId", requestId);


                    if (call!=null ){
                        newIntent.putExtra(SinchService.CALL_ID, call.getCallId());
                        callPassed=true;
                    }else{

                        Call call = getSinchServiceInterface().getCall(mCallId);
                        if (call!=null ){
                            newIntent.putExtra(SinchService.CALL_ID, mCallId);
                            callPassed=true;
                        }

                    }
                    startActivity(newIntent);
                    finish();

                }else if((dataSnapshot.getValue().toString()).equals("0")){

                    ProgressBar progess = findViewById(R.id.progressBar7);
                    progess.setVisibility(View.INVISIBLE);

                    TextView waitingText = findViewById(R.id.waitingForResponseText);
                    waitingText.setVisibility(View.INVISIBLE);

                    Toast.makeText(getApplicationContext(), "The volunteer denied to meet, Might Chat or Call Help You!",Toast.LENGTH_LONG).show();
                    db.child(volUid).child(requestId).child("meetAccepted").setValue("").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    });


                }else if ((dataSnapshot.getValue().toString()).equals("")){

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //check if the volunteer accepted to help elderly go somewhere, if so , create the new activity and finish this,if not reset the values.
        db.child(volUid).child(requestId).child("goSmwhrAccepted").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if((dataSnapshot.getValue().toString()).equals("1")){

                    ProgressBar progess = findViewById(R.id.progressBar7);
                    progess.setVisibility(View.INVISIBLE);

                    TextView waitingText = findViewById(R.id.waitingForResponseText);
                    waitingText.setVisibility(View.INVISIBLE);

                    Intent i = new Intent(elderly_vol_main.this, SelectDestinationToShare.class);
                    i.putExtra("elderUid", myUid);
                    i.putExtra("helperUid", volUid);
                    i.putExtra("userType", "Elder");
                    i.putExtra("requestId", requestId);
                    if (call!=null){
                        i.putExtra(SinchService.CALL_ID, call.getCallId());
                    }else{

                        Call call = getSinchServiceInterface().getCall(mCallId);
                        if (call!=null){
                            i.putExtra(SinchService.CALL_ID, mCallId);

                        }

                    }
                    startActivity(i);
                    finish();

                }else if((dataSnapshot.getValue().toString()).equals("0")){

                    ProgressBar progess = findViewById(R.id.progressBar7);
                    progess.setVisibility(View.INVISIBLE);

                    TextView waitingText = findViewById(R.id.waitingForResponseText);
                    waitingText.setVisibility(View.INVISIBLE);

                    Toast.makeText(getApplicationContext(), "The volunteer denied to help you go somewhere, Might Chat or Call Help You!",Toast.LENGTH_LONG).show();
                    db.child(volUid).child(requestId).child("goSmwhrAccepted").setValue("").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    });


                }else if ((dataSnapshot.getValue().toString()).equals("")){

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        goSmwhrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.child(volUid).child(requestId).child("action").setValue(GOSMWHR).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        ProgressBar progess = findViewById(R.id.progressBar7);
                        progess.setVisibility(View.VISIBLE);

                        TextView waitingText = findViewById(R.id.waitingForResponseText);
                        waitingText.setVisibility(View.VISIBLE);
                    }
                });

            }
        });

        speakerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (speakerOn){
                    //switch it off
                    getSinchServiceInterface().getSinchClient().getAudioController().disableSpeaker();

                    speakerOn=false;
                }else{
                    //switch it on
                    getSinchServiceInterface().getSinchClient().getAudioController().enableSpeaker();

                    speakerOn=true;
                }
            }
        });

        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakerOn=false;
                endCallButton.setVisibility(View.INVISIBLE);
                speakerButton.setVisibility(View.INVISIBLE);
                mAudioPlayer.stopProgressTone();
                mAudioPlayer.stopRingtone();
                if (call != null) {
                    call.hangup();
                }
                Call call = getSinchServiceInterface().getCall(mCallId);



                if (call != null) {
                    call.hangup();
                }
                callButton.setText("Call");
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("call"));


    }

    //disconnects the user
    private void disconnectUser() {
        db.child(volUid).child(requestId).child("connected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            }
        });
    }



    /**********************************************************************************************/
    /*Broadcast receiver for the call*/
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

    private void fakeData(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("fake");
        ref.setValue(1).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if(!(getSinchServiceInterface().isStarted())){
                    getSinchServiceInterface().startClient(myUid);

                }


            }
        });

    }


    /**********************************************************************************************/
    //Calls

    @Override
    protected void onServiceConnected(){

    }

    //run when a call is received from the broadcast
    private void gettingCall(){


        Call call = getSinchServiceInterface().getCall(mCallId);

        if (call != null) {
            call.addCallListener(new SinchCallListener());
            callButton.setText("Answer");
            mAudioPlayer.playRingtone();
            endCallButton.setVisibility(View.VISIBLE);


        }

    }


    private void answerClicked() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        callButton.setText(call.getState().toString());
        if (call != null) {
            mAudioPlayer.stopRingtone();
            call.answer();
        }
    }


    // call listener to have control over the calls
    private class SinchCallListener implements CallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            callButton.setText("Call");
            Log.d(TAG, "Call ended, cause: " + cause.toString());
            mAudioPlayer.stopRingtone();
            mAudioPlayer.stopProgressTone();
            endCallButton.setVisibility(View.INVISIBLE);
            speakerButton.setVisibility(View.INVISIBLE);
            speakerOn=false;

        }

        @Override
        public void onCallEstablished(Call call) {

            Log.d(TAG, "Call established");
            mAudioPlayer.stopRingtone();
            mAudioPlayer.stopProgressTone();
            callButton.setText(call.getState().toString());
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            speakerButton.setVisibility(View.VISIBLE);
            endCallButton.setVisibility(View.VISIBLE);


        }

        @Override
        public void onCallProgressing(Call call) {


            mAudioPlayer.playProgressTone();
            Log.d(TAG, "Call progressing");
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // Send a push through your push provider here, e.g. GCM
        }
    }




    /**************************************************************************************/

    @Override
    public void onStart() {
        super.onStart();
        fakeData();
    }
    @Override
    public void onStop() {
        super.onStop();
    }

    /**************************************************************************************/
    //end the call when back pressed
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
