package com.lawfirm.model;

public class Client {
    private int clientId;
    private String fullName;
    private String email;

    Client(int clientId, String fullName, String email)
    {
        this.clientId = clientId;
        this.fullName = fullName;
        this.email = email;
    }

    public int getClient()
    {
        return clientId;
    }
    public String getFullName()
    {
        return fullName;
    }
    public String getEmail()
    {
        return email;
    }
}
