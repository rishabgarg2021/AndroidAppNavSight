
/*
 * Author:
 *
 * Dennis Goyal     | ID: 776980
 *
 */

package com.helpyou.itproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

public class FavouritePlacesActivity extends AppCompatActivity {

    private static final String TAG = "FavouritePlacesActivity" ;
    private String localPlacesLocation;
    private FavouritePlacesService favouritePlacesService;
    CustomPlacesAdapter customListAdapter;
    private Stack<Integer> deletedPlaces = new Stack<>();
    private Button addPlaceButton;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);

        // Set back button click listener
        btnBack = findViewById(R.id.btnPlacesToMainMenuMap);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Get local storage path to save data
        localPlacesLocation = this.getApplicationContext().getFilesDir().getAbsolutePath();
        Log.d(TAG, "Location is " + localPlacesLocation);
        try {
            favouritePlacesService = new FavouritePlacesService(localPlacesLocation);
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        // Set add place on click listener
        addPlaceButton = findViewById(R.id.add_place_button);
        addPlaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askForPlaceToAdd();
            }
        });

        try {
            displayPlacesInTheList();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    // Starts Look Up Place Acitivity
    public void askForPlaceToAdd() {
        Intent intent;
        intent = new Intent(this, LookupPlaceActivity.class);
        startActivityForResult(intent, 1);
    }

    // Gets results passed down from the next activity and adds to the list
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1 && data != null){
            Log.d(TAG, data.getStringExtra("name"));
            String name = data.getStringExtra("name");
            String address = data.getStringExtra("address");
            String lat = data.getStringExtra("lat");
            String lon = data.getStringExtra("lon");
            try {
                addPlaceToList(name, address, lat, lon);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
            customListAdapter.notifyDataSetChanged();
        }
    }

    // Add a place to the list
    public void addPlaceToList(String name, String address, String lat, String lon) throws ParserConfigurationException, TransformerException, IOException, SAXException {
        favouritePlacesService.addPlace(name, address, lat, lon);
    }


    // Display the places on the list adapter
    private void displayPlacesInTheList() throws IOException, SAXException, ParserConfigurationException {
        ListView placesListView = findViewById(R.id.places_list_view);
        customListAdapter = new CustomPlacesAdapter();
        placesListView.setAdapter(customListAdapter);
        placesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Log.d(TAG, "Clicked places activity");
                Intent i = new Intent();
                setResult(3, i);
                try {
                    i.putExtra("lat", favouritePlacesService.getAllPlaces().get(position).lat);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }
                try {
                    i.putExtra("lon", favouritePlacesService.getAllPlaces().get(position).lon);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }
                try {
                    i.putExtra("name", favouritePlacesService.getAllPlaces().get(position).name);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }
                try {
                    i.putExtra("address", favouritePlacesService.getAllPlaces().get(position).address);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }
                finish();
            }
        });
    }

    // Class for the custom list adapter
    class CustomPlacesAdapter extends BaseAdapter {

        // Getter functions
        @Override
        public int getCount() {
            try {
                return favouritePlacesService.getAllPlaces().size();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {

            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = getLayoutInflater().inflate(R.layout.custom_places_layout, null);
            TextView textView_name = (TextView) convertView.findViewById(R.id.placeName);
            if (favouritePlacesService != null){
                try {
                    Log.d(TAG, "name is this: " + favouritePlacesService.getAllPlaces().get(position).getName());
                    textView_name.setText(favouritePlacesService.getAllPlaces().get(position).getName());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }
            }
            Button btnDelete = (Button) convertView.findViewById(R.id.deleteButton);
            btnDelete.setOnClickListener(v -> {
                Log.d(TAG, "Trying to delete");
                try {
                    favouritePlacesService.removePlace(favouritePlacesService.getAllPlaces().get(position));
                    deletedPlaces.add(position);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (TransformerException e) {
                    e.printStackTrace();
                }
                customListAdapter.notifyDataSetChanged();
            });
            return convertView;
        }
    }

    public ArrayList<String> getNames() throws ParserConfigurationException, SAXException, IOException {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<mPlace> places = favouritePlacesService.getAllPlaces();

        for (mPlace place : places){
            names.add(place.getName());
        }
        return getNames();
    }

}

