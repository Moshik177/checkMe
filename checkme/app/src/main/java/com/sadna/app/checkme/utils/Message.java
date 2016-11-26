package com.sadna.app.checkme.utils;

import com.google.gson.Gson;



public class Message {
    private String name;
    private String message;
    private boolean isSelf;
    private String tokens;

    public Message() {}

    public Message(String fromName, String message, boolean isSelf,String tokens) {
        this.name = fromName;
        this.message = message;
        this.isSelf = isSelf;
        this.tokens= tokens;
    }


    public String getFromName() {
        return name;
    }

    public void setFromName(String fromName) {
        this.name = fromName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSelf() {
        return isSelf;
    }

    public void setSelf(boolean isSelf) {
        this.isSelf = isSelf;
    }

    public String toJsonString()
    {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getTokens() {
        return tokens;
    }

    public void setTokens(String tokens) {
        this.tokens = tokens;
    }
}