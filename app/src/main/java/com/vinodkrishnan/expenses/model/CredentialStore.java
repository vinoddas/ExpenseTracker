package com.vinodkrishnan.expenses.model;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.vinodkrishnan.expenses.util.CommonUtil;
import com.vinodkrishnan.expenses.view.activity.MainActivity;

import java.util.Arrays;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Takes care of all the credentials needed.
 */
public class CredentialStore {
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };

    private Activity mActivity;
    private SharedPreferences mPrefs;
    private GoogleAccountCredential mCredential;

    private static CredentialStore mInstance;

    private CredentialStore(Activity activity) {
        mActivity = activity;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                activity.getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }

    public static synchronized CredentialStore getInstance(Activity activity) {
        if (mInstance == null) {
            mInstance = new CredentialStore(activity);
        }
        return mInstance;
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    public void loadCredentials() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        }
    }

    public void setAccountName(String accountName) {
        mPrefs.edit().putString(CommonUtil.ACCOUNT_NAME_PREF_KEY, accountName).apply();
        mCredential.setSelectedAccountName(accountName);
    }

    public GoogleAccountCredential getCredential() {
        if (mCredential.getSelectedAccount() != null) {
            return mCredential;
        }
        return null;
    }

    public boolean isReady() {
        return mCredential.getSelectedAccount() != null;
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(MainActivity.REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(mActivity, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = mPrefs.getString(CommonUtil.ACCOUNT_NAME_PREF_KEY, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
            } else {
                // Start a dialog from which the user can choose an account
                mActivity.startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        MainActivity.REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(mActivity,
                    "This app needs to access your Google account (via Contacts).",
                    MainActivity.REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(mActivity);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(mActivity);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            CommonUtil.showGooglePlayServicesAvailabilityErrorDialog(
                    mActivity, connectionStatusCode);
        }
    }
}

