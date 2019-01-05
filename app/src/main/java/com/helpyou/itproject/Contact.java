package com.helpyou.itproject;

import java.util.ArrayList;

//contacts class used to create contacts objects
public class Contact{
    public String contactName;
    public ArrayList<String> contactNumber;
    public String userId;
    public Contact(String name){
        userId = null;
        this.contactName = name;
        contactNumber = new ArrayList<>();
    }
    public void setUserId(String userId){
        this.userId = userId;
    }
}