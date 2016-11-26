package com.sadna.app.checkme.entities;


public class UserId {

    private String username;
    private String id;


    public UserId(String username , String id ) {
       this.setUsername(username);
        this.setId(id);

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String Username) {
        this.username = Username;
    }

    @Override
    public String toString() {
        return getUsername();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
