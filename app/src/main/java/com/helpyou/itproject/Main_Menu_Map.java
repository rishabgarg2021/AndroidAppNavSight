
/*
 * Authors:
 *
 * Danish Bassi     | ID: 867811,
 * Uvin Abeysinghe  | ID: 789931,
 * Rish Garg        | ID:
 *
 * Location and Google Maps code written by Danish.
 * Reconnection code written by Uvin and Rish.
 *
 */

package com.helpyou.itproject;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main_Menu_Map extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private boolean connectionState =false;
    static boolean active = false;
    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location latestLocation;
    private Marker currentUserLocation;
    private DirectionsResult directionsResult;
    private Polyline polyline;
    private Marker destinationMarker;
    private static final int Request_User_Location_Code = 99;
    private Switch isVolunteer;
    private Button btnContacts;
    private Button btnVolunteers;
    private Button btnPlaces;
    private Button btnAR;
    private LatLng destinationLatLng;
    private Boolean searchedPlace = true;
    private static final String KEY_PHONE = "phone";
    private static final String KEY_USERID = "userID";
    private Boolean firstZoom = true;
    private Spinner spinner;
    private LatLng placeLatLng;
    private View menuLayout;
    private PlaceAutocompleteFragment destinationTextEdit;
    private int screenHeight;
    private int[] menuPosition = new int[2];
    private boolean menuToggled = true;
    private DatabaseReference myref;
    private String userId, myphone;
    private String currentModeIsVolunteer="false";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main__menu__map);

        // Get user permission for Location Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            checkUserLocationPermission();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        isVolunteer=findViewById(R.id.volunteer);
        // Get the height of the screen for animation purposes
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        // Get user data passed from login
        Intent i = getIntent();
        spinner = findViewById(R.id.spinner);
        addSpinner();


        // Ask for the required permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED||
            ContextCompat.checkSelfPermission(
                    this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE,Manifest.permission.CAMERA
                    },
                    1);
        }

        // Get the required data passed from previous activity
        myphone = i.getExtras().get("phone").toString();
        userId = i.getExtras().get("userId").toString();

        // Set the firebase reference
        myref = FirebaseDatabase.getInstance().getReference("users").child(userId);

        switchInitialValue();

        // Find all components
        btnAR= findViewById(R.id.btnAR);
        btnContacts = findViewById(R.id.btnContacts);
        btnVolunteers = findViewById(R.id.btnVolunteers);
        btnPlaces = findViewById(R.id.btnPlaces);
        menuLayout = findViewById(R.id.relativeLayout);
        destinationTextEdit = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        // Get location of the menu buttons view for animation purposes
        menuLayout.getLocationOnScreen(menuPosition);

        //checking if the user is already in a connection, if so change the volunteer button red and
        //change the name to the person who it is connected to.
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("userConnect");

        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(userId)){
                    if(db.child(userId).toString() !=null){
                        db.child(userId).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                UserConnection userChild = dataSnapshot.getValue(UserConnection.class);

                                //users are connected and is helper
                                if(userChild.getIsConnected().equals("0")){
                                    connectionState=false;
                                    btnVolunteers.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                                    btnVolunteers.setText("Volunteer");
                                }

                                if(userChild.getIsConnected().equals("1") && userChild.getUserType().equals("Helper"))
                                {
                                    connectionState=true;
                                    btnVolunteers.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                                    FirebaseDatabase.getInstance().getReference("users").child(userChild.getConnectWith()).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if ((dataSnapshot.getValue().toString()!=null)){
                                                btnVolunteers.setText(dataSnapshot.getValue().toString());
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                                    });
                                }

                                if(userChild.getIsConnected().equals("1") && userChild.getUserType().equals("Elder")){
                                    btnVolunteers.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                                    btnVolunteers.setText("Elder");
                                    connectionState=true;
                                    FirebaseDatabase.getInstance().getReference("users").child(userChild.getConnectWith()).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if ((dataSnapshot.getValue().toString()!=null)){
                                                btnVolunteers.setText(dataSnapshot.getValue().toString());
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) { }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        // Go to AR Activity
        btnAR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                viewInAr();
            }
        });

        // Go to Contacts Activity
        btnContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkContactsPermission()){
                    Intent intent = new Intent(Main_Menu_Map.this, Contacts.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("phone", myphone);
                    startActivity(intent);
                }
            }
        });

        // Go to Places Activity
        btnPlaces.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Main_Menu_Map.this, FavouritePlacesActivity.class);
                intent.putExtra("userId", userId);
                intent.putExtra("phone", myphone);
                startActivityForResult(intent, 3);
            }
        });

        // Toggle Volunteer mode
        isVolunteer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                myref.child("volunteering").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        DatabaseReference volRef = myref.child("volunteering");
                        String isVolunt="false";
                        String isTurnOn = "Off";

                        if(isChecked){
                            isVolunt="true";
                            isTurnOn="On";
                            currentModeIsVolunteer="true";
                        }

                        volRef.setValue(isVolunt);
                        currentModeIsVolunteer=isVolunt;
                        Toast.makeText(getApplicationContext(),"Volunteer mode "+isTurnOn,Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                });
            }
        });

        // Go to Volunteers Activity
        btnVolunteers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(connectionState) {
                    connectionResume();
                }
                else {
                    Intent intent = new Intent(Main_Menu_Map.this, volunteerConnect.class);
                    intent.putExtra("from",userId);
                    startActivity(intent);

                }
            }
        });

        // Restrict searches to Australian locations only
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setCountry("AU")
                .build();
        destinationTextEdit.setFilter(typeFilter);
        destinationTextEdit.setHint("Enter your destination");
        destinationTextEdit.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onPlaceSelected(final Place place) {
                // Hide the menu buttons, display AR button and request route polyline
                hideMenuLayout();
                placeLatLng = place.getLatLng();
                btnAR.setVisibility(View.VISIBLE);
                destinationLatLng = place.getLatLng();
                searchedPlace = true;
                showRouteToDestination();
            }

            @Override
            public void onError(Status status) {
                Toast.makeText(Main_Menu_Map.this, status.toString(), Toast.LENGTH_LONG).show();
            }
        });
        destinationTextEdit.getView().findViewById(R.id.place_autocomplete_clear_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (polyline != null && destinationMarker != null) {
                    btnAR.setVisibility(View.INVISIBLE);
                    destinationTextEdit.setText("");
                    polyline.remove();
                    destinationMarker.remove();
                    destinationLatLng =null;
                    placeLatLng=null;
                }
            }
        });
    }

    // Get data from Place Activity and request route polyline
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 3 && data != null) {

            String name = data.getStringExtra("name");
            String lat = data.getStringExtra("lat");
            String lon = data.getStringExtra("lon");

            destinationLatLng = new LatLng(Double.valueOf(lat), Double.valueOf(lon));
            placeLatLng = destinationLatLng;
            destinationTextEdit.setText(name);
            btnAR.setVisibility(View.VISIBLE);
            hideMenuLayout();
            firstZoom = true;
            showRouteToDestination();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    //resume a connection if already in one
    private void connectionResume(){
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("userConnect");

        if(db.child(userId).toString() !=null){
            db.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    UserConnection userChild = dataSnapshot.getValue(UserConnection.class);

                    //users are connected and is helper
                    String elderUid="";
                    String helperUid="";
                    String requestId=userChild.getRequestId();

                    if(!active){
                        return;
                    }

                    if(userChild.getIsConnected().equals("0")){
                        return;
                    }

                    ActivityManager activityManager = (ActivityManager) getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
                    List<ActivityManager.RunningAppProcessInfo> tasks = activityManager.getRunningAppProcesses();
                    for(int i=0;i<tasks.size();i++){
                        System.out.println(tasks.get(i).processName.getClass());
                    }

                    if(this.equals(tasks.get(tasks.size()-1))){
                        return;
                    }


                    if(userChild.getUserType().equals("Elder")){
                        elderUid=userId;
                        helperUid=userChild.getConnectWith();

                    }
                    else{
                        elderUid=userChild.getConnectWith();
                        helperUid=userId;
                    }

                    final String elderUidFinal = elderUid;
                    final String helperUidFinal= helperUid;
                    Intent i;

                    switch (userChild.getLastActivity()){
                        case "elderSelectDestWaiting":
                            i = new Intent(Main_Menu_Map.this,elderSelectDestWaiting.class);
                            i.putExtra("elderUid",elderUid);
                            i.putExtra("helperUid",helperUid);
                            i.putExtra("userType",userChild.getUserType());
                            i.putExtra("requestId",requestId);
                            startActivity(i);
                            break;
                        case "showVolunteerRoute":
                            i = new Intent(Main_Menu_Map.this,showVolunteerRoute.class);
                            i.putExtra("elderUid",elderUid);
                            i.putExtra("helperUid",helperUid);
                            i.putExtra("userType",userChild.getUserType());
                            i.putExtra("requestId",requestId);
                            startActivity(i);
                            break;
                        case "showDestinationRoute":
                            DatabaseReference ref_vol = FirebaseDatabase.getInstance().getReference("help_req").child(helperUid).child(requestId).child("volAddedDest");
                            ref_vol.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    String dest =dataSnapshot.getValue().toString();
                                    if(dest.equals("")){

                                        DatabaseReference ref_eld = FirebaseDatabase.getInstance().getReference("help_req").child(helperUidFinal).child(requestId).child("elderAddedDest");
                                        ref_eld.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                String dest1 =dataSnapshot.getValue().toString();
                                                String[] latlong =  dest1.split(",");
                                                double latitude = Double.parseDouble(latlong[0]);
                                                double longitude = Double.parseDouble(latlong[1]);

                                                LatLng location = new LatLng(latitude, longitude);

                                                Intent i = new Intent(Main_Menu_Map.this,showDestinationRoute.class);
                                                i.putExtra("elderUid",elderUidFinal);
                                                i.putExtra("helperUid",helperUidFinal);
                                                i.putExtra("userType",userChild.getUserType());
                                                i.putExtra("requestId",requestId);
                                                i.putExtra("destinationLatLng",location);
                                                startActivity(i);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) { }
                                        });

                                    }
                                    else {

                                        String[] latlong =  dest.split(",");
                                        double latitude = Double.parseDouble(latlong[0]);
                                        double longitude = Double.parseDouble(latlong[1]);

                                        LatLng location = new LatLng(latitude, longitude);

                                        Intent i = new Intent(Main_Menu_Map.this,showDestinationRoute.class);
                                        i.putExtra("elderUid",elderUidFinal);
                                        i.putExtra("helperUid",helperUidFinal);
                                        i.putExtra("userType",userChild.getUserType());
                                        i.putExtra("requestId",requestId);
                                        i.putExtra("destinationLatLng",location);

                                        startActivity(i);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) { }
                            });
                            break;

                        case "SelectDestinationToShare":
                            i = new Intent(Main_Menu_Map.this,SelectDestinationToShare.class);
                            i.putExtra("elderUid",elderUid);
                            i.putExtra("helperUid",helperUid);
                            i.putExtra("userType",userChild.getUserType());
                            i.putExtra("requestId",requestId);
                            startActivity(i);
                            break;
                        case "volSelectDestWaiting":
                            i = new Intent(Main_Menu_Map.this,volSelectDestWaiting.class);
                            i.putExtra("elderUid",elderUid);
                            i.putExtra("helperUid",helperUid);
                            i.putExtra("userType",userChild.getUserType());
                            i.putExtra("requestId",requestId);
                            startActivity(i);
                            break;
                        case "elderly_vol_main":
                            i = new Intent(Main_Menu_Map.this,elderly_vol_main.class);
                            i.putExtra("elderUid",elderUid);
                            i.putExtra("helperUid",helperUid);
                            i.putExtra("userType",userChild.getUserType());
                            i.putExtra("requestId",requestId);
                            startActivity(i);
                            break;
                        case "HelperWaiting":
                            i = new Intent(Main_Menu_Map.this,HelperWaiting.class);
                            i.putExtra("elderUid",elderUid);
                            i.putExtra("helperUid",helperUid);
                            i.putExtra("userType",userChild.getUserType());
                            i.putExtra("requestId",requestId);
                            startActivity(i);
                            break;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    // Function to pass coordinates to AR Activity
    private void viewInAr(){
        Intent i = new Intent(Main_Menu_Map.this,AR.class);
        i.putExtra("placeLatLng", placeLatLng);
        startActivity(i);
    }


    private void addSpinner(){
        final HintArrayAdapter hintAdapter = new HintArrayAdapter<String>(getApplicationContext(), 0);
        //first one is always Settigns by default.
        hintAdapter.add("Settings");
        hintAdapter.add("Edit Profile");
        hintAdapter.add("Logout");

        spinner.setAdapter(hintAdapter);
        spinner.setSelection(0,false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
             System.out.println("position selected is "+position);
             switch (position) {

                 case 1:
                     Intent i1 = new Intent(Main_Menu_Map.this, userInfo.class);
                     i1.putExtra("userId", userId);
                     i1.putExtra("phone", myphone);
                     startActivity(i1);
                     break;
                 case 2:
                     Toast.makeText(getApplicationContext(),"Logging Out",Toast.LENGTH_SHORT).show();
                     SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                     sp.edit().remove(KEY_PHONE).commit();
                     sp.edit().remove(KEY_USERID).commit();
                     Intent i = new Intent(Main_Menu_Map.this,MainActivity.class);
                     startActivity(i);
                     finish();
                     break;
             }
         }

         @Override
         public void onNothingSelected(AdapterView<?> parent) { }
     });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setPadding(25,300,25,0);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        // Add a marker and move the camera
        LatLng unimelb = new LatLng(-37.7963, 144.9614);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(unimelb, 14.0f));

        // Toggle animation, unfocus search bar and hide soft keyboard
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (menuToggled) {
                    hideMenuLayout();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(findViewById(R.id.constraintLayout).getWindowToken(), 0);
                } else {
                    showMenuLayout();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(findViewById(R.id.constraintLayout).getWindowToken(), 0);
                }
            }
        });
    }

    // Checks and asks for permissions before opening Contacts Activity
    public boolean checkContactsPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 1);
            }
            else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 1);
            }
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_CONTACTS)){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CONTACTS}, 1);
            }
            else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CONTACTS}, 1);
            }
            return false;
        }
        else {
            return true;
        }
    }

    private void switchInitialValue(){

        myref.child("volunteering").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String initialVal = dataSnapshot.getValue().toString();
                if(initialVal.equals("true")) {
                    isVolunteer.setChecked(true);
                    currentModeIsVolunteer="true";
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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

        DatabaseReference mDatabaseRef = FirebaseDatabase.getInstance().getReference("users");
        mDatabaseRef.child(userId).child("latitude").setValue(String.valueOf(location.getLatitude()));
        mDatabaseRef.child(userId).child("longitude").setValue(String.valueOf(location.getLongitude()));

        if (currentUserLocation != null){
            currentUserLocation.remove();
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("My Current Location!");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

        if (destinationMarker != null){
            showRouteToDestination();
        }

        if(firstZoom){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.5f));
            firstZoom = false;
        }
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
    }

    // Function required due to various interface implementations
    @Override
    public void onConnectionSuspended(int i) { }

    // Function required due to various interface implementations
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }

    // Animates to hide menu buttons
    public void hideMenuLayout(){
        ObjectAnimator animation = ObjectAnimator.ofFloat(menuLayout, "translationY", screenHeight);
        animation.setDuration(400);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.start();
        menuToggled = false;
    }

    // Animates to show menu buttons
    public void showMenuLayout(){
        ObjectAnimator animation = ObjectAnimator.ofFloat(menuLayout, "translationY", menuPosition[1]);
        animation.setDuration(400);
        animation.setInterpolator(new OvershootInterpolator(1));
        animation.start();
        menuToggled = true;
    }

    private GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext();
        return geoApiContext.setQueryRateLimit(3)
                .setApiKey(getString(R.string.google_api_key))
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS);
    }

    private class HintArrayAdapter<T> extends ArrayAdapter<T> {

        Context mContext;

        public HintArrayAdapter(Context context, int resource) {
            super(context, resource);
            this.mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
            TextView texview = (TextView) view.findViewById(android.R.id.text1);

            //by default show the 0th text which is hint.
            texview.setText("");
            texview.setHint(getItem(0).toString());

            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View view;

            if(position == 0){
                view = inflater.inflate(R.layout.spinner_hint_list_item_layout, parent, false); // Hide first row
            } else {
                view = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
                TextView texview = (TextView) view.findViewById(android.R.id.text1);
                texview.setText(getItem(position).toString());
            }

            return view;
        }
    }

    // Displays the route to a destination
    @SuppressLint("StaticFieldLeak")
    private void showRouteToDestination() {

        if (mMap != null && destinationLatLng != null && latestLocation != null) {
            // Async request to get the polyline of the route
            new AsyncTask<Void, Void, DirectionsResult>() {

                // Gets the route in a background thread
                @Override
                protected DirectionsResult doInBackground(Void... voids) {
                    try {

                        directionsResult = DirectionsApi.newRequest(getGeoContext())
                                .origin(new com.google.maps.model.LatLng(latestLocation.getLatitude(), latestLocation.getLongitude()))
                                .destination(new com.google.maps.model.LatLng(destinationLatLng.latitude, destinationLatLng.longitude))
                                .mode(TravelMode.WALKING)
                                .await();

                        System.out.println("RESULT: " + directionsResult.toString());
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
                    if(result == null || result.routes.length == 0){
                        return;
                    }

                    // Decodes the array into polyline
                    List<LatLng> decodedPath = PolyUtil.decode(result.routes[0].overviewPolyline.getEncodedPath());

                    System.out.println("DECODE: " + result.routes.toString());

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
                    builder.include(new LatLng(destinationLatLng.latitude, destinationLatLng.longitude));
                    builder.include(new LatLng(latestLocation.getLatitude(), latestLocation.getLongitude()));
                    LatLngBounds bounds = builder.build();

                    // Animate the map to view the polyline using the LatLng Bounds
                    if (firstZoom){
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 200);
                        mMap.animateCamera(cu);
                        firstZoom = false;
                    }
                    else if (searchedPlace){
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 200);
                        mMap.animateCamera(cu);
                        searchedPlace = false;
                    }
                }
            }.execute();
        }
    }
}