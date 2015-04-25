package com.vinodkrishnan.expenses.auth;

public interface Authenticator {

    /**
     * Do the authentication and read Auth_Token from Server. The implementor class
     * will provide Account (Gmail) and Service details
     *
     * @return
     */
    public String getAuthToken();
}
