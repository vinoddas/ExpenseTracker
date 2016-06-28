package com.vinodkrishnan.expenses.tasks;

import android.app.Activity;

import com.google.api.services.sheets.v4.model.CellData;
import com.vinodkrishnan.expenses.model.SpreadsheetStore;
import com.vinodkrishnan.expenses.util.CommonUtil;

import java.util.List;

public class AddRowTask extends BaseTask<List<CellData>, Integer, Void> {
    private AddRowListener mListener;
    private String mSheetName;

    public AddRowTask(Activity activity, AddRowListener listener, String sheetName) {
        super(activity);
        mListener = listener;
        mSheetName = sheetName;
    }

    @Override
    protected Void doInBackground(List<CellData>... params) {
        try {
            SpreadsheetStore.getInstance(mActivity).appendRow(CommonUtil.getSpreadsheetId(mActivity),
                    mSheetName, params[0]);
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
