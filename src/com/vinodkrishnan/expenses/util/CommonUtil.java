package com.vinodkrishnan.expenses.util;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.widget.TextView;
import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.view.fragment.SettingsFragment;

/**
 *
 */
public class CommonUtil {
    public static boolean isNetworkConnected(Activity activity) {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null;
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
