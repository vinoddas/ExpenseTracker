package com.vinodkrishnan.expenses.view.fragment;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.auth.AndroidAuthenticator;
import com.vinodkrishnan.expenses.sp.SpreadSheet;
import com.vinodkrishnan.expenses.sp.SpreadSheetFactory;
import com.vinodkrishnan.expenses.sp.WorkSheet;
import com.vinodkrishnan.expenses.tasks.AddRowListener;
import com.vinodkrishnan.expenses.tasks.AddRowTask;
import com.vinodkrishnan.expenses.tasks.GetRowsListener;
import com.vinodkrishnan.expenses.tasks.GetRowsTask;
import com.vinodkrishnan.expenses.util.CommonUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnterExpenseFragment extends Fragment implements View.OnClickListener, AddRowListener {
    private final String TAG = "EnterExpenseFragment";

    private static final String BY_PREF_KEY = "com.vinodkrishnan.expenses.pref_by";
    private static final String TYPE_PREF_KEY = "com.vinodkrishnan.expenses.pref_type";

    private List<String> mCategories;
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
            setDefaults();
        } else {
            CommonUtil.showDialog(mErrorDialogTextView, "Network connection does not seem to exist!", Color.RED);
        }
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

    private void addExpenseAsync() {
        if (!CommonUtil.isNetworkConnected(getActivity())) {
            CommonUtil.showDialog(mErrorDialogTextView, "Network connection does not seem to exist!", Color.RED);
            return;
        }

        double amount = 0.0;
        try {
            amount = Double.parseDouble(mAmountEditText.getText().toString());
            if (amount == 0.0) {
                CommonUtil.showDialog(mErrorDialogTextView, "Amount cannot be 0.", Color.RED);
                return;
            }
        } catch (NumberFormatException e) {
            CommonUtil.showDialog(mErrorDialogTextView, "Amount has to be a number greater than 0.", Color.RED);
            return;
        }

        // Just in case the categories got change in between loading the application and submitting an expense.
        String category = mCategorySpinner.getSelectedItem().toString();
        if (!mCategories.contains(category)) {
            setCategoriesSpinner();
            CommonUtil.showDialog(mErrorDialogTextView, "Categories got changed!", Color.RED);
            return;
        }
        String type = mTypeSpinner.getSelectedItem().toString();
        DecimalFormat df = new DecimalFormat("#.00");
        // TODO: Verify the keys by checking the column headers in the worksheet.
        Map<String, String> values = new HashMap<String, String>();
        String date = mDateTextView.getText().toString();
        values.put("date", date);
        // Set a negative number if it is Income.
        values.put("amount", df.format("Income".equals(type) ? -amount : amount));
        values.put("category", category);
        values.put("by", mBySpinner.getSelectedItem().toString());
        values.put("description", mDescriptionEditText.getText().toString());

        new AddRowTask(getActivity(), this, date.substring(date.lastIndexOf("/") + 1))
                .execute(values);
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
    }

    private void setCategoriesSpinner() {
        if (mCategories != null) {
            ArrayAdapter adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_dropdown_item, mCategories);
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
            EnterExpenseFragment.this.mCategories = categories;
            EnterExpenseFragment.this.setCategoriesSpinner();

            return true;
        }
    }
}
