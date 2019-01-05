package com.helpyou.itproject;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.sql.SQLOutput;
import java.util.ArrayList;

public class LocalContactsStorage {

    private File contactsDataFile;

    public LocalContactsStorage(String fileLocation) throws java.io.IOException, SAXException, ParserConfigurationException {
        String finalFileLocation = fileLocation + "/contacts.xml";
        contactsDataFile = new File(finalFileLocation);
    }

    public ArrayList<Contact> getLocalContacts() throws ParserConfigurationException, IOException, SAXException {
        if(!contactsDataFile.exists()){
            //File doesn't exists yet
            return null;
        }
        ArrayList<Contact> contacts = new ArrayList<>();
        //So file is accessible don't know if there is any data create a ArrayList<Contact> and send back
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(contactsDataFile);
        doc.getDocumentElement().normalize();
        if(!doc.getDocumentElement().getNodeName().equals("contacts")){
            return null;
        }
        NodeList nList = doc.getElementsByTagName("contact");

        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            Contact cont = null;
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String nm = eElement.getAttribute("name");
                String userId = eElement.getAttribute("user_id");

                //Grab all numbers
                int i = 0;
                cont = new Contact(nm);
                cont.setUserId(userId);
                while (true) {
                    if(eElement.getElementsByTagName("number" + i).getLength() == 0){
                        break;
                    }
                    String phoneNo = eElement
                            .getElementsByTagName("number" + i)
                            .item(0)
                            .getTextContent();
                    cont.contactNumber.add(phoneNo);
                    i++;
                }
            }
            contacts.add(cont);
        }
        return contacts;
    }

    public boolean updateLocalContacts(ArrayList<Contact> contacts) throws ParserConfigurationException {

        if(!contactsDataFile.exists()){
            return createNewLocalContactsData(contactsDataFile, contacts);
        }
        contactsDataFile.delete();
        return createNewLocalContactsData(contactsDataFile, contacts);
    }

    private boolean createNewLocalContactsData(File contactsDataFile, ArrayList<Contact> contacts) {
        //testFileSaving();

        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element contactsRootElement = doc.createElement("contacts");
            doc.appendChild(contactsRootElement);

            for(Contact cont : contacts){
                Element contactString = doc.createElement("contact");
                contactsRootElement.appendChild(contactString);
                // setting attribute to element
                Attr attr = doc.createAttribute("name");
                attr.setValue(cont.contactName);
                Attr attr2 = doc.createAttribute("user_id");
                attr2.setValue(cont.userId);
                System.out.println("name of the contact being saved" + cont.contactName);
                contactString.setAttributeNode(attr);
                contactString.setAttributeNode(attr2);

                int counter = 0;
                for(String number : cont.contactNumber){
                    Element num = doc.createElement("number" + counter);
                    contactString.appendChild(num);
                    num.setTextContent(number);
                    counter++;
                }
            }
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(contactsDataFile);
            transformer.transform(source, result);
            // Output to console for testing
            System.out.println("here is the contacts file");
            StreamResult consoleResult = new StreamResult(System.out);
            transformer.transform(source, consoleResult);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void testFileSaving(String fileLocation) {
        File fp = new File(fileLocation);
        try {
            fp.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}