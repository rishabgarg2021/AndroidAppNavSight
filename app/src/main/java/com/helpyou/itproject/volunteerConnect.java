
/*
 * Authors:
 *
 * Tarnvir Grewal   | ID:838527
 * Danish Bassi     | ID: 867811
 *
 * Getting nearby volunteer data code written by Tarnvir.
 * Fetching user GPS coordinate code written by Danish.
 *
 */

package com.helpyou.itproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.lang.Math;

public class volunteerConnect extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleApiClient googleApiClient;
    private Location userLocation;
    private static final int topTen = 10;
    private DatabaseReference myref;
    private ArrayList<User> userList = new ArrayList<>();
    private ArrayList<String> userDatabaseIDs = new ArrayList<>();
    double[] topTenNearestVolunteersFinalList = new double[10];
    private Button backToMenuButton;
    double currentUserLat;
    double currentUserLong;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_connect);

        // Find all UI components
        backToMenuButton = findViewById(R.id.btnVolConnectToMain);
        backToMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //Code for getting user location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();

        //Getting the user name and location from firebase
        myUid = getIntent().getStringExtra("from");
        myref = FirebaseDatabase.getInstance().getReference("users");
    }

    // Fetches the data of nearby volunteers
    private void loadAllVolunteers() {

        myref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userList.clear();

                for (DataSnapshot users : dataSnapshot.getChildren()) {

                    User user = users.getValue(User.class);
                    String userDatabaseId = users.getKey();

                    Log.d("myTag", "user received: " +user.getUsername());
                    Log.d("myTag","ID: " +userDatabaseId);

                    if(user.getLatitude() != null && user.getLongitude() != null
                            && !userDatabaseId.equals(myUid) && user.getVolunteering().equals("true")){
                        userList.add(user);
                        userDatabaseIDs.add(userDatabaseId);
                    }
                }

                Log.d("myTag", "Value = "+ Float.toString(userList.size()));
                afterLoadingData();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }


    // Sets up the list adapter and handles on item click events
    private void afterLoadingData(){

        //Method to add items to the list
        String[] topTennearbyVolunteers = getNearByVolunteers();

        ListAdapter nearbyVolunteersAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, topTennearbyVolunteers);
        ListView nearbyVolunteersListView = (ListView) findViewById(R.id.nearbyVolunteersListView);
        nearbyVolunteersListView.setAdapter(nearbyVolunteersAdapter);

        //Making the list items clickable
        nearbyVolunteersListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        String nearByVolunteer = String.valueOf(parent.getItemAtPosition(position));

                        Toast.makeText(volunteerConnect.this, nearByVolunteer, Toast.LENGTH_LONG).show();

                        int selectedUserPosition = (int) topTenNearestVolunteersFinalList[position];
                        User selectedUser = userList.get(selectedUserPosition);
                        String selectUsedDatabaseID = userDatabaseIDs.get(selectedUserPosition);
                        Log.d("myTag", "Selected item = "+ selectUsedDatabaseID);

                        double selectedVolDistance = calculateDistance(Double.parseDouble(selectedUser.getLatitude()),Double.parseDouble(selectedUser.getLongitude()),currentUserLat,currentUserLong,"K");

                        Intent intent = new Intent(volunteerConnect.this, volunteerProfile.class);
                        intent.putExtra("elderUid", myUid );
                        intent.putExtra("volDistance", Double.toString(round(selectedVolDistance,1)));
                        intent.putExtra("helperUid", selectUsedDatabaseID);

                        startActivity(intent);
                        finish();
                    }
                }
        );
    }

    // Retrieves all the volunteers and sorts by ascending order of distance from user
    private String[] getNearByVolunteers(){

        int totalNumberOfVolunteers = userList.size();
        double[][] volunteersDistances = new double[totalNumberOfVolunteers][2];

        for (int i = 0; i < totalNumberOfVolunteers; i++) {
            User user = userList.get(i);
            double volunteerDistance= calculateDistance(Double.parseDouble(user.getLatitude()),Double.parseDouble(user.getLongitude()),currentUserLat,currentUserLong,"K");

            volunteersDistances[i][0] = volunteerDistance;
            volunteersDistances[i][1] = i;
        }

        sortByDistance(volunteersDistances);

        if(totalNumberOfVolunteers<11){
            String[] nearbyVolunteersListDisplayFormat = new String[totalNumberOfVolunteers];

            for(int i = 0; i<totalNumberOfVolunteers;i++){
                int userId =(int) volunteersDistances[i][1];
                topTenNearestVolunteersFinalList[i] = userId;
                nearbyVolunteersListDisplayFormat[i] = userList.get(userId).getUsername() +"  ~  " + Double.toString(round(volunteersDistances[i][0],2)) +" Km ";
            }

            return nearbyVolunteersListDisplayFormat;
        }
        else{
            String[] nearbyVolunteersListDisplayFormat = new String[topTen];

            for(int i = 0;i <topTen; i++){
                int userId =(int) volunteersDistances[i][1];
                topTenNearestVolunteersFinalList[i] = userId;
                nearbyVolunteersListDisplayFormat[i] = userList.get(userId).getUsername() +"  ~  " + Double.toString(round(volunteersDistances[i][0],2)) +" Km ";
            }

            return nearbyVolunteersListDisplayFormat;
        }
    }

    // Function to calculate the distance between two users using coordinates
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        if (unit == "K") {
            dist = dist * 1.609344;
        }
        else if (unit == "N") {
            dist = dist * 0.8684;
        }

        return (dist);
    }


    //  Converts decimal degrees to radians
    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    // Converts radians to decimal degrees
    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    // Round up decimals
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);

        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    // Sort the list by nearest distance
    private void sortByDistance(double[][] arr) {
        int n = arr.length;
        double[] temp = {0.0, 0.0};
        for (int i = 0; i < n; i++) {
            for (int j = 1; j < (n - i); j++) {
                if (arr[j - 1][0] > arr[j][0]) {
                    //swap elements
                    temp = arr[j- 1];
                    arr[j - 1] = arr[j];
                    arr[j] = temp;
                }
            }
        }
    }

    // Checks and asks for location permission
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

    // Connects the Google API Client
    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    // Disconnects the Google API Client
    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    // Handler for when Google API Client is connected and location can be received
    @Override
    public void onConnected(Bundle bundle) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

            if(lastLocation != null){
                currentUserLat = lastLocation.getLatitude();
                currentUserLong = lastLocation.getLongitude();

                TextView locationText = findViewById(R.id.userLocation);
                locationText.setText("My location: " + currentUserLat + ", " + currentUserLong);
                Log.d("myTag", "Lat = "+ currentUserLat+" || Long = "+currentUserLong);

                userLocation = lastLocation;

                myref.child(myUid).child("latitude").setValue(String.valueOf(userLocation.getLatitude()));
                myref.child(myUid).child("longitude").setValue(String.valueOf(userLocation.getLongitude()));

                loadAllVolunteers();
            }
        }
    }

    // Function required due to various interface implementations
    @Override
    public void onConnectionSuspended(int i) {    }

    // Function required due to various interface implementations
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {    }

    // Handler for when device location changes
    @Override
    public void onLocationChanged(Location location) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

            currentUserLat = lastLocation.getLatitude();
            currentUserLong = lastLocation.getLongitude();

            TextView locationText = findViewById(R.id.userLocation);
            locationText.setText("My location: " + currentUserLat + ", " + currentUserLong);

            userLocation = lastLocation;

            myref.child(myUid).child("latitude").setValue(String.valueOf(location.getLatitude()));
            myref.child(myUid).child("longitude").setValue(String.valueOf(location.getLongitude()));
        }
    }

}