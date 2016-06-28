package com.vinodkrishnan.expenses.view.fragment;

import static com.vinodkrishnan.expenses.util.CommonUtil.EXPENSES_PREF_KEY;
import static com.vinodkrishnan.expenses.util.CommonUtil.CATEGORIES_PREF_KEY;

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

public class EnterExpenseFragment extends Fragment implements View.OnClickListener, AddRowTask.AddRowListener {
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
        rootView.findViewById(R.id.enter_expense_add_expense_button).setOnClickListener(this);
        rootView.findViewById(R.id.enter_expense_pick_date).setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setDefaults();
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
        CommonUtil.showToast(getActivity(), "Expense/Income added!");
        resetFields();
    }

    private void addExpenseAsync() {
        Map<String, String> values = constructValues();
        if (values != null) {
            if (!CommonUtil.isNetworkConnected(getActivity())) {
                CommonUtil.showErrorDialog(getActivity(), "No network connection, so saving offline");
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
                CommonUtil.showErrorDialog(getActivity(), "Amount cannot be 0.");
                return null;
            }
        } catch (NumberFormatException e) {
            CommonUtil.showErrorDialog(getActivity(), "Amount has to be a number greater than 0.");
            return null;
        }

        String type = mTypeSpinner.getSelectedItem().toString();

        Map<String, String> values = new HashMap<>();
        String date = mDateTextView.getText().toString();
        values.put("Date", date);
        // Set a negative number if it is Income.
        values.put("Amount", new DecimalFormat("#.00").format(
                "Income".equals(type) ? -amount : amount));
        values.put("Category", mCategorySpinner.getSelectedItem().toString());
        values.put("By", mBySpinner.getSelectedItem().toString());
        values.put("Description", mDescriptionEditText.getText().toString());

        return values;
    }

    private void addValues(Map<String, String> values) {
        String date = values.get("Date");
        String category = values.get("Category");
        synchronized (mCategories) {
            if (!mCategories.contains(category)) {
                setCategoriesSpinner();
                CommonUtil.showErrorDialog(getActivity(), "Categories got changed!");
                return;
            }
        }
        new AddRowTask(getActivity(), this, date.substring(date.lastIndexOf("/") + 1))
                .execute(convertMapToCellData(values));
    }

    private List<CellData> convertMapToCellData(Map<String, String> values) {
        List<CellData> cellDataList = new ArrayList<>(values.size());
        // TODO: The ordering and keys are hardcoded here, verify the ordering
        // Date, Amount, Category, By, Description.
        cellDataList.add(new CellData().setUserEnteredValue(
                new ExtendedValue().setStringValue(values.get("Date"))));
        cellDataList.add(new CellData().setUserEnteredValue(
                new ExtendedValue().setStringValue(values.get("Amount"))));
        cellDataList.add(new CellData().setUserEnteredValue(
                new ExtendedValue().setStringValue(values.get("Category"))));
        cellDataList.add(new CellData().setUserEnteredValue(
                new ExtendedValue().setStringValue(values.get("By"))));
        cellDataList.add(new CellData().setUserEnteredValue(
                new ExtendedValue().setStringValue(values.get("Description"))));
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
                    if (row.containsKey("Categories")) {
                        mCategories.add(row.get("Categories"));
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
