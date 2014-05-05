package com.vinodkrishnan.expenses.auth;

public interface Authenticator {

    /**
     * Do the authentication and read Auth_Token from Server. The implementor class
     * will provide Account (Gmail) and Service details
     *
     * @param service Name of the Service for which Authentication Token is required e.g. "wise" for SpreadSheet
     * @param invalidate Should we invalidate or not.
     * @return
     */
    public String getAuthToken(String service, boolean invalidate);
}
