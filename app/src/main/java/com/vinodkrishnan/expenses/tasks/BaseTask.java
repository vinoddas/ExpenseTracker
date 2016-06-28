package com.vinodkrishnan.expenses.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.vinodkrishnan.expenses.util.CommonUtil;
import com.vinodkrishnan.expenses.view.activity.MainActivity;

/**
 * Task that will take care of connection details.
 */
public abstract class BaseTask<Params, Progress, Result>
        extends AsyncTask<Params, Progress, Result> {
    private static final String TAG = "BaseTask";

    protected Exception mLastError = null;
    protected final Activity mActivity;

    protected BaseTask(Activity activity) {
        mActivity = activity;
    }

    @Override
    protected void onCancelled() {
        if (mLastError != null) {
            if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                CommonUtil.showGooglePlayServicesAvailabilityErrorDialog(mActivity,
                        ((GooglePlayServicesAvailabilityIOException) mLastError)
                                .getConnectionStatusCode());
            } else if (mLastError instanceof UserRecoverableAuthIOException) {
                mActivity.startActivityForResult(
                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                        MainActivity.REQUEST_AUTHORIZATION);
            } else {
                CommonUtil.showErrorDialog(mActivity,
                        "An error has occurred talking to the spreadsheet");
                Log.e(TAG, "The following error occurred:\n"
                        + mLastError.getMessage());
            }
        } else {
            Log.w(TAG, "Request was cancelled");
        }
    }
}
