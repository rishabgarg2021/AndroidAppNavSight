package com.helpyou.itproject;

/*
 * Authors:
 * Uvin Abeysinghe | ID: 789931
 * Tarnvir Grewal  | ID: 838527
 * University of Melbourne
 */


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

/*volunteer waiting for elderly to decide what to do*/
public class HelperWaiting extends BaseActivity {

    static final String TAG = HelperWaiting.class.getSimpleName();


    static final String GOSMWHR = "1";
    static final String MEETUP = "2";
    static final int disconnectTimeLimit = 60000;

    private Button chatButton;
    private Button callButton;
    private Button disconnectButton;
    private String myUid;
    private String elderlyUid;
    private String userType;
    private String mCallId;
    private AudioPlayer mAudioPlayer;
    private Call call;
    private Button endCallButtoon;
    private Button speakerButton;
    private Boolean speakerOn;
    private String requestId;
    private DatabaseReference db;
    private DatabaseReference requestDb;
    private CountDownTimer counterTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_waiting);

        mAudioPlayer = new AudioPlayer(this);


        chatButton = (Button) findViewById(R.id.chatBtn);
        callButton = (Button) findViewById(R.id.callBtn);
        disconnectButton = (Button) findViewById(R.id.disconnectBtn);
        endCallButtoon = (Button) findViewById(R.id.endCallBtn);
        speakerButton = (Button) findViewById(R.id.speakerCallBtn);

        endCallButtoon.setVisibility(View.INVISIBLE);
        speakerButton.setVisibility(View.INVISIBLE);

        Intent previousIntent = getIntent();
        elderlyUid = previousIntent.getStringExtra("elderUid");
        myUid = previousIntent.getStringExtra("helperUid");
        userType = previousIntent.getStringExtra("userType");
        requestId = previousIntent.getStringExtra("requestId");
        speakerOn = false;

        requestDb = FirebaseDatabase.getInstance().getReference("help_req").child(myUid).child(requestId);
        db = FirebaseDatabase.getInstance().getReference("help_req");
        //request permission
        if (ContextCompat.checkSelfPermission(HelperWaiting.this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                HelperWaiting.this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(HelperWaiting.this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE},
                    1);
        }

        fakeData();


        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectUser();
                FirebaseDatabase.getInstance().getReference("userConnect").child(myUid).child("isConnected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        FirebaseDatabase.getInstance().getReference("userConnect").child(elderlyUid).child("isConnected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
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

        //check if still connected, if not disconnect
        db.child(myUid).child(requestId).child("connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if ((dataSnapshot.getValue().toString()).equals("0")) {

                    finish();

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        //check if any of the apps are clossed, if not start a timer.
        db.child(myUid).child(requestId).child("isOneAppClose").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if ((dataSnapshot.getValue().toString()).equals("0") || (dataSnapshot.getValue().toString()).equals("2")) {
                    db.child(myUid).child(requestId).child("isOneAppClose").setValue("1");
                    if (counterTimer != null) {
                        counterTimer.cancel();
                    }


                } else if ((dataSnapshot.getValue().toString()).equals("1")) {
                    counterTimer = new CountDownTimer(disconnectTimeLimit, disconnectTimeLimit) {
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

        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HelperWaiting.this, ChatPop.class);
                intent.putExtra("chat_from_uid", myUid);
                intent.putExtra("chat_to_uid", elderlyUid);
                startActivity(intent);


            }
        });

        //make a call or answer
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fakeData();
                if(getSinchServiceInterface().isStarted()){
                    if (callButton.getText().equals("Answer")) {
                        answerClicked();
                    } else if (callButton.getText().equals("Call")) {
                        Map<String, String> headers = new HashMap<String, String>();
                        //type of call
                        headers.put("type", "Helper");
                        headers.put("IMAGE_URL", "");
                        call = getSinchServiceInterface().callUser(elderlyUid, headers);
                        call.addCallListener(new SinchCallListener());

                        callButton.setText(call.getState().toString());
                        mAudioPlayer.playProgressTone();
                        endCallButtoon.setVisibility(View.VISIBLE);
                        speakerButton.setVisibility(View.VISIBLE);

                        endCallButtoon.setVisibility(View.VISIBLE);

                        // do the calling
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "Client not started yet, press again in few seconds",Toast.LENGTH_LONG).show();
                }



            }
        });



        //end the call
        endCallButtoon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCallButtoon.setVisibility(View.INVISIBLE);
                speakerButton.setVisibility(View.INVISIBLE);
                mAudioPlayer.stopProgressTone();
                mAudioPlayer.stopRingtone();
                speakerOn = false;

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

        //switch speaker
        speakerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (speakerOn) {
                    //switch it off
                    getSinchServiceInterface().getSinchClient().getAudioController().disableSpeaker();
                    speakerOn = false;
                } else {
                    //switch it on
                    getSinchServiceInterface().getSinchClient().getAudioController().enableSpeaker();
                    speakerOn = true;
                }
            }
        });

        //cheak if still connected
        db.child(myUid).child(requestId).child("connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if ((dataSnapshot.getValue().toString()).equals("0")) {

                    finish();

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //check id the elderly selected action, if so which.
        db.child(myUid).child(requestId).child("action").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue().toString().equals(MEETUP)) {
                    //DANISH
                    allow_meetup_permissions();


                } else if (dataSnapshot.getValue().toString().equals(GOSMWHR)) {

                    allow_gosmwhr_permissions();

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("call"));


    }


    //ask for meet up permission from the volunteer
    public void allow_meetup_permissions() {
        // permission was not granted
        //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
        // shouldShowRequestPermissionRationale will return true

        if(((Activity) this).isFinishing())
        {
            return;
        }

        showDialogOK("Press OK TO GET CONNECTED ON MAP",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:


                                db.child(myUid).child(requestId).child("meetAccepted").setValue("1").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //user enables permisssion do your task..

                                        Intent newIntent = new Intent(HelperWaiting.this, showVolunteerRoute.class);
                                        newIntent.putExtra("elderUid", elderlyUid);//testing
                                        newIntent.putExtra("helperUid", myUid);
                                        newIntent.putExtra("userType", "Helper");
                                        newIntent.putExtra("requestId", requestId);

                                        if (call!=null){
                                            newIntent.putExtra(SinchService.CALL_ID, call.getCallId());
                                        }else{

                                            Call call = getSinchServiceInterface().getCall(mCallId);
                                            if (call!=null){
                                                newIntent.putExtra(SinchService.CALL_ID, mCallId);
                                            }

                                        }
                                        startActivity(newIntent);
                                        finish();

                                    }
                                });

                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                db.child(myUid).child(requestId).child("meetAccepted").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        db.child(myUid).child(requestId).child("action").setValue("").addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                            }
                                        });


                                    }
                                });

                                break;
                        }
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        fakeData();
    }

    private void disconnectUser() {

        requestDb.child("connected").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            }
        });

    }
    //ask for go somewhere permission from the volunteer
    public void allow_gosmwhr_permissions() {
        // permission was not granted
        //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
        // shouldShowRequestPermissionRationale will return true
        if(((Activity) this).isFinishing())
        {
            return;
        }
        showDialogOK("Press OK TO GET CONNECTED ON MAP",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:


                                db.child(myUid).child(requestId).child("goSmwhrAccepted").setValue("1").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        Intent i = new Intent(HelperWaiting.this, volSelectDestWaiting.class);
                                        i.putExtra("elderUid", elderlyUid);
                                        i.putExtra("helperUid",myUid );
                                        i.putExtra("userType", "Helper");
                                        i.putExtra("requestId", requestId);
                                        if (call!=null ){
                                            i.putExtra(SinchService.CALL_ID, call.getCallId());
                                        }else{

                                            Call call = getSinchServiceInterface().getCall(mCallId);
                                            if (call!=null ){
                                                i.putExtra(SinchService.CALL_ID, mCallId);
                                            }

                                        }
                                        startActivity(i);
                                        finish();

                                    }
                                });

                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                db.child(myUid).child(requestId).child("goSmwhrAccepted").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        db.child(myUid).child(requestId).child("action").setValue("").addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                            }
                                        });


                                    }
                                });

                                break;
                        }
                    }
                });
    }



    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(HelperWaiting.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }

    /**********************************************************************************************/
    /*Broadcast for calls*/

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
    //code for calls

    @Override
    protected void onServiceConnected(){



    }

    private void gettingCall(){
        Call call = getSinchServiceInterface().getCall(mCallId);


        if (call != null) {
            call.addCallListener(new SinchCallListener());

            endCallButtoon.setVisibility(View.VISIBLE);

            callButton.setText("Answer");
            mAudioPlayer.playRingtone();

        }

    }

    private void answerClicked() {
        Call call = getSinchServiceInterface().getCall(mCallId);

        if (call != null) {
            mAudioPlayer.stopRingtone();
            call.answer();
            callButton.setText(call.getState().toString());

        }
    }


    //call listener
    private class SinchCallListener implements CallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended, cause: " + cause.toString());
            callButton.setText("Call");
            mAudioPlayer.stopRingtone();
            mAudioPlayer.stopProgressTone();
            endCallButtoon.setVisibility(View.INVISIBLE);
            speakerButton.setVisibility(View.INVISIBLE);
            speakerOn=false;


        }

        @Override
        public void onCallEstablished(Call call) {

            Log.d(TAG, "Call established");
            mAudioPlayer.stopProgressTone();
            mAudioPlayer.stopRingtone();
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            callButton.setText(call.getState().toString());
            endCallButtoon.setVisibility(View.VISIBLE);
            speakerButton.setVisibility(View.VISIBLE);



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

    @Override
    public void onStop() {
        super.onStop();
    }

    /**************************************************************************************/
    //call end back button
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
