package com.vinodkrishnan.expenses.view.fragment;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.tasks.AddRowListener;
import com.vinodkrishnan.expenses.tasks.AddRowTask;
import com.vinodkrishnan.expenses.util.CommonUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 *
 */
public class ModifyCategoryFragment extends Fragment implements View.OnClickListener, AddRowListener {
    private final String TAG = "ModifyCategoryFragment";

    private TextView mErrorDialogTextView;
    private EditText mNewCategoryEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.modify_category, container, false);
        mErrorDialogTextView = (TextView) rootView.findViewById(R.id.modify_category_error_dialog);
        mNewCategoryEditText = (EditText) rootView.findViewById(R.id.modify_category_new_category);
        rootView.findViewById(R.id.modify_category_add_new_category_button).setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.modify_category_add_new_category_button:
                addCategoryAsync();
                break;
        }
    }

    private void addCategoryAsync() {
        if (!CommonUtil.isNetworkConnected(getActivity())) {
            CommonUtil.showDialog(mErrorDialogTextView, "Network connection does not seem to exist!", Color.RED);
            return;
        }

        String newCategory = mNewCategoryEditText.getText().toString();
        if (newCategory == null || newCategory.isEmpty()) {
            CommonUtil.showDialog(mErrorDialogTextView, "New category can't be empty.", Color.RED);
            return;
        }
        Map<String, String> values = new HashMap<String, String>();
        values.put("categories", newCategory);

        new AddRowTask(getActivity(), this, CommonUtil.getCategoriesSheetName(getActivity()))
                .execute(values);
    }

    @Override
    public boolean onAddRowCompleted(boolean addSucceeded) {
        if (addSucceeded) {
            CommonUtil.showDialog(mErrorDialogTextView, "Category added!", Color.YELLOW);
            Spinner categorySpinner = (Spinner)getActivity().findViewById(R.id.enter_expense_category);
            if (categorySpinner != null) {
                ArrayAdapter adapter = (ArrayAdapter) categorySpinner.getAdapter();
                adapter.add(mNewCategoryEditText.getText().toString());
                adapter.sort(new Comparator <String>() {
                    public int compare(String e1, String e2) {
                        return e1.compareTo(e2);
                    }
                });
            }
            mNewCategoryEditText.setText("");
            return true;
        } else {
            CommonUtil.showDialog(mErrorDialogTextView, "Some error during adding category!", Color.RED);
            return false;
        }
    }
}
