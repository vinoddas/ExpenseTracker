package com.vinodkrishnan.expenses.view.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.vinodkrishnan.expenses.R;

/**
 *
 */
public class SettingsFragment extends PreferenceFragment {
    public static final String SPREADSHEET_NAME_KEY = "pref_spreadsheet_name";
    public static final String CATEGORIES_SHEET_NAME_KEY = "pref_categories_name";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
