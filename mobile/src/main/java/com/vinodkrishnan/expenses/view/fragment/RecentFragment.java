package com.vinodkrishnan.expenses.view.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.gson.Gson;
import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.tasks.DeleteRowTask;
import com.vinodkrishnan.expenses.tasks.GetRowsTask;
import com.vinodkrishnan.expenses.util.CommonUtil;
import com.vinodkrishnan.expenses.view.activity.MainActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.vinodkrishnan.expenses.util.CommonUtil.ROW_NUM_KEY;
import static com.vinodkrishnan.expenses.util.CommonUtil.AMOUNT_COLUMN;
import static com.vinodkrishnan.expenses.util.CommonUtil.BY_COLUMN;
import static com.vinodkrishnan.expenses.util.CommonUtil.CATEGORIES_PREF_KEY;
import static com.vinodkrishnan.expenses.util.CommonUtil.CATEGORY_COLUMN;
import static com.vinodkrishnan.expenses.util.CommonUtil.DATE_COLUMN;
import static com.vinodkrishnan.expenses.util.CommonUtil.DESCRIPTION_COLUMN;
import static com.vinodkrishnan.expenses.util.CommonUtil.EXPENSES_PREF_KEY;

public class RecentFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "RecentFragment";

    private static final String ALL_CATEGORIES = "ALL";
    private static final String DATE_FORMAT = "MM/dd/yyyy";

    private TextView mNumDaysTextView;
    private TextView mLastRefreshedTextView;
    private Spinner mCategorySpinner;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recent_transactions, container, false);
        rootView.findViewById(R.id.choose_recent_button).setOnClickListener(this);
        mNumDaysTextView = (TextView)rootView.findViewById(R.id.number_of_days);
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
                if (mCategorySpinner.getSelectedItem() != null) {
                    refreshHistoryAsync(mCategorySpinner.getSelectedItem().toString());
                }
                break;
        }
    }

    private void resetFields() {
        mNumDaysTextView.setText(getActivity().getString(R.string.default_recent_days));
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
        String year = getCurrentSheetName();
        int numDays = 0;
        CharSequence numDaysCharSeq = mNumDaysTextView.getText();
        if (!TextUtils.isEmpty(numDaysCharSeq) && TextUtils.isDigitsOnly(numDaysCharSeq)) {
            numDays = Integer.parseInt(numDaysCharSeq.toString());
        }
        mLastRefreshedTextView.setText(getActivity().getString(R.string.loading));
        new GetRowsTask(getActivity(), new GetHistoryListener(category, numDays)).execute(year);
    }

    private String getCurrentSheetName() {
        final Calendar cal = Calendar.getInstance();
        return Integer.toString(cal.get(Calendar.YEAR));
    }

    private void refreshHistoryWidget(List<Map<String, String>> history,
            String category, int numDays) {
        final Date now = new Date();
        if (getActivity() == null || history == null) {
            return;
        }
        TableLayout table = (TableLayout)getActivity().findViewById(R.id.summary_table);
        if (table == null) {
            return;
        }
        table.removeAllViews();
        // Show all the offline transactions first.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (prefs.contains(EXPENSES_PREF_KEY)) {
            for (String expense : prefs.getStringSet(EXPENSES_PREF_KEY, null)) {
                table.addView(inflateTableRow(new Gson().fromJson(expense, Map.class), true));
            }
        }

        int rowNum = 1;
        if (history != null) {
            Iterator<Map<String, String>> iterator = history.iterator();
            while (iterator.hasNext()) {
                Map<String, String> row = iterator.next();
                row.put(ROW_NUM_KEY, String.valueOf(rowNum++));
                if (!ALL_CATEGORIES.equals(category) &&
                        !category.equals(row.get(CATEGORY_COLUMN))) {
                    iterator.remove();
                }
                if (row.containsKey(DATE_COLUMN) && !TextUtils.isEmpty(row.get(DATE_COLUMN))) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
                        Date date = sdf.parse(row.get(DATE_COLUMN));
                        if (now.getTime() - date.getTime() > numDays * DateUtils.DAY_IN_MILLIS) {
                            iterator.remove();
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Could not parse date.", e);
                    }
                }
            }

            for (int i = history.size() - 1; i >=0 ; i--) {
                // Inflate your row "template" and fill out the fields.
                table.addView(inflateTableRow(history.get(i), false));
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
        rowDate.setText(rowValues.get(DATE_COLUMN));
        if (isOffline) {
            rowDate.setTextColor(Color.RED);
        }
        TextView rowAmount = (TextView)row.findViewById(R.id.summary_row_amount);
        rowAmount.setText(rowValues.get(AMOUNT_COLUMN));
        if (isOffline) {
            rowAmount.setTextColor(Color.RED);
        }
        TextView rowCategory = (TextView)row.findViewById(R.id.summary_row_category);
        rowCategory.setText(rowValues.get(CATEGORY_COLUMN));
        if (isOffline) {
            rowCategory.setTextColor(Color.RED);
        }
        TextView rowBy = (TextView)row.findViewById(R.id.summary_row_by);
        rowBy.setText(rowValues.get(BY_COLUMN));
        if (isOffline) {
            rowBy.setTextColor(Color.RED);
        }
        if (getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE) {
            TextView rowDescription = (TextView)row.findViewById(R.id.summary_row_description);
            rowDescription.setText(rowValues.get(DESCRIPTION_COLUMN));
            if (isOffline) {
                rowDescription.setTextColor(Color.RED);
            }
        }
        if (!isOffline) {
            // Only set this if the rowNum is specified, basically, not for offline transactions.
            setupEditSettings(row, rowValues);
        }
        return row;
    }

    private void setupEditSettings(TableRow r, final Map<String, String> rowValues) {
        r.findViewById(R.id.summary_row_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View editButton) {
                Intent intent = new Intent(getContext(), MainActivity.class);
                intent.putExtra(ROW_NUM_KEY, rowValues.get(ROW_NUM_KEY));
                intent.putExtra(DATE_COLUMN, rowValues.get(DATE_COLUMN));
                intent.putExtra(CATEGORY_COLUMN, rowValues.get(CATEGORY_COLUMN));
                intent.putExtra(BY_COLUMN, rowValues.get(BY_COLUMN));
                intent.putExtra(AMOUNT_COLUMN, rowValues.get(AMOUNT_COLUMN));
                intent.putExtra(DESCRIPTION_COLUMN, rowValues.get(DESCRIPTION_COLUMN));
                startActivity(intent);
            }
        });
        r.findViewById(R.id.summary_row_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext())
                        .setCancelable(false)
                        .setMessage(getString(R.string.confirm_delete_dialog_message,
                                rowValues.get(AMOUNT_COLUMN), rowValues.get(DATE_COLUMN),
                                rowValues.get(BY_COLUMN), rowValues.get(CATEGORY_COLUMN)))
                        .setPositiveButton(R.string.confirm_delete_dialog_yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String year = getCurrentSheetName();
                                        new DeleteRowTask(getActivity(), year,
                                                rowValues.get(ROW_NUM_KEY)).execute();
                                        refreshHistoryAsync(ALL_CATEGORIES);
                                    }
                        })
                        .setNegativeButton(R.string.confirm_delete_dialog_no,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        refreshHistoryAsync(ALL_CATEGORIES);
                                    }
                                })
                        .create().show();
            }
        });
        r.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View row) {
                TextView rowCategory = (TextView)row.findViewById(R.id.summary_row_category);
                TextView rowBy = (TextView)row.findViewById(R.id.summary_row_by);
                TextView rowDescription = (TextView)row.findViewById(R.id.summary_row_description);
                Button rowEdit = (Button)row.findViewById(R.id.summary_row_edit);
                Button rowDelete = (Button)row.findViewById(R.id.summary_row_delete);

                boolean isEditMode = (rowEdit.getVisibility() == View.VISIBLE);
                if (isEditMode) {
                    // Swap modes.
                    rowCategory.setVisibility(View.VISIBLE);
                    rowBy.setVisibility(View.VISIBLE);
                    rowDescription.setVisibility(View.VISIBLE);
                    rowEdit.setVisibility(View.GONE);
                    rowDelete.setVisibility(View.GONE);
                } else {
                    rowCategory.setVisibility(View.GONE);
                    rowBy.setVisibility(View.GONE);
                    rowDescription.setVisibility(View.GONE);
                    rowEdit.setVisibility(View.VISIBLE);
                    rowDelete.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });
    }

    private class GetHistoryListener implements GetRowsTask.GetRowsListener {
        private String mCategory;
        private int mNumDays;
        public GetHistoryListener(String category, int numDays) {
            mCategory = category;
            mNumDays = numDays;
        }

        @Override
        public void onGetRowsCompleted(List<Map<String, String>> history) {
            if (history != null) {
                refreshHistoryWidget(history, mCategory, mNumDays);
            }
        }
    }
}
