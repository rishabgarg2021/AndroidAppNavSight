package com.helpyou.itproject;


import java.util.ArrayList;
import java.util.MissingFormatArgumentException;
//user class used to create an object
public class User {

    private String username;
    private String phoneNumber;
    private String emergencyPhone;
    private String imageUrl;
    private String dateOfBirth;
    private String volunteering="false";


    private String name;
    private String latitude;
    private String longitude;
    private ArrayList<String> medicalCondition = new ArrayList<>() ;


    public User(String  mPhoneNumber, String mUsername, String mEmergencyPhone, String mImageUrl, String mDateOfBirth, ArrayList<String> mMedicalCondition, String volunteering){
        setPhoneNumber(mPhoneNumber);
        setUsername(mUsername);
        setEmergencyPhone(mEmergencyPhone);
        setImageUrl(mImageUrl);
        setDateOfBirth(mDateOfBirth);
        setMedicalCondition(mMedicalCondition);
        setVolunteering(volunteering);
    }

    public User(){
        //empty constructor needed for FireBase data object constructor
    }


    public String getLatitude() {

        return latitude;
    }

    public void setVolunteering(String volunteering) {
       this.volunteering = volunteering;
    }

    public String getVolunteering() {

        return volunteering;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getDateOfBirth() {
        return this.dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public ArrayList<String> getMedicalCondition() {
        return medicalCondition;
    }

    public void setMedicalCondition(ArrayList<String> medicalCondition) {
        this.medicalCondition.addAll(medicalCondition);
    }


    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public void setEmergencyPhone(String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


}