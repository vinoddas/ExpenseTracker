package com.vinodkrishnan.expenses.util;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;
import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.view.fragment.SettingsFragment;

public class CommonUtil {
    public static final String CATEGORIES_PREF_KEY = "com.vinodkrishnan.expenses.pref_categories";
    public static final String EXPENSES_PREF_KEY = "com.vinodkrishnan.expenses.pref_expenses";

    public static boolean isNetworkConnected(Activity activity) {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null){
            return false;
        }
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            return false;
        }
        return true;
    }

    public static void showDialog(TextView dialogView, String message, int color) {
        dialogView.setText(message);
        dialogView.setTextColor(color);
    }

    public static String getSpreadsheetName(Activity activity) {
        return  PreferenceManager.getDefaultSharedPreferences(activity).getString(SettingsFragment.SPREADSHEET_NAME_KEY,
                activity.getResources().getString(R.string.pref_spreadsheet_name_default));
    }

    public static String getCategoriesSheetName(Activity activity) {
        return  PreferenceManager.getDefaultSharedPreferences(activity).getString(
                SettingsFragment.CATEGORIES_SHEET_NAME_KEY,
                activity.getResources().getString(R.string.pref_categories_name_default));
    }
}
