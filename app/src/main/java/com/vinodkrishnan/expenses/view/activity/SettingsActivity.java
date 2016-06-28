package com.vinodkrishnan.expenses.view.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.vinodkrishnan.expenses.view.fragment.SettingsFragment;

/**
 *
 */
public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
