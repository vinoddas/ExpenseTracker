package com.vinodkrishnan.expenses.view.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.view.fragment.EnterExpenseFragment;
import com.vinodkrishnan.expenses.view.fragment.RecentFragment;
import com.vinodkrishnan.expenses.view.fragment.ModifyCategoryFragment;

/**
 * Adapter for the main view.
 */
public class TabAdapter extends FragmentPagerAdapter {
    private final Context mContext;

    public TabAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0:
                return new EnterExpenseFragment();
            case 1:
                return new ModifyCategoryFragment();
            case 2:
                return new RecentFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return mContext.getResources().getStringArray(R.array.tab_titles).length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getStringArray(R.array.tab_titles)[position];
    }
}
