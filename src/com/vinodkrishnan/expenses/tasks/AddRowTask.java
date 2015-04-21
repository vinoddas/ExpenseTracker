package com.vinodkrishnan.expenses.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.vinodkrishnan.expenses.auth.AndroidAuthenticator;
import com.vinodkrishnan.expenses.sp.SpreadSheet;
import com.vinodkrishnan.expenses.sp.SpreadSheetFactory;
import com.vinodkrishnan.expenses.sp.WorkSheet;
import com.vinodkrishnan.expenses.util.CommonUtil;

import java.util.Map;

public class AddRowTask extends AsyncTask<Map<String, String>, Integer, Boolean> {
    private final String TAG = "AddRowTask";

    private AddRowListener mListener;
    private Activity mActivity;
    private String mSheetName;
    private SpreadSheetFactory mSpreadSheetFactory;

    public AddRowTask(Activity activity, AddRowListener listener, String sheetName) {
        this.mActivity = activity;
        this.mListener = listener;
        this.mSheetName = sheetName;
    }

    @Override
    protected Boolean doInBackground(Map<String, String>... params) {
        mSpreadSheetFactory = SpreadSheetFactory.getInstance(new AndroidAuthenticator(mActivity));
        SpreadSheet spreadSheet = mSpreadSheetFactory.getSpreadSheet(
                CommonUtil.getSpreadsheetName(mActivity));
        if (spreadSheet == null) {
            Log.e(TAG, "Could not find spreadsheet, maybe there is no internet.");
            return false;
        }
        WorkSheet workSheet = spreadSheet.getWorkSheet(mSheetName, true);
        if (workSheet == null) {
            Log.e(TAG, "Could not find the \"" + mSheetName + "\" worksheet.");
            return false;
        }

        return workSheet.addRow(params[0]);
    }

    @Override
    protected void onPostExecute(Boolean addSucceeded) {
        if (mListener != null) {
            mListener.onAddRowCompleted(addSucceeded);
        }
    }

}
