
/*
 * Author:
 *
 * Dennis Goyal     | ID:776980
 *
 */

package com.helpyou.itproject;

import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;

public class FavouritePlacesService {

    private static final String TAG = "FavouritePlacesService";
    private LocalPlacesStorage localPlacesStorage;
    private ArrayList<mPlace> places;

    // Get local storage path to get data
    public FavouritePlacesService(String localPlacesLocation) throws TransformerException, ParserConfigurationException, IOException, SAXException {
        localPlacesStorage = new LocalPlacesStorage(localPlacesLocation);
        places = localPlacesStorage.getPlaces();
        if (places == null){
            places = new ArrayList<>();
        }
    }

    // Deletes a place from the list
    public void removePlace(mPlace place) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        ArrayList<mPlace> places = localPlacesStorage.getPlaces();
        mPlace tmpPlaceToRemove = null;
        for(mPlace tplace : places){
            if (tplace.getName().equals(place.getName())){
                tmpPlaceToRemove = tplace;
                break;
            }
        }
        places.remove(tmpPlaceToRemove);
        System.out.println("new places size " + places.size());
        localPlacesStorage.refreshInTheDatabase(places);
    }

    // Retrieves the entire list of places
    public ArrayList<mPlace> getAllPlaces() throws IOException, SAXException, ParserConfigurationException {
        return localPlacesStorage.getPlaces();
    }

    // Adds a place to the list
    public void addPlace(String name, String address, String lat, String lon) throws IOException, TransformerException, ParserConfigurationException, SAXException {
        places = localPlacesStorage.getPlaces();
        places.add(new mPlace(name, address,  lat, lon));
        localPlacesStorage.refreshInTheDatabase(places);
    }

}
