
/*
 * Author:
 *
 * Dennis Goyal     | ID:776980
 *
 */

package com.helpyou.itproject;

import android.util.Log;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class LocalPlacesStorage implements LocalStorageService<mPlace>{
    private File placesDataFile;
    private final String TAG = "LocalPlacesStorage";

    public LocalPlacesStorage(String localPlacesLocation) throws TransformerException, ParserConfigurationException, IOException {
        Log.d(TAG, "location is this: " + localPlacesLocation);
        String fileLocation = localPlacesLocation + "/places.xml";

        placesDataFile = new File(fileLocation);
        ArrayList<mPlace> places = new ArrayList<>();

    }

    void deleteEverything() {
        placesDataFile.delete();
    }

    public ArrayList<mPlace> getPlaces() throws ParserConfigurationException, IOException, SAXException {
        ArrayList<mPlace> places = new ArrayList<>();
        if(!placesDataFile.exists()){
            //File doesn't exists yet
            return places;
        }
        //So file is accessible don't know if there is any data create a ArrayList<Contact> and send back
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(placesDataFile);
        doc.getDocumentElement().normalize();
        if(!doc.getDocumentElement().getNodeName().equals("favourite_places")){
            return null;
        }
        NodeList nList = doc.getElementsByTagName("place");

        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            mPlace place = null;
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String name = eElement.getAttribute("name");
                //Grab all attributes
                String lat = eElement
                        .getElementsByTagName("lat")
                        .item(0)
                        .getTextContent();
                String address = eElement
                        .getElementsByTagName("address")
                        .item(0)
                        .getTextContent();
                String lon = eElement
                        .getElementsByTagName("lon")
                        .item(0)
                        .getTextContent();
                place = new mPlace(name, address, lat, lon);
            }
            places.add(place);
            Log.d(TAG, "Found a place in the database: " + place.getName());
        }
        Log.d(TAG, "at least tried: " + places.size());
        return places;
    }

    public void refreshInTheDatabase(ArrayList<mPlace> places) throws ParserConfigurationException, TransformerException, IOException {

        Log.d(TAG, places.size() + "");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();

        Element placesRootElement = doc.createElement("favourite_places");
        doc.appendChild(placesRootElement);

        for (mPlace place : places){
            Element placeString = doc.createElement("place");
            placesRootElement.appendChild(placeString);
            Attr attr = doc.createAttribute("name");
            attr.setValue(place.getName().toString());
            placeString.setAttributeNode(attr);
            Element address = doc.createElement("address");
            Element lat = doc.createElement("lat");
            Element lon = doc.createElement("lon");
            placeString.appendChild(address);
            placeString.appendChild(lat);
            placeString.appendChild(lon);
            address.setTextContent(place.getAddress());
            lat.setTextContent(place.getLat());
            lon.setTextContent(place.getLon());
        }

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        Log.d(TAG, "Here is the new file");
        StreamResult result = new StreamResult(placesDataFile);

        transformer.transform(source, result);
        // Output to console for testing
        StreamResult consoleResult = new StreamResult(System.out);
        transformer.transform(source, consoleResult);

    }

    public void createFile(ArrayList<mPlace> places) throws TransformerException, ParserConfigurationException, IOException {
        refreshInTheDatabase(places);
    }

    @Override
    public ArrayList<mPlace> getAll() throws IOException, SAXException, ParserConfigurationException {
        return getPlaces();
    }

    @Override
    public boolean add(mPlace element) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        ArrayList<mPlace> places = getPlaces();
        places.add(element);
        refreshInTheDatabase(places);
        return true;
    }


    @Override
    public boolean deleteAll() {
        deleteEverything();
        return true;
    }

    @Override
    public boolean replaceAll(ArrayList<mPlace> elements) throws IOException, TransformerException, ParserConfigurationException {
        refreshInTheDatabase(elements);
        return true;
    }
}
