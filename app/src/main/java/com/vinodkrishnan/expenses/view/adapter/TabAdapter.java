package com.vinodkrishnan.expenses.view.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.vinodkrishnan.expenses.view.fragment.EnterExpenseFragment;
import com.vinodkrishnan.expenses.view.fragment.RecentFragment;
import com.vinodkrishnan.expenses.view.fragment.ModifyCategoryFragment;

/**
 * Adapter for the main view.
 */
public class TabAdapter extends FragmentPagerAdapter {
    // Tab Titles
    private static final String TAB_TITLES[] = new String[] { "Expense", "Category", "Recent" };

    public TabAdapter(FragmentManager fm) {
        super(fm);
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
        return TAB_TITLES.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return TAB_TITLES[position];
    }
}
