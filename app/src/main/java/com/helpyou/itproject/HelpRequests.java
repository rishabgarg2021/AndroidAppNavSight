package com.helpyou.itproject;

/*
 * Author: Uvin Abeysinghe
 * Student Id : 789931
 * University of Melbourne
 */

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
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

import java.util.HashMap;

public class HelpRequests extends Activity {

    private DatabaseReference mHelpRequestDatabase;
    private String from;
    private String to;
    private String message;
    private ProgressBar progressBar;
    private TextView timeLeft;
    private String pushKey;
    private Button cancelButt;

    //start request timer Max
    final private int STARTTIME = 45;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_requests);




        progressBar= (ProgressBar)findViewById(R.id.progressBarTime);
        timeLeft=(TextView)findViewById(R.id.timeLeft);
        cancelButt=(Button)findViewById(R.id.cancelButton2);
        mHelpRequestDatabase= FirebaseDatabase.getInstance().getReference().child("help_req");
        Intent previousIntent = getIntent();
        from=previousIntent.getStringExtra("elderUid");
        to=previousIntent.getStringExtra("helperUid");
        message=previousIntent.getStringExtra("message");
        sendFriendReq();

        //timer
        final CountDownTimer myCountDownTimer=new CountDownTimer(45000, 1000) {

            public void onTick(long millisUntilFinished) {
                updateTime(Float.toString(millisUntilFinished / 1000));
                timeLeft.setText(millisUntilFinished / 1000 +"s");
                progressBar.setProgress((int)((millisUntilFinished/1000)*(100/STARTTIME)));
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                timeLeft.setText("");
                finish();
            }

        }.start();


        //listen if the volunteer will help
        mHelpRequestDatabase.child(to).child(pushKey).child("willHelp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue().toString().equals("1")){
                    myCountDownTimer.cancel();
                    helpAccepted();
                    //elderly got connected to volunteer at this point
                    DatabaseReference db_eld = FirebaseDatabase.getInstance().getReference("userConnect").child(from);
                    db_eld.setValue(new UserConnection("1","elderly_vol_main", "Elder", to, pushKey)).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    });

                    DatabaseReference db_eld2 = FirebaseDatabase.getInstance().getReference("userConnect").child(to);
                    db_eld2.setValue(new UserConnection("1","HelperWaiting", "Helper", from, pushKey)).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    });



                };
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //cancel the request
        cancelButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myCountDownTimer.cancel();
                updateTime("0.0");
                Toast.makeText(HelpRequests.this, "Request cancelled", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        //volunteer ignored the request
        mHelpRequestDatabase.child(to).child(pushKey).child("ignored").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue().toString().equals("1")){
                    myCountDownTimer.cancel();
                    updateTime("0.0");
                    Toast.makeText(HelpRequests.this, "Request Ignored", Toast.LENGTH_LONG).show();
                    finish();
                };
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });










    }

    //if help accepted, do this
    private void helpAccepted(){
        Intent newIntent = new Intent(HelpRequests.this, elderly_vol_main.class);
        newIntent.putExtra("elderUid", from);//testing
        newIntent.putExtra("helperUid", to);
        newIntent.putExtra("userType", "Elder");
        newIntent.putExtra("requestId", pushKey);
        startActivity(newIntent);
        finish();
    }


    //update time on firebase
    private void updateTime(String time){
        if (pushKey!=null){
            mHelpRequestDatabase.child(to).child(pushKey).child("expireTime").setValue(time).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                }
            });
        }
    }





    //request info saved on firebase
    private void sendFriendReq(){





        HashMap<String,String> requestData = new HashMap<>();
        requestData.put("from", from);  //from whom the request is coming from
        requestData.put("message",message); //any message they want to send
        requestData.put("willHelp", "0"); //decided to help
        requestData.put("expireTime", Integer.toString(STARTTIME)); //timer
        requestData.put("notified","0"); //notification received
        requestData.put("ignored","0"); //ignored by volunteer
        requestData.put("action","0"); //whch action selected
        requestData.put("meetAccepted",""); //meet accepted
        requestData.put("goSmwhrAccepted",""); // go somewhere accepted
        requestData.put("elderAddedDest",""); //elder adds the destination
        requestData.put("wantsHelpAddingDest",""); //helper needs volunteer to add destination
        requestData.put("volAddedDest",""); //volunteer adds the destination
        requestData.put("connected","0"); //is connected
        requestData.put("isOneAppClose","2"); //is one app closed
        DatabaseReference db = mHelpRequestDatabase.child(to).push();
        pushKey = db.getKey();

        db.setValue(requestData).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        });

    }

}
