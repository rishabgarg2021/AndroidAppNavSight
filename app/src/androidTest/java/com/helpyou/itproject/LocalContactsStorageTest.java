package com.helpyou.itproject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import static org.junit.Assert.*;

public class LocalContactsStorageTest {
    LocalContactsStorage localContactsStorage = null;
    ArrayList<Contact> testContacts = new ArrayList<>();
    @Before
    public void setUp() throws Exception {
        localContactsStorage = new LocalContactsStorage("/data/user/0/com.helpyou.itproject/files");
        testContacts.add(new Contact("Dennis"));
    }

    @Test
    public void completeTest() throws ParserConfigurationException, IOException, SAXException, InterruptedException {
        localContactsStorage.updateLocalContacts(testContacts);
        ArrayList<String> numbers = new ArrayList<>();
        numbers.add("5495654");
        Contact testContact = new Contact("Studio");
        testContact.contactNumber = numbers;
        testContact.userId = "asdf";
        testContacts.add(new Contact("Studio"));
        localContactsStorage.updateLocalContacts(testContacts);
    }

    @After
    public void tearDown() throws Exception {
        localContactsStorage.updateLocalContacts(new ArrayList<Contact>());
    }
}