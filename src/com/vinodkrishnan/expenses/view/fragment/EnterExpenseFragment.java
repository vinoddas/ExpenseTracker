package com.vinodkrishnan.expenses.view.fragment;

import static com.vinodkrishnan.expenses.util.CommonUtil.EXPENSES_PREF_KEY;
import static com.vinodkrishnan.expenses.util.CommonUtil.CATEGORIES_PREF_KEY;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.tasks.AddRowListener;
import com.vinodkrishnan.expenses.tasks.AddRowTask;
import com.vinodkrishnan.expenses.tasks.GetRowsListener;
import com.vinodkrishnan.expenses.tasks.GetRowsTask;
import com.vinodkrishnan.expenses.util.CommonUtil;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class EnterExpenseFragment extends Fragment implements View.OnClickListener, AddRowListener {
    private final String TAG = "EnterExpenseFragment";

    private static final String BY_PREF_KEY = "com.vinodkrishnan.expenses.pref_by";
    private static final String TYPE_PREF_KEY = "com.vinodkrishnan.expenses.pref_type";

    private final Set<String> mCategories = new TreeSet<String>();
    private SharedPreferences mPrefs;

    private Spinner mCategorySpinner;
    private TextView mDateTextView;
    private Spinner mBySpinner;
    private Spinner mTypeSpinner;
    private EditText mAmountEditText;
    private EditText mDescriptionEditText;
    private TextView mErrorDialogTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.enter_expense, container, false);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mCategorySpinner = (Spinner) rootView.findViewById(R.id.enter_expense_category);
        mDateTextView = (TextView) rootView.findViewById(R.id.enter_expense_date);
        mBySpinner = (Spinner) rootView.findViewById(R.id.enter_expense_by);
        mTypeSpinner = (Spinner) rootView.findViewById(R.id.enter_expense_type);
        mAmountEditText = (EditText) rootView.findViewById(R.id.enter_expense_amount);
        mDescriptionEditText = (EditText) rootView.findViewById(R.id.enter_expense_description);
        mErrorDialogTextView = (TextView) rootView.findViewById(R.id.enter_expense_error_dialog);
        rootView.findViewById(R.id.enter_expense_add_expense_button).setOnClickListener(this);
        rootView.findViewById(R.id.enter_expense_pick_date).setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (CommonUtil.isNetworkConnected(getActivity())) {
            new GetRowsTask(getActivity(), new GetCategoriesListener()).execute(
                    CommonUtil.getCategoriesSheetName(getActivity()));
            syncOfflineExpenses();
        }
        setDefaults();
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.enter_expense_add_expense_button:
                addExpenseAsync();
                break;
            case R.id.enter_expense_pick_date:
                if (mDateTextView != null) {
                    DialogFragment newFragment = new DatePickerFragment(mDateTextView);
                    newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
                }
                break;
        }
    }

    @Override
    public boolean onAddRowCompleted(boolean addSucceeded) {
        if (addSucceeded) {
            CommonUtil.showDialog(mErrorDialogTextView, "Expense/Income added!", Color.YELLOW);
            resetFields();
            return true;
        } else {
            CommonUtil.showDialog(mErrorDialogTextView, "Some error during adding expense!", Color.RED);
            return false;
        }
    }

    private void addExpenseAsync() {
        Map<String, String> values = constructValues();
        if (values != null) {
            if (!CommonUtil.isNetworkConnected(getActivity())) {
                CommonUtil.showDialog(mErrorDialogTextView, "No network connection, so saving offline", Color.RED);
                addExpenseOffline(values);
            } else {
                addValues(values);
            }
        }
   }

    private Map<String, String> constructValues() {
        double amount = 0.0;
        try {
            amount = Double.parseDouble(mAmountEditText.getText().toString());
            if (amount == 0.0) {
                CommonUtil.showDialog(mErrorDialogTextView, "Amount cannot be 0.", Color.RED);
                return null;
            }
        } catch (NumberFormatException e) {
            CommonUtil.showDialog(mErrorDialogTextView, "Amount has to be a number greater than 0.", Color.RED);
            return null;
        }

        String type = mTypeSpinner.getSelectedItem().toString();
        // TODO: Verify the keys by checking the column headers in the worksheet.
        Map<String, String> values = new HashMap<String, String>();
        String date = mDateTextView.getText().toString();
        values.put("date", date);
        // Set a negative number if it is Income.
        values.put("amount", new DecimalFormat("#.00").format(
                "Income".equals(type) ? -amount : amount));
        values.put("category", mCategorySpinner.getSelectedItem().toString());
        values.put("by", mBySpinner.getSelectedItem().toString());
        values.put("description", mDescriptionEditText.getText().toString());

        return values;
    }

    private void addValues(Map<String, String> values) {
        String date = values.get("date");
        String category = values.get("category");
        synchronized (mCategories) {
            if (!mCategories.contains(category)) {
                setCategoriesSpinner();
                CommonUtil.showDialog(mErrorDialogTextView, "Categories got changed!", Color.RED);
                return;
            }
        }
        new AddRowTask(getActivity(), this, date.substring(date.lastIndexOf("/") + 1))
                .execute(values);
    }

    private void addExpenseOffline(Map<String, String> values) {
        Set<String> offlineExpenses;
        if (mPrefs.contains(EXPENSES_PREF_KEY)) {
            offlineExpenses = mPrefs.getStringSet(EXPENSES_PREF_KEY, null);
        } else {
            offlineExpenses = new HashSet<String>();
        }
        offlineExpenses.add(new Gson().toJson(values));
        mPrefs.edit().putStringSet(EXPENSES_PREF_KEY, offlineExpenses).commit();
    }

    private void syncOfflineExpenses() {
        if (mPrefs.contains(EXPENSES_PREF_KEY)) {
            for (String expense : mPrefs.getStringSet(EXPENSES_PREF_KEY, null)) {
                Log.d(TAG, "Adding offline expense " + expense);
                addValues(new Gson().fromJson(expense, Map.class));
            }
        }
        mPrefs.edit().remove(EXPENSES_PREF_KEY).commit();
    }

    private void setDefaults() {
        // Set the default time.
        final Calendar c = Calendar.getInstance();
        mDateTextView.setText(c.get(Calendar.MONTH) + 1 + "/" + c.get(Calendar.DAY_OF_MONTH) + "/"
                + c.get(Calendar.YEAR));
        if (mBySpinner != null) {
            ArrayAdapter adapter = ArrayAdapter.createFromResource(
                    getActivity(), R.array.by_entries, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mBySpinner.setAdapter(adapter);
            if (mPrefs.contains(BY_PREF_KEY)) {
                mBySpinner.setSelection(adapter.getPosition(mPrefs.getString(BY_PREF_KEY, "")));
            }
        }
        if (mTypeSpinner != null) {
            ArrayAdapter adapter = ArrayAdapter.createFromResource(
                    getActivity(), R.array.type_entries, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mTypeSpinner.setAdapter(adapter);
            if (mPrefs.contains(TYPE_PREF_KEY)) {
                mTypeSpinner.setSelection(adapter.getPosition(mPrefs.getString(TYPE_PREF_KEY, "")));
            }

        }
        if (mPrefs.contains(CATEGORIES_PREF_KEY)) {
            synchronized (mCategories) {
                mCategories.clear();
                mCategories.addAll(mPrefs.getStringSet(CATEGORIES_PREF_KEY, null));
                setCategoriesSpinner();
            }
        }
    }

    private void setCategoriesSpinner() {
        synchronized (mCategories) {
            ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, mCategories.toArray(new String[0]));
            mCategorySpinner.setAdapter(adapter);
        }
    }

    private void resetFields() {
        mAmountEditText.setText("");
        mDescriptionEditText.setText("");
        // Store the By field in prefs.
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(BY_PREF_KEY, mBySpinner.getSelectedItem().toString());
        editor.putString(TYPE_PREF_KEY, mTypeSpinner.getSelectedItem().toString());
        editor.commit();
    }

    private class GetCategoriesListener implements GetRowsListener {
        @Override
        public boolean onGetRowsCompleted(List<Map<String, String>> rows) {
            if (rows == null) {
                return false;
            }

            synchronized (mCategories) {
                mCategories.clear();
                for (Map<String, String> row : rows) {
                    if (row.containsKey("categories")) {
                        mCategories.add(row.get("categories"));
                    }
                }
                Log.d(TAG, "Found " + mCategories.size() + " categories.");
                // Store the categories in prefs.
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putStringSet(CATEGORIES_PREF_KEY, mCategories);
                editor.commit();
            }
            setCategoriesSpinner();

            return true;
        }
    }
}
