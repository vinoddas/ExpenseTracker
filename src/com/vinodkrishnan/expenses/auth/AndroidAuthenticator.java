package com.vinodkrishnan.expenses.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Vinod Krishnan
 */
public class AndroidAuthenticator implements Authenticator {

    private final String TAG = "AndroidAuthenticator";
    Activity mActivity;
    AccountManager mManager;
    private String mService = null;
    private String mAuthToken = "";

    private Set<String> mAccountNames = new HashSet<String> (Arrays.asList(new String[] {
            "namitamangalath@gmail.com",
            "vinod.krishnan@gmail.com"
    }));

    public AndroidAuthenticator(Activity activity) {
        this.mActivity = activity;
        mManager = AccountManager.get(activity.getApplicationContext());
    }

    @Override
    public String getAuthToken(String service, boolean invalidate) {
        if (service == null) {
            throw new IllegalAccessError("No Service name defined, Can't create Auth Token...");
        }

        if (invalidate || (mService != null && !mService.equals(service))) {
            // Reset previous Token
            mManager.invalidateAuthToken("com.google", mAuthToken);
        }

        Account[] acs = mManager.getAccountsByType("com.google");
        Log.i(TAG, "Num of Matching account: " + acs.length);

        if (acs == null || acs.length == 0) {
            Toast.makeText(this.mActivity.getApplicationContext(), "No Google Account Added...", Toast.LENGTH_LONG).show();
            return "";
        }

        for (int i = 0; i < acs.length; i++) {
            if (acs[i].type.equals("com.google") && mAccountNames.contains(acs[i].name)) {
                // The first Account in the list above will be chosen.
                Log.i(TAG, "Selected Google Account " + acs[i].name);
                AccountManagerFuture result = (AccountManagerFuture) (
                        mManager.getAuthToken(acs[i], service, null, mActivity, null, null));

                try {
                    Bundle b = (Bundle) result.getResult();
                    mAuthToken = b.getString(AccountManager.KEY_AUTHTOKEN);
                    Log.i(TAG, "Auth_Token: " + mAuthToken);
                    return mAuthToken;
                } catch (Exception ex) {
                    Log.i(TAG, "Error: " + ex.toString());
                }
            }
        }
        Log.i(TAG, "Problem in getting Auth Token...");
        return "";
    }
}