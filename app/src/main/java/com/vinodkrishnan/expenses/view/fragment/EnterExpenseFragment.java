package com.vinodkrishnan.expenses.view.fragment;

import android.content.SharedPreferences;
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
import android.widget.TableRow;
import android.widget.TextView;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.gson.Gson;
import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.model.CredentialStore;
import com.vinodkrishnan.expenses.tasks.AddRowTask;
import com.vinodkrishnan.expenses.tasks.GetRowsTask;
import com.vinodkrishnan.expenses.util.CommonUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.vinodkrishnan.expenses.util.CommonUtil.AMOUNT_COLUMN;
import static com.vinodkrishnan.expenses.util.CommonUtil.BY_COLUMN;
import static com.vinodkrishnan.expenses.util.CommonUtil.CATEGORIES_PREF_KEY;
import static com.vinodkrishnan.expenses.util.CommonUtil.CATEGORY_COLUMN;
import static com.vinodkrishnan.expenses.util.CommonUtil.DATE_COLUMN;
import static com.vinodkrishnan.expenses.util.CommonUtil.DESCRIPTION_COLUMN;
import static com.vinodkrishnan.expenses.util.CommonUtil.EXPENSES_PREF_KEY;
import static com.vinodkrishnan.expenses.util.CommonUtil.ROW_NUM_KEY;

public class EnterExpenseFragment extends Fragment implements View.OnClickListener, AddRowTask.AddRowListener {
    private final String TAG = "EnterExpenseFragment";

    private static final String BY_PREF_KEY = "com.vinodkrishnan.expenses.pref_by";
    private static final String TYPE_PREF_KEY = "com.vinodkrishnan.expenses.pref_type";

    private static final String CATEGORIES_CELL = "Categories";

    private final Set<String> mCategories = new TreeSet<String>();
    private SharedPreferences mPrefs;

    private Spinner mCategorySpinner;
    private TextView mDateTextView;
    private Spinner mBySpinner;
    private Spinner mTypeSpinner;
    private EditText mAmountEditText;
    private EditText mDescriptionEditText;
    private TableRow mRowNumTableRow;
    private TextView mRowNumTextView;

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
        mRowNumTableRow = (TableRow) rootView.findViewById(R.id.enter_expense_row_num);
        mRowNumTextView = (TextView) rootView.findViewById(R.id.enter_expense_row_num_value);
        rootView.findViewById(R.id.enter_expense_add_expense_button).setOnClickListener(this);
        rootView.findViewById(R.id.enter_expense_pick_date).setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setDefaults();
        if (getArguments() != null && !getArguments().isEmpty()) {
            mRowNumTableRow.setVisibility(View.VISIBLE);
            setEditDefaults(getArguments());
        } else {
            mRowNumTableRow.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean networkConnected = CommonUtil.isNetworkConnected(getActivity());
        if (mCategories.isEmpty() && networkConnected &&
                CredentialStore.getInstance(getActivity()).isReady()) {
            new GetRowsTask(getActivity(), new GetCategoriesListener()).execute(
                    CommonUtil.getCategoriesSheetName(getActivity()));
            syncOfflineExpenses();
        }
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
    public void onAddRowCompleted() {
        CommonUtil.showToast(getActivity(), R.string.expense_added);
        resetFields();
    }

    private void addExpenseAsync() {
        Map<String, String> values = constructValues();
        if (values != null) {
            if (!CommonUtil.isNetworkConnected(getActivity())) {
                CommonUtil.showErrorDialog(getActivity(), R.string.error_saving_offline);
                addExpenseOffline(values);
            } else {
                addValues(values);
            }
        }
   }

    private Map<String, String> constructValues() {
        double amount;
        try {
            amount = Double.parseDouble(mAmountEditText.getText().toString());
            if (amount == 0.0) {
                CommonUtil.showErrorDialog(getActivity(), R.string.error_amount_positive);
                return null;
            }
        } catch (NumberFormatException e) {
            CommonUtil.showErrorDialog(getActivity(), R.string.error_amount_positive);
            return null;
        }

        String type = mTypeSpinner.getSelectedItem().toString();

        Map<String, String> values = new HashMap<>();
        String date = mDateTextView.getText().toString();
        values.put(DATE_COLUMN, date);
        // Set a negative number if it is Income.
        values.put(AMOUNT_COLUMN, new DecimalFormat("#.00").format(
                "Income".equals(type) ? -amount : amount));
        values.put(CATEGORY_COLUMN, mCategorySpinner.getSelectedItem().toString());
        values.put(BY_COLUMN, mBySpinner.getSelectedItem().toString());
        values.put(DESCRIPTION_COLUMN, mDescriptionEditText.getText().toString());
        values.put(ROW_NUM_KEY, mRowNumTextView.getText().toString());

        return values;
    }

    private void addValues(Map<String, String> values) {
        String date = values.get("Date");
        new AddRowTask(getActivity(), this, date.substring(date.lastIndexOf("/") + 1))
                .execute(convertMapToCellData(values));
    }

    private List<CellData> convertMapToCellData(Map<String, String> values) {
        List<CellData> cellDataList = new ArrayList<>(values.size());
        // TODO: The ordering and keys are hardcoded here, verify the ordering
        // Date, Amount, Category, By, Description.
        cellDataList.add(new CellData().setUserEnteredValue(
                new ExtendedValue().setStringValue(values.get(DATE_COLUMN))));
        cellDataList.add(new CellData().setUserEnteredValue(
                new ExtendedValue().setStringValue(values.get(AMOUNT_COLUMN))));
        cellDataList.add(new CellData().setUserEnteredValue(
                new ExtendedValue().setStringValue(values.get(CATEGORY_COLUMN))));
        cellDataList.add(new CellData().setUserEnteredValue(
                new ExtendedValue().setStringValue(values.get(BY_COLUMN))));
        cellDataList.add(new CellData().setUserEnteredValue(
                new ExtendedValue().setStringValue(values.get(DESCRIPTION_COLUMN))));
        // TODO: Use RowNum to do something for editing.
        return cellDataList;
    }

    private void addExpenseOffline(Map<String, String> values) {
        Set<String> offlineExpenses;
        if (mPrefs.contains(EXPENSES_PREF_KEY)) {
            offlineExpenses = mPrefs.getStringSet(EXPENSES_PREF_KEY, null);
        } else {
            offlineExpenses = new HashSet<>();
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

    private void setEditDefaults(Bundle editArgs) {
        // TODO: We expect all these fields to be set, log error.
        mRowNumTextView.setText(editArgs.getString(ROW_NUM_KEY));
        mDateTextView.setText(editArgs.getString(DATE_COLUMN));
        int pos = CommonUtil.getPositionFromSpinner(
                mCategorySpinner, editArgs.getString(CATEGORY_COLUMN));
        if (pos >= 0) {
            mCategorySpinner.setSelection(pos);
        }
        pos = CommonUtil.getPositionFromSpinner(mBySpinner, editArgs.getString(BY_COLUMN));
        if (pos >= 0) {
            mBySpinner.setSelection(pos);
        }

        mDescriptionEditText.setText(editArgs.getString(DESCRIPTION_COLUMN));
        double amount = Double.parseDouble(editArgs.getString(AMOUNT_COLUMN));
        if (amount > 0) {
            mAmountEditText.setText(String.valueOf(amount));
            pos = CommonUtil.getPositionFromSpinner(mTypeSpinner, "Expense");
            if (pos >= 0) {
                mTypeSpinner.setSelection(pos);
            }
        } else {
            mAmountEditText.setText(String.valueOf(-amount));
            pos = CommonUtil.getPositionFromSpinner(mTypeSpinner, "Income");
            if (pos > 0) {
                mTypeSpinner.setSelection(pos);
            }
        }
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

    private class GetCategoriesListener implements GetRowsTask.GetRowsListener {
        @Override
        public void onGetRowsCompleted(List<Map<String, String>> rows) {
            if (rows == null || rows.isEmpty()) {
                Log.e(TAG, "Got back empty results.");
                return;
            }

            synchronized (mCategories) {
                mCategories.clear();
                for (Map<String, String> row : rows) {
                    if (row.containsKey(CATEGORIES_CELL)) {
                        mCategories.add(row.get(CATEGORIES_CELL));
                    }
                }
                Log.d(TAG, "Found " + mCategories.size() + " categories.");
                // Store the categories in prefs.
                mPrefs.edit().putStringSet(CATEGORIES_PREF_KEY, mCategories).apply();
            }

            setCategoriesSpinner();
        }
    }
}
