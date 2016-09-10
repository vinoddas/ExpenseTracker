package com.vinodkrishnan.expenses.tasks;

import android.app.Activity;
import android.text.TextUtils;

import com.google.api.services.sheets.v4.model.CellData;
import com.vinodkrishnan.expenses.model.SpreadsheetStore;
import com.vinodkrishnan.expenses.util.CommonUtil;

import java.util.List;

public class AddRowTask extends BaseTask<List<CellData>, Integer, Void> {
    private AddRowListener mListener;
    private String mSheetName;
    private String mRowNum;

    public AddRowTask(Activity activity, AddRowListener listener, String sheetName, String rowNum) {
        super(activity);
        mListener = listener;
        mSheetName = sheetName;
        mRowNum = rowNum;
    }

    @Override
    protected Void doInBackground(List<CellData>... params) {
        try {
            SpreadsheetStore store = SpreadsheetStore.getInstance(mActivity);
            if (TextUtils.isEmpty(mRowNum)) {
                store.appendRow(CommonUtil.getSpreadsheetId(mActivity), mSheetName, params[0]);
            } else {
                store.updateRow(CommonUtil.getSpreadsheetId(mActivity), mSheetName, params[0],
                        mRowNum);
            }
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
        if (mListener != null) {
            mListener.onAddRowCompleted();
        }
    }

    public interface AddRowListener {
        void onAddRowCompleted();
    }
}
