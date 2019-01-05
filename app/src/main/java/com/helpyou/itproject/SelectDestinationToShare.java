package com.helpyou.itproject;

/*
 * Author: Uvin Abeysinghe
 * Student Id : 789931
 * University of Melbourne
 */

import android.annotation.SuppressLint;
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
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;

import com.google.android.gms.maps.model.LatLng;

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

public class SelectDestinationToShare extends BaseActivity {

    private PlaceAutocompleteFragment destinationTextEdit;
    private TextView address;
    private LatLng destinationLatLng;
    private String elderUid;
    private String helperUid;

    private Button disconnectBttn;
    private Button btnNext;
    private String requestId;
    private String userType;
    private Button btnRemoteControl;
    private FloatingActionButton chatButton;
    static final int disconnectTimeLimit = 60000;
    private CountDownTimer counterTimer;
    private DatabaseReference db_disconnect_on_timer;
    boolean callPassed=false;


    /*************************************/
    private String callState;
    private FloatingActionButton callButton;
    private FloatingActionButton endCallButton;
    private AudioPlayer mAudioPlayer;
    private Call call;
    private String callTo;
    private String mCallId;
    private String callType="";
    private DatabaseReference db;
    private String callFrom;

    /*************************************/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_destination_to_share);

        Intent i = getIntent();
        elderUid = i.getExtras().get("elderUid").toString();
        helperUid = i.getExtras().get("helperUid").toString();
        userType = i.getStringExtra("userType");
        requestId= i.getStringExtra("requestId");
        db= FirebaseDatabase.getInstance().getReference().child("help_req");
        db_disconnect_on_timer = FirebaseDatabase.getInstance().getReference().child("help_req");


        if((getIntent().getStringExtra(SinchService.CALL_ID))!=null){
            mCallId=getIntent().getStringExtra(SinchService.CALL_ID);


        }

        chatButton = findViewById(R.id.chatButton);

        if (userType.equals("Elder")){
            callTo=helperUid;
            callFrom=elderUid;

        }else{
            callTo=elderUid;
            callFrom=helperUid;
        }
        fakeData();

        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMiniChat();
            }
        });

        /********************************************/

        //checking if the other app is disconnected
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

        //disconnect form the connection
        disconnectBttn = findViewById(R.id.diconnectBttn);
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
                                finish();
                            }
                        });
                    }
                });
            }
        });

        /****************************************/

        //checking if the one app close timer is run out
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
                            public void onTick(long millisUntilFinished) {

                            }

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
                                                    public void onSuccess(Void aVoid) {
                                                    }
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
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
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
                            public void onTick(long millisUntilFinished) {

                            }

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
                                                    public void onSuccess(Void aVoid) {
                                                    }
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
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }


        /****************************************/
        //saving the current state incase of unexpected disconnection
        if(userType.equals("Helper")){
            DatabaseReference db_eld = FirebaseDatabase.getInstance().getReference("userConnect").child(helperUid);
            db_eld.setValue(new UserConnection("1","SelectDestinationToShare", "Helper", elderUid, requestId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                }
            });

            DatabaseReference db_eld2 = FirebaseDatabase.getInstance().getReference("userConnect").child(elderUid);
            db_eld2.setValue(new UserConnection("1","elderSelectDestWaiting", "Elder", helperUid, requestId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                }
            });

        }
        else{
            DatabaseReference db_eld = FirebaseDatabase.getInstance().getReference("userConnect").child(elderUid);
            db_eld.setValue(new UserConnection("1","SelectDestinationToShare", "Elder", helperUid, requestId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                }
            });

            DatabaseReference db_eld2 = FirebaseDatabase.getInstance().getReference("userConnect").child(helperUid);
            db_eld2.setValue(new UserConnection("1","volSelectDestWaiting", "Helper", elderUid, requestId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                }
            });


        }

        btnNext = findViewById(R.id.btnShowDestination);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(destinationLatLng != null){
                    if (userType.equals("Elder")){
                        //send data to firebase
                        db.child(helperUid).child(requestId).child("elderAddedDest").setValue(destinationLatLng.latitude+","+destinationLatLng.longitude).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {


                            }
                        });

                    }else{
                        //send data to firebase
                        db.child(helperUid).child(requestId).child("volAddedDest").setValue(destinationLatLng.latitude+","+destinationLatLng.longitude).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                            }
                        });


                    }
                    Intent intent = new Intent(SelectDestinationToShare.this, showDestinationRoute.class);
                    intent.putExtra("destinationLatLng", destinationLatLng);
                    intent.putExtra("elderUid", elderUid);
                    intent.putExtra("requestId", requestId);
                    intent.putExtra("helperUid", helperUid);
                    intent.putExtra("userType", userType);
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
                else {
                    Toast.makeText(SelectDestinationToShare.this, "Please enter a destination",
                            Toast.LENGTH_LONG).show();
                }

            }
        });
        btnRemoteControl = findViewById(R.id.remoteAccessButton);
        if (userType.equals("Helper")){
            btnRemoteControl.setEnabled(false);
        }
        btnRemoteControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.child(helperUid).child(requestId).child("wantsHelpAddingDest").setValue("1").addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Intent i = new Intent(SelectDestinationToShare.this, elderSelectDestWaiting.class);
                        i.putExtra("elderUid", elderUid);
                        i.putExtra("helperUid",helperUid );
                        i.putExtra("userType", userType);
                        i.putExtra("requestId", requestId);
                        if (call!=null){
                            i.putExtra(SinchService.CALL_ID, call.getCallId());
                            callPassed=true;
                        }else{

                            Call call = getSinchServiceInterface().getCall(mCallId);
                            if (call!=null ){
                                i.putExtra(SinchService.CALL_ID, mCallId);
                                callPassed=true;
                            }

                        }

                        startActivity(i);
                        finish();
                    }
                });
            }
        });


        address = findViewById(R.id.txtAddress);

        destinationTextEdit = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment2);

        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setCountry("AU")
                .build();
        destinationTextEdit.setFilter(typeFilter);

        destinationTextEdit.setHint("Enter your destination");
        destinationTextEdit.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onPlaceSelected(final Place place) {
                address.setText(place.getAddress());
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {

            }
        });


        /*************************************/
        //calling code
        mAudioPlayer = new AudioPlayer(this);


        if (userType.equals("Elder")){
            callTo=helperUid;
        }else{
            callTo=elderUid;
        }

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



                        // do the calling
                    }else if(callState.equals("incomingCall")){
                        answerClicked();
                    }

                }else{

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

    }

    //open the chat
    private void openMiniChat(){
        String callFrom;
        if (userType.equals("Elder")){
            callTo=helperUid;
            callFrom=elderUid;
        }else{
            callTo=elderUid;
            callFrom=helperUid;

        }

        Intent intent = new Intent(SelectDestinationToShare.this,ChatPop.class);
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
    //started if a call was passed
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

    //run when getting a call
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

    //run when the call is answered
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

    //run when the call is declined
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
    //end call on back pressed
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
