package com.sadna.app.checkme.entities;


public class UserPhone {

    private String phone;
    private String username;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String Phone) {
        this.phone = Phone;
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

}
