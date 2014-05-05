package com.vinodkrishnan.expenses.view.fragment;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.auth.AndroidAuthenticator;
import com.vinodkrishnan.expenses.sp.SpreadSheet;
import com.vinodkrishnan.expenses.sp.SpreadSheetFactory;
import com.vinodkrishnan.expenses.sp.WorkSheet;
import com.vinodkrishnan.expenses.tasks.GetRowsListener;
import com.vinodkrishnan.expenses.tasks.GetRowsTask;
import com.vinodkrishnan.expenses.util.CommonUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 *
 */
public class RecentFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "RecentFragment";

    private final Object mNumRowsLock = new Object();
    private Integer mNumRows;
    private TextView mNumRowsTextView;
    private TextView mLastRefreshedTextView;
    private Spinner mCategorySpinner;

    private List<String> mCategories;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recent_transactions, container, false);
        rootView.findViewById(R.id.choose_recent_button).setOnClickListener(this);
        mNumRowsTextView = (TextView)rootView.findViewById(R.id.number_of_days);
        mLastRefreshedTextView = (TextView)rootView.findViewById(R.id.last_refreshed_dialog);
        mCategorySpinner = (Spinner) rootView.findViewById(R.id.recent_category);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (CommonUtil.isNetworkConnected(getActivity())) {
            new GetRowsTask(getActivity(), new GetCategoriesListener()).execute(
                    CommonUtil.getCategoriesSheetName(getActivity()));
            refreshHistoryAsync();
        }
        resetFields();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.choose_recent_button:
                refreshHistoryAsync();
                break;
        }
    }

    private void resetFields() {
        mNumRowsTextView.setText("10");
    }

    private void setCategoriesSpinner() {
        if (mCategories != null) {
            ArrayAdapter adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_dropdown_item, mCategories);
            mCategorySpinner.setAdapter(adapter);
        }
    }

    private void refreshHistoryAsync() {
        final Calendar cal = Calendar.getInstance();
        String year = Integer.toString(cal.get(Calendar.YEAR));
        synchronized (mNumRowsLock) {
            mNumRows = 0;
            CharSequence numRows = mNumRowsTextView.getText();
            if (!TextUtils.isEmpty(numRows) && TextUtils.isDigitsOnly(numRows)) {
                mNumRows = Integer.parseInt(numRows.toString());
            }
        }
        mLastRefreshedTextView.setText("Loading...");
        new GetRowsTask(getActivity(), new GetHistoryListener(
                mCategorySpinner.getSelectedItem().toString())).execute(year);
    }

    private class GetHistoryListener implements GetRowsListener {
        private String mCategory;
        public GetHistoryListener(String category) {
            mCategory = category;
        }

        @Override
        public boolean onGetRowsCompleted(List<Map<String, String>> history) {
            if (history == null) {
                return false;
            }
            synchronized (mNumRowsLock) {
                if (mNumRows >= 0) {
                    int startIndex = history.size() > mNumRows ? history.size() - mNumRows : 0;
                    history = history.subList(startIndex, history.size());
                }
            }

            // Remove all the rows that are not the chosen category.
            if (!TextUtils.isEmpty(mCategory)) {
                for (Map<String, String> row : history) {
                    if (row.containsKey("category") && !mCategory.equals(row.get("category"))) {
                        history.remove(row);
                    }
                }
            }

            Calendar c = Calendar.getInstance();

            TableLayout table = (TableLayout)getActivity().findViewById(R.id.summary_table);
            table.removeAllViews();
            for (Map<String, String> historyRow : history) {
                // Inflate your row "template" and fill out the fields.
                TableRow row = (TableRow)LayoutInflater.from(getActivity()).inflate(
                        R.layout.summary_row, null);
                ((TextView)row.findViewById(R.id.summary_row_date)).setText(historyRow.get("date"));
                ((TextView)row.findViewById(R.id.summary_row_amount)).setText(historyRow.get("amount"));
                ((TextView)row.findViewById(R.id.summary_row_category)).setText(historyRow.get("category"));
                ((TextView)row.findViewById(R.id.summary_row_by)).setText(historyRow.get("by"));
                if (getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_LANDSCAPE) {
                    ((TextView) row.findViewById(R.id.summary_row_description)).setText(
                            historyRow.get("description"));
                }
                table.addView(row);
            }
            table.requestLayout();

            mLastRefreshedTextView.setText(String.format("Refreshed: %2d/%2d/%4d %2d:%2d:%2d",
                    c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                    c.get(Calendar.YEAR), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                    + c.get(Calendar.SECOND)));
            return true;
        }
    }

    private class GetCategoriesListener implements GetRowsListener {
        @Override
        public boolean onGetRowsCompleted(List<Map<String, String>> rows) {
            if (rows == null) {
                return false;
            }

            List<String> categories = new ArrayList<String>();
            for (Map<String, String> row : rows) {
                if (row.containsKey("categories")) {
                    categories.add(row.get("categories"));
                }
            }
            Log.d(TAG, "Found " + (categories == null ? 0 : categories.size()) + " categories.");
            categories.add("");
            mCategories = categories;
            setCategoriesSpinner();

            return true;
        }
    }
}
