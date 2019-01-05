
/*
 * Authors:
 *
 * Danish Bassi     | ID: 867811,
 * Uvin Abeysinghe  | ID: 789931
 *
 * Location and Google Maps code written by Danish.
 * Calling (Sinch) and reconnection code written by Uvin.
 *
 */

package com.helpyou.itproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class showVolunteerRoute extends BaseActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location latestLocation;
    private Marker currentUserLocation;
    private DirectionsResult directionsResult;
    private Polyline polyline;
    private Marker destinationMarker;
    private static final int Request_User_Location_Code = 99;
    private FloatingActionButton chatButton;
    private DatabaseReference myref;
    private String helperUid;
    private String elderUid;
    private LatLng helperLocation;
    private LatLng elderLocation;
    private Boolean firstZoom = true;
    private String userType;
    private String callFrom;
    static final int disconnectTimeLimit = 60000;
    private DatabaseReference db_disconnect_on_timer;
    private CountDownTimer counterTimer;
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
        setContentView(R.layout.activity_show_volunteer_route);

        // Find all UI components
        Button btnBack = findViewById(R.id.btnBackToMain);
        chatButton = findViewById(R.id.chatButton);
        callButton = findViewById(R.id.callBttn);
        endCallButton = findViewById(R.id.endCallBttn);

        // Get disconnection timer reference
        db_disconnect_on_timer = FirebaseDatabase.getInstance().getReference().child("help_req");

        // Get user IDs through intent to obtain location data from firebase
        Intent i = getIntent();
        helperUid = (String) i.getExtras().get("helperUid");
        elderUid = (String) i.getExtras().get("elderUid");
        userType = i.getStringExtra("userType");
        String requestId =  i.getStringExtra("requestId");

        /*****************************************************************/
        if((getIntent().getStringExtra(SinchService.CALL_ID))!=null){
            mCallId=getIntent().getStringExtra(SinchService.CALL_ID);
        }
        /*****************************************************************/

        //saving the connection state in case of accidental disconnections
        if(userType.equals("Helper")){

            DatabaseReference db_eld = FirebaseDatabase.getInstance().getReference("userConnect").child(helperUid);
            db_eld.setValue(new UserConnection("1","showVolunteerRoute", "Helper", elderUid, requestId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                }
            });

            DatabaseReference db_eld2 = FirebaseDatabase.getInstance().getReference("userConnect").child(elderUid);
            db_eld2.setValue(new UserConnection("1","showVolunteerRoute", "Elder", helperUid, requestId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                }
            });
        }
        else{

            DatabaseReference db_eld = FirebaseDatabase.getInstance().getReference("userConnect").child(elderUid);
            db_eld.setValue(new UserConnection("1","showVolunteerRoute", "Elder", helperUid, requestId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                }
            });

            DatabaseReference db_eld2 = FirebaseDatabase.getInstance().getReference("userConnect").child(helperUid);
            db_eld2.setValue(new UserConnection("1","showVolunteerRoute", "Helper", elderUid, requestId)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                }
            });
        }


        //Call code
        /*************************************/

        mAudioPlayer = new AudioPlayer(this);

        if (userType.equals("Elder")){
            callTo=helperUid;
            callFrom=elderUid;
        }else{
            callTo=elderUid;
            callFrom=helperUid;
        }

        // Required due to Sinch client not starting bug
        fakeData();

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
                        headers.put("type","Helper");
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

        // Close activity on back button press
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        /********************************************/

        //checking if closed
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

        //disconnecting the connection
        Button btnDone = findViewById(R.id.btnVolConnectToMain);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                declineClicked();

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

        //disconnection time limit
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
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }

        /****************************************/

        // Get user permission for Location Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            checkUserLocationPermission();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMiniChat();
            }
        });
    }

    // Starts a chat activity
    private void openMiniChat(){
        Intent intent = new Intent(showVolunteerRoute.this,ChatPop.class);
        intent.putExtra("chat_from_uid",callFrom);
        intent.putExtra("chat_to_uid",callTo);
        startActivity(intent);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("StaticFieldLeak")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setPadding(25, 300, 25, 0);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        // Move the camera to Unimelb initially
        LatLng unimelb = new LatLng(-37.7963, 144.9614);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(unimelb, 14.0f));
    }

    // Checks and asks for location permission
    public boolean checkUserLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Request_User_Location_Code);
            }
            else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Request_User_Location_Code);
            }
            return false;
        }
        else {
            return true;
        }
    }

    // Builds Google API Client for location detection
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case Request_User_Location_Code:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        if (googleApiClient == null){
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    // Handler for when device location changes
    @Override
    public void onLocationChanged(Location location) {
        latestLocation = location;

        String userType = getIntent().getStringExtra("userType");
        if(userType.equals("Elder")) {
            myref.child(elderUid).child("latitude").setValue(String.valueOf(location.getLatitude()));
            myref.child(elderUid).child("longitude").setValue(String.valueOf(location.getLongitude()));
        }
        else {
            myref.child(helperUid).child("latitude").setValue(String.valueOf(location.getLatitude()));
            myref.child(helperUid).child("longitude").setValue(String.valueOf(location.getLongitude()));
        }

        if (currentUserLocation != null){
            currentUserLocation.remove();
        }

        showRouteToVol();
    }

    // Handler for when Google API Client is connected and location can be received
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();

        locationRequest.setInterval(1100);
        locationRequest.setFastestInterval(1100);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }

        myref = FirebaseDatabase.getInstance().getReference("users");

        final DatabaseReference helperRef = myref.child(helperUid);
        helperRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User helper = dataSnapshot.getValue(User.class);
                helperLocation = new LatLng(Double.parseDouble(helper.getLatitude()),
                        Double.parseDouble(helper.getLongitude()));
                showRouteToVol();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        final DatabaseReference elderRef = myref.child(elderUid);
        elderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User elder = dataSnapshot.getValue(User.class);
                elderLocation = new LatLng(Double.parseDouble(elder.getLatitude()),
                        Double.parseDouble(elder.getLongitude()));
                showRouteToVol();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    // Function required due to various interface implementations
    @Override
    public void onConnectionSuspended(int i) { }

    // Function required due to various interface implementations
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }

    private GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext();
        return geoApiContext.setQueryRateLimit(3)
                .setApiKey(getString(R.string.google_api_key))
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS);
    }

    // Displays the route to the volunteer
    @SuppressLint("StaticFieldLeak")
    private void showRouteToVol() {
        if (mMap != null && elderLocation != null && helperLocation != null) {
            // Async request to get the polyline of the route
            new AsyncTask<Void, Void, DirectionsResult>() {

                // Gets the route in a background thread
                @Override
                protected DirectionsResult doInBackground(Void... voids) {
                    try {

                        // Check if elder or helper is requesting route
                        String userType = getIntent().getStringExtra("userType");
                        if (userType.equals("Elder")) {
                            directionsResult = DirectionsApi.newRequest(getGeoContext())
                                    .origin(new com.google.maps.model.LatLng(elderLocation.latitude, elderLocation.longitude))
                                    .destination(new com.google.maps.model.LatLng(helperLocation.latitude, helperLocation.longitude))
                                    .mode(TravelMode.WALKING)
                                    .await();
                        } else {
                            directionsResult = DirectionsApi.newRequest(getGeoContext())
                                    .origin(new com.google.maps.model.LatLng(helperLocation.latitude, helperLocation.longitude))
                                    .destination(new com.google.maps.model.LatLng(elderLocation.latitude, elderLocation.longitude))
                                    .mode(TravelMode.WALKING)
                                    .await();
                        }

                        return directionsResult;
                    } catch (Exception e) {
                        Log.e("Poly", e.toString());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(DirectionsResult result) {
                    super.onPostExecute(result);

                    // Exits function if no result is received
                    if (result == null || result.routes.length == 0){
                        return;
                    }

                    // Decodes the array into polyline
                    List<LatLng> decodedPath = PolyUtil.decode(result.routes[0].overviewPolyline.getEncodedPath());

                    // Clear any existing polyline and marker
                    if (polyline != null && destinationMarker != null) {
                        polyline.remove();
                        destinationMarker.remove();
                    }

                    // Add new polyline
                    polyline = mMap.addPolyline(new PolylineOptions()
                            .width(10)
                            .color(Color.BLUE)
                            .addAll(decodedPath));

                    // Add new marker
                    destinationMarker = mMap.addMarker(new MarkerOptions().
                            position(new LatLng(directionsResult.routes[0].legs[0].endLocation.lat,
                                    directionsResult.routes[0].legs[0].endLocation.lng)));

                    // Builder for LatLngBounds to ensure origin and destination are in view
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(new LatLng(elderLocation.latitude, elderLocation.longitude));
                    builder.include(new LatLng(helperLocation.latitude, helperLocation.longitude));
                    LatLngBounds bounds = builder.build();

                    // Animate the map to view the polyline using the LatLng Bounds
                    if(firstZoom == true){
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 200);
                        mMap.animateCamera(cu);
                        firstZoom = false;
                    }
                }
            }.execute();
        }
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

    //starting incoming calls
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

    //when getting a call run this
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

    //phone answer clicked
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

    //phone decline clicked
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

    //sinch call listener
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

    private void endCall() {
        mAudioPlayer.stopProgressTone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.hangup();
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

    //call ends when back button pressed
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