package com.helpyou.itproject;

// message class

public class Messages {



    private String message,user,from,time,type,notify;


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNotify() {
        return notify;
    }

    public void setNotify(String notify) {
        this.notify = notify;
    }
    public Messages(){

    }
    public Messages(String message, String user, String from, String time, String type, String notify) {
        this.message = message;
        this.user = user;
        this.from=from;
        this.time=time;
        this.type=type;
        this.notify=notify;
    }


}