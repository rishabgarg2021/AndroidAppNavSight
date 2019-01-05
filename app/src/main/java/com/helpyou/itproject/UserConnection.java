package com.helpyou.itproject;

/*
 * Author: Uvin Abeysinghe
 * Student Id : 789931
 * University of Melbourne
 *
 * Rishab
 */


public class UserConnection {
    private String isConnected,lastActivity,userType,connectWith,requestId;

    public UserConnection(){ }
    public String getIsConnected() {
        return isConnected;
    }

    public void setIsConnected(String isConnected) {
        this.isConnected = isConnected;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(String lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getConnectWith() {
        return connectWith;
    }

    public void setConnectWith(String connectWith) {
        this.connectWith = connectWith;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public UserConnection(String isConnected, String lastActivity, String userType, String connectWith, String requestId){
        setIsConnected(isConnected);
        setLastActivity(lastActivity);
        setUserType(userType);
        setConnectWith(connectWith);
        setRequestId(requestId);
    }
}

