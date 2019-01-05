
/*
 * Author:
 *
 * Dennis Goyal     | ID:
 *
 */

package com.helpyou.itproject;

import android.content.Intent;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.GoogleMap;

// This class uses the Google Autocomplete fragment and passes a chosen result back to
// the previous activity

public class LookupPlaceActivity extends AppCompatActivity {

    private final String TAG = "TestActivityDebug";
    private GoogleMap mMap;
    private PlaceAutocompleteFragment destinationTextEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_look_up_place);

        Button btnBack = findViewById(R.id.btnSearchToPlaces);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        destinationTextEdit = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment_places);
        // Restrict searches to Australian locations only
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setCountry("AU")
                .build();
        destinationTextEdit.setFilter(typeFilter);
        destinationTextEdit.setHint("Enter destination to add.");
        destinationTextEdit.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(final Place place) {
                int resultCodeGotIT = 1;
                Log.d(TAG, place.getAddress().toString() + place.getLatLng().latitude);
                Intent intent = new Intent();
                setResult(1, intent);
                intent.putExtra("name",  place.getName().toString());
                intent.putExtra("address", place.getAddress().toString());
                intent.putExtra("lat", String.valueOf(place.getLatLng().latitude));
                intent.putExtra("lon", String.valueOf(place.getLatLng().longitude));
                finish();
            }

            @Override
            public void onError(Status status) { }
        });
    }
}


