package com.vinodkrishnan.expenses.tasks;

import android.app.Activity;
import android.text.TextUtils;

import com.google.api.services.sheets.v4.model.CellData;
import com.vinodkrishnan.expenses.model.SpreadsheetStore;
import com.vinodkrishnan.expenses.util.CommonUtil;

import java.util.List;

public class DeleteRowTask extends BaseTask<Void, Integer, Void> {
    private String mSheetName;
    private String mRowNum;

    public DeleteRowTask(Activity activity, String sheetName, String rowNum) {
        super(activity);
        mSheetName = sheetName;
        mRowNum = rowNum;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            SpreadsheetStore store = SpreadsheetStore.getInstance(mActivity);
            store.deleteRow(CommonUtil.getSpreadsheetId(mActivity), mSheetName, mRowNum);
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
        }
        return null;
    }
}
