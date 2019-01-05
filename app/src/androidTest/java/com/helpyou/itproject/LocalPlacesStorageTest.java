package com.helpyou.itproject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class LocalPlacesStorageTest {
    private final String TAG = "LocalPlacesStorageTest";
    LocalPlacesStorage localPlacesStorage = null;
    ArrayList<mPlace> testPlaces = new ArrayList<>();
    @Before
    public void setUp() throws IOException, TransformerException, ParserConfigurationException {
        localPlacesStorage = new LocalPlacesStorage("/data/user/0/com.helpyou.itproject/files");
        testPlaces.add(new mPlace("Boogie", "Woogie, Boogie", "78", "85"));
    }

    @Test
    public void test1() throws IOException, SAXException, ParserConfigurationException, TransformerException {
        localPlacesStorage.deleteEverything();
        assertEquals(localPlacesStorage.getAll().size(), 0);
        localPlacesStorage.add(new mPlace("Java", "java.util.android", "0", "0"));
        mPlace place = new mPlace("Android", "good device", "24", "0");
        localPlacesStorage.add(place);
        //lon can't be -24 but it hasn't been implemented yet
        localPlacesStorage.add(new mPlace("Google", "Silicon Valley", "0", "-24"));
        assertEquals(localPlacesStorage.getAll().size(), 3);
        assertEquals(localPlacesStorage.getAll().get(1).name, place.name);
    }

    @Test
    public void test2() throws IOException, SAXException, ParserConfigurationException, TransformerException {
        testPlaces.remove(testPlaces.get(0));
        localPlacesStorage.replaceAll(testPlaces);
        assertEquals(localPlacesStorage.getAll().size(), 0);
    }

    @After
    public void finish(){
        localPlacesStorage.deleteEverything();
    }
}

