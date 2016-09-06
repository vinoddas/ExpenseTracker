package com.vinodkrishnan.expenses.view.activity;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.model.CredentialStore;
import com.vinodkrishnan.expenses.util.CommonUtil;
import com.vinodkrishnan.expenses.view.adapter.TabAdapter;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * MainActivity
 */
public class MainActivity extends FragmentActivity implements EasyPermissions.PermissionCallbacks {
    public static final int REQUEST_ACCOUNT_PICKER = 0;
    public static final int REQUEST_AUTHORIZATION = 1;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 2;
    public static final int REQUEST_PERMISSION_GET_ACCOUNTS = 3;
    public static final int RESULT_SETTINGS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Initialization
        ((ViewPager) findViewById(R.id.pager))
                .setAdapter(new TabAdapter(this, getSupportFragmentManager(),
                        getIntent().getExtras()));
        CredentialStore.getInstance(this).loadCredentials();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    CommonUtil.showErrorDialog(this, R.string.error_required_google_play_services);
                } else {
                    CredentialStore.getInstance(this).loadCredentials();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        CredentialStore.getInstance(this).setAccountName(accountName);
                        CredentialStore.getInstance(this).loadCredentials();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    CredentialStore.getInstance(this).loadCredentials();
                }
                break;
            case RESULT_SETTINGS:
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        // Nothing to do
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        // Nothing to do
    }
}
