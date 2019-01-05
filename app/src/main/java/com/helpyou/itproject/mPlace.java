
/*
 * Author:
 *
 * Dennis Goyal     | ID: 776980
 *
 */

package com.helpyou.itproject;

// This class defines the blueprint for a Place object which is stored in local storage
public class mPlace {

    String name;
    String address;
    String lat;
    String lon;

    public mPlace(String name, String address, String lat, String lon) {
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lon = lon;
    }

    public String getName() {
        return name;
    }
    public String getAddress() {
        return address;
    }
    public String getLat() {
        return lat;
    }
    public String getLon() {
        return lon;
    }
}
