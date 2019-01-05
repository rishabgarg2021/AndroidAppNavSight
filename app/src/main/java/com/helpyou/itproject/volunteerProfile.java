package com.helpyou.itproject;

/*
 * Authors:
 * Uvin Abeysinghe | ID: 789931
 * Tarnvir Grewal  | ID: 838527
 * University of Melbourne
 */

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class volunteerProfile extends AppCompatActivity {
    private static final String TAG = "volunteerProfile";
    private DatabaseReference myref;
    private Button helpButton;
    private TextView nameText;
    private TextView distanceText;
    private ImageView profilePhoto;
    private String volUid;
    private String myUid;
    private EditText message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_profile);
        helpButton = (Button) findViewById(R.id.helpButton);
        nameText=(TextView) findViewById(R.id.nameView);
        distanceText=(TextView) findViewById(R.id.distanceView);
        Button goBackToSelect = (Button) findViewById(R.id.ignoreButton);
        profilePhoto=(ImageView) findViewById(R.id.imageView);
        message=(EditText)findViewById(R.id.messageInput);
        Intent intent = getIntent();
        volUid = intent.getStringExtra("helperUid");
        myUid=intent.getStringExtra("elderUid");
        distanceText.setText(intent.getStringExtra("volDistance") + " km");

        myref = FirebaseDatabase.getInstance().getReference("users").child(volUid);

        //loading user profile info
        myref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                User value = dataSnapshot.getValue(User.class);
                nameText.setText(value.getUsername());
                if (!value.getImageUrl().equals("")){
                    Uri uri = Uri.parse(value.getImageUrl());
                    Picasso.with(volunteerProfile.this ).load(uri).into(profilePhoto);
                }
                else {
                    Picasso.with(volunteerProfile.this ).load(R.drawable.callbg).into(profilePhoto);
                }

                Log.d(TAG, "Value is: " + value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });


        goBackToSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //accept help
        helpButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Help click");
                Intent help = new Intent(volunteerProfile.this, HelpRequests.class);
                help.putExtra("message", message.getText().toString());
                help.putExtra("elderUid", myUid);
                help.putExtra("helperUid", volUid);
                startActivity(help);
                finish();

            }
        });
    }
}