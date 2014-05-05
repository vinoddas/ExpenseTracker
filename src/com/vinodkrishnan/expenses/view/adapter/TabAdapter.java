package com.vinodkrishnan.expenses.view.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.vinodkrishnan.expenses.view.fragment.EnterExpenseFragment;
import com.vinodkrishnan.expenses.view.fragment.RecentFragment;
import com.vinodkrishnan.expenses.view.fragment.ModifyCategoryFragment;

/**
 *
 */
public class TabAdapter extends FragmentPagerAdapter {
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
        return 3;
    }
}
