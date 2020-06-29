package com.vinodkrishnan.expenses.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.view.activity.MainActivity;
import com.vinodkrishnan.expenses.view.fragment.SettingsFragment;

public class CommonUtil {
    private static final String TAG = "CommonUtil";

    public static final String CATEGORIES_PREF_KEY = "com.vinodkrishnan.expenses.pref_categories";
    public static final String EXPENSES_PREF_KEY = "com.vinodkrishnan.expenses.pref_expenses";
    public static final String ACCOUNT_NAME_PREF_KEY = "com.vinodkrishnan.expenses.account_name";

    public static final String ROW_NUM_KEY = "RowNum";  // Not actually set as a column.
    public static final String DATE_COLUMN = "Date";
    public static final String AMOUNT_COLUMN = "Amount";
    public static final String CATEGORY_COLUMN = "Category";
    public static final String BY_COLUMN = "By";
    public static final String DESCRIPTION_COLUMN = "Description";

    public static boolean isNetworkConnected(Activity activity) {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (cm == null){
            return false;
        }
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            return false;
        }
        return true;
    }

    public static void showErrorDialog(Context context, int messageResId) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_error)
                .setMessage(context.getString(messageResId))
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }

    public static void showToast(Context context, int messageResId) {
        Toast.makeText(context, context.getString(messageResId), Toast.LENGTH_LONG).show();
    }

    public static String getSpreadsheetId(Activity activity) {
        return  PreferenceManager.getDefaultSharedPreferences(activity).getString(
                SettingsFragment.SPREADSHEET_ID_KEY,
                activity.getString(R.string.pref_spreadsheet_id_default));
    }

    public static String getCategoriesSheetName(Activity activity) {
        return  PreferenceManager.getDefaultSharedPreferences(activity).getString(
                SettingsFragment.CATEGORIES_SHEET_NAME_KEY,
                activity.getString(R.string.pref_categories_name_default));
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    public static void showGooglePlayServicesAvailabilityErrorDialog(final Activity activity,
            final int connectionStatusCode) {
        GoogleApiAvailability.getInstance().getErrorDialog(activity, connectionStatusCode,
                MainActivity.REQUEST_GOOGLE_PLAY_SERVICES).show();
    }

    public static int getPositionFromSpinner(Spinner spinner, String value) {
        if (spinner == null || TextUtils.isEmpty(value) || spinner.getAdapter() == null) {
            Log.e(TAG, "Either spinner is null or value is empty");
            return -1;
        }

        SpinnerAdapter adapter = spinner.getAdapter();

        for (int i = 0, size = adapter.getCount(); i < size; i++) {
            if (TextUtils.equals(value, (String)adapter.getItem(i))) {
                return i;
            }
        }
        return -1;
    }
}
