package com.helpyou.itproject;

/*
 * Author:
 * Uvin Abeysinghe  | ID : 789931
 * Tarnvir Grewal   | ID : 838527
 * University of Melbourne
 */

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sinch.android.rtc.SinchError;
import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class IncomingHelpRequest extends  BaseActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, SinchService.StartFailedListener {

    private GoogleApiClient googleApiClient;
    private Location userLocation;

    private DatabaseReference myref;
    private Button helpButton;
    private TextView nameText;
    private TextView distanceText;
    private ImageView profilePhoto;
    private String helpUid;
    private String myUid;
    private TextView message;
    private String messageString;
    private String helpUserLatitude;
    private String helpUserLongitude;
    private ProgressBar progressBar;
    private TextView timeLeftView;
    private String noficationId;
    private DatabaseReference requestDb;
    private Button ignoreButton;


    double currentUserLat;
    double currentUserLong;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_help_request);
        helpButton = (Button) findViewById(R.id.helpButton);
        nameText=(TextView) findViewById(R.id.nameView);
        distanceText=(TextView) findViewById(R.id.distanceView);
        ignoreButton = (Button) findViewById(R.id.ignoreButton);
        profilePhoto=(ImageView) findViewById(R.id.imageView);
        message=(TextView) findViewById(R.id.messageText);
        progressBar= (ProgressBar)findViewById(R.id.progressBarTime);
        timeLeftView=(TextView)findViewById(R.id.timeLeft);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildGoogleApiClient();
        }



        Intent intent = getIntent();
        myUid=intent.getStringExtra("myUid");
        helpUid=intent.getStringExtra("helpUid");
        messageString=intent.getStringExtra("message");
        noficationId=intent.getStringExtra("requestId");

        requestDb= FirebaseDatabase.getInstance().getReference("help_req").child(myUid).child(noficationId);

        myref = FirebaseDatabase.getInstance().getReference("users").child(helpUid);


        if (!messageString.equals("")){
            message.setText(messageString);
        }
        //check the expire time
        requestDb.child("expireTime").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Object object = dataSnapshot.getValue();
                String timeLeft =  object.toString();
                progressBar.setProgress((int)((Float.parseFloat(timeLeft))*(100/45)));
                timeLeftView.setText(((int)Float.parseFloat(timeLeft))+"s");
                if (((int)Float.parseFloat(timeLeft))<=0){
                    helpButton.setEnabled(false);
                    Toast.makeText(IncomingHelpRequest.this, "Request Expired", Toast.LENGTH_LONG).show();
                    finish();

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



        //get the user profile info
        myref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                User value = dataSnapshot.getValue(User.class);
                nameText.setText(value.getUsername());
                if (!value.getImageUrl().equals("")){
                    Uri uri = Uri.parse(value.getImageUrl());
                    Picasso.with(IncomingHelpRequest.this ).load(uri).into(profilePhoto);
                }
                else{
                    Picasso.with(IncomingHelpRequest.this ).load(R.drawable.callbg).into(profilePhoto);
                }


                helpUserLatitude = value.getLatitude();
                helpUserLongitude = value.getLongitude();


                double doubleHelpUserLatitude =  Double.parseDouble(helpUserLatitude);
                double doubleHelpUserLongitutde = Double.parseDouble(helpUserLongitude);



                double distanceBetweenUsers =  calculateDistance(currentUserLat,currentUserLong,doubleHelpUserLatitude,doubleHelpUserLongitutde,"K");

                Log.d("myTag", "Lat = "+ currentUserLat+" || Long = "+currentUserLong);

                Log.d("myTag", "Help Lat = "+ helpUserLatitude+" || Help Long = "+helpUserLongitude);
                distanceText.setText(String.valueOf(round(distanceBetweenUsers,1))+" Km");


            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
            }
        });

        ignoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setIgnore();
                finish();
            }
        });



        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setWillHelp();

                setConnected();

                Intent nextIntent = new Intent(IncomingHelpRequest.this,HelperWaiting.class);

                nextIntent.putExtra("userLatLng", userLocation);

                //save the current activities for unexpected disconnects
                DatabaseReference db_eld = FirebaseDatabase.getInstance().getReference("userConnect").child(myUid);
                db_eld.setValue(new UserConnection("1","HelperWaiting", "Helper", helpUid, noficationId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                });
                //save the current activities for unexpected disconnects
                DatabaseReference db_eld2 = FirebaseDatabase.getInstance().getReference("userConnect").child(helpUid);
                db_eld2.setValue(new UserConnection("1","elderly_vol_main", "Elder", myUid, noficationId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                });


                // Added by Danish
                nextIntent.putExtra("elderUid", helpUid);
                nextIntent.putExtra("helperUid", myUid);
                nextIntent.putExtra("userType", "helper");
                nextIntent.putExtra("requestId",noficationId);

                startActivity(nextIntent);
                finish();
            }
        });
//      to make sinch client start
        fakeData();



    }


    private void setWillHelp(){
        requestDb.child("willHelp").setValue("1").addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            }
        });

    }

    private void setIgnore(){
        requestDb.child("ignored").setValue("1").addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            }
        });

    }

    private void setConnected(){
        requestDb.child("connected").setValue("1").addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        });
    }






    // Code to calculate distance between two users

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2, String unit) {

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;


        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        }
        return (dist);


    }



    //  This function converts decimal degrees to radians

    private double deg2rad(double deg) {

        return (deg * Math.PI / 180.0);

    }



    // This function converts radians to decimal degrees

    private double rad2deg(double rad) {

        return (rad * 180.0 / Math.PI);

    }


    public static double round(double value, int places) {

        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);

        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }



    //Code to retrive current User location
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                } else {
                    Toast.makeText(this, "Need your permission for location!", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient != null) {

            googleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if (googleApiClient!=null){
            googleApiClient.disconnect();

        }
        super.onStop();
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);


            if(lastLocation != null){

                currentUserLat = lastLocation.getLatitude();

                currentUserLong = lastLocation.getLongitude();


                Log.d("myTag", "Lat = "+ currentUserLat+" || Long = "+currentUserLong);

                userLocation = lastLocation;

                myref.child(myUid).child("latitude").setValue(String.valueOf(userLocation.getLatitude()));
                myref.child(myUid).child("longitude").setValue(String.valueOf(userLocation.getLongitude()));

            }

        }
    }

    @Override
    public void onConnectionSuspended(int i) {    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {    }

    @Override
    public void onLocationChanged(Location location) {

        currentUserLat = location.getLatitude();
        currentUserLong = location.getLongitude();

        userLocation = location;

        myref.child(myUid).child("latitude").setValue(String.valueOf(location.getLatitude()));
        myref.child(myUid).child("longitude").setValue(String.valueOf(location.getLongitude()));

    }

    private void fakeData(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("fake");
        ref.push().setValue(1).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (!getSinchServiceInterface().isStarted()) {
                    getSinchServiceInterface().startClient(myUid);


                }

            }
        });

    }
    @Override
    public void onStartFailed(SinchError error) {

    }

    @Override
    public void onStarted() {

    }
    protected void onServiceConnected() {

        getSinchServiceInterface().setStartListener(this);

    }
}

