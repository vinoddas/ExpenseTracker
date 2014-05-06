package com.vinodkrishnan.expenses.view.fragment;

import static com.vinodkrishnan.expenses.util.CommonUtil.EXPENSES_PREF_KEY;
import static com.vinodkrishnan.expenses.util.CommonUtil.CATEGORIES_PREF_KEY;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import com.google.gson.Gson;
import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.tasks.GetRowsListener;
import com.vinodkrishnan.expenses.tasks.GetRowsTask;
import com.vinodkrishnan.expenses.util.CommonUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RecentFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "RecentFragment";

    private static final String ALL_CATEGORIES = "ALL";

    private TextView mNumRowsTextView;
    private TextView mLastRefreshedTextView;
    private Spinner mCategorySpinner;

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
        resetFields();
        if (CommonUtil.isNetworkConnected(getActivity())) {
            refreshHistoryAsync(ALL_CATEGORIES);
        } else {
            refreshHistoryWidget(null, null, 0);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        List<String> categories = new ArrayList<String>();
        categories.add(ALL_CATEGORIES);
        if (prefs.contains(CATEGORIES_PREF_KEY)) {
            categories.addAll(prefs.getStringSet(CATEGORIES_PREF_KEY, null));
            setCategoriesSpinner(categories);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.choose_recent_button:
                refreshHistoryAsync(mCategorySpinner.getSelectedItem().toString());
                break;
        }
    }

    private void resetFields() {
        mNumRowsTextView.setText("10");
    }

    private void setCategoriesSpinner(List<String> categories) {
        if (categories != null) {
            ArrayAdapter adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_dropdown_item, categories);
            mCategorySpinner.setAdapter(adapter);
            // Set the default to "ALL"
            mCategorySpinner.setSelection(0);
        }
    }

    private void refreshHistoryAsync(String category) {
        final Calendar cal = Calendar.getInstance();
        String year = Integer.toString(cal.get(Calendar.YEAR));
        int numRows = 0;
        CharSequence numRowsCharSeq = mNumRowsTextView.getText();
        if (!TextUtils.isEmpty(numRowsCharSeq) && TextUtils.isDigitsOnly(numRowsCharSeq)) {
            numRows = Integer.parseInt(numRowsCharSeq.toString());
        }
        mLastRefreshedTextView.setText("Loading...");
        new GetRowsTask(getActivity(), new GetHistoryListener(category, numRows)).execute(year);
    }

    private void refreshHistoryWidget(List<Map<String, String>> history, String category, int numRows) {
        TableLayout table = (TableLayout)getActivity().findViewById(R.id.summary_table);
        table.removeAllViews();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (prefs.contains(EXPENSES_PREF_KEY)) {
            for (String expense : prefs.getStringSet(EXPENSES_PREF_KEY, null)) {
                table.addView(inflateTableRow(new Gson().fromJson(expense, Map.class), true));
            }
        }

        if (history != null) {
            if (numRows >= 0) {
                int startIndex = history.size() > numRows ? history.size() - numRows : 0;
                history = history.subList(startIndex, history.size());
            }

            // Remove all the rows that are not the chosen category.
            if (!ALL_CATEGORIES.equals(category)) {
                Iterator<Map<String, String>> iterator = history.iterator();
                while (iterator.hasNext()) {
                    Map<String, String> row = iterator.next();
                    if (row.containsKey("category") && !category.equals(row.get("category"))) {
                        iterator.remove();
                    }
                }
            }
            for (Map<String, String> historyRow : history) {
                // Inflate your row "template" and fill out the fields.
                table.addView(inflateTableRow(historyRow, false));
            }
        }
        table.requestLayout();

        Calendar c = Calendar.getInstance();
        mLastRefreshedTextView.setText(String.format("Refreshed: %2d/%2d/%4d %2d:%2d:%2d",
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.YEAR), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                + c.get(Calendar.SECOND)));
    }

    private TableRow inflateTableRow(Map<String, String> rowValues, boolean isOffline) {
        TableRow row = (TableRow)LayoutInflater.from(getActivity()).inflate(
                R.layout.summary_row, null);
        TextView rowDate = (TextView)row.findViewById(R.id.summary_row_date);
        rowDate.setText(rowValues.get("date"));
        if (isOffline) {
            rowDate.setTextColor(Color.RED);
        }
        TextView rowAmount = (TextView)row.findViewById(R.id.summary_row_amount);
        rowAmount.setText(rowValues.get("amount"));
        if (isOffline) {
            rowAmount.setTextColor(Color.RED);
        }
        TextView rowCategory = (TextView)row.findViewById(R.id.summary_row_category);
        rowCategory.setText(rowValues.get("category"));
        if (isOffline) {
            rowCategory.setTextColor(Color.RED);
        }
        TextView rowBy = (TextView)row.findViewById(R.id.summary_row_by);
        rowBy.setText(rowValues.get("by"));
        if (isOffline) {
            rowBy.setTextColor(Color.RED);
        }
        if (getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE) {
            TextView rowDescription = (TextView)row.findViewById(R.id.summary_row_description);
            rowDescription.setText(rowValues.get("description"));
            if (isOffline) {
                rowDescription.setTextColor(Color.RED);
            }
        }
        return row;
    }

    private class GetHistoryListener implements GetRowsListener {
        private String mCategory;
        private int mNumRows;
        public GetHistoryListener(String category, int numRows) {
            mCategory = category;
            mNumRows = numRows;
        }

        @Override
        public boolean onGetRowsCompleted(List<Map<String, String>> history) {
            if (history == null) {
                return false;
            }
            refreshHistoryWidget(history, mCategory, mNumRows);
            return true;
        }
    }
}
