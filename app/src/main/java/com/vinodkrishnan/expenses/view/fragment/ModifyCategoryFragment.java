package com.vinodkrishnan.expenses.view.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.vinodkrishnan.expenses.R;
import com.vinodkrishnan.expenses.tasks.AddRowTask;
import com.vinodkrishnan.expenses.util.CommonUtil;

import java.util.Arrays;

/**
 *
 */
public class ModifyCategoryFragment extends Fragment implements View.OnClickListener, AddRowTask.AddRowListener {
    private EditText mNewCategoryEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.modify_category, container, false);
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
        Activity activity = getActivity();
        if (!CommonUtil.isNetworkConnected(activity)) {
            CommonUtil.showErrorDialog(activity, "Network connection does not seem to exist!");
            return;
        }

        String newCategory = mNewCategoryEditText.getText().toString();
        if (newCategory == null || newCategory.isEmpty()) {
            CommonUtil.showErrorDialog(activity, "New category can't be empty.");
            return;
        }

        new AddRowTask(activity, this, CommonUtil.getCategoriesSheetName(activity))
                .execute(Arrays.asList(new CellData[] {new CellData().setUserEnteredValue(
                                new ExtendedValue().setStringValue(newCategory))}));
    }

    @Override
    public void onAddRowCompleted() {
        CommonUtil.showToast(getActivity(), "Category added!");
        getActivity().recreate();
    }
}
