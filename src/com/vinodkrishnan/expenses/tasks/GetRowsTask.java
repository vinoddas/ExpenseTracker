package com.vinodkrishnan.expenses.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.vinodkrishnan.expenses.auth.AndroidAuthenticator;
import com.vinodkrishnan.expenses.sp.SpreadSheet;
import com.vinodkrishnan.expenses.sp.SpreadSheetFactory;
import com.vinodkrishnan.expenses.sp.WorkSheet;
import com.vinodkrishnan.expenses.util.CommonUtil;

import java.util.List;
import java.util.Map;

public class GetRowsTask extends AsyncTask<String, Integer, List<Map<String, String>>>  {
    private final String TAG = "GetRowsTask";

    private GetRowsListener mListener;
    private Activity mActivity;
    private SpreadSheetFactory mSpreadSheetFactory;
    public GetRowsTask(Activity activity, GetRowsListener listener) {
        this.mActivity = activity;
        this.mListener = listener;
    }

    @Override
    protected List<Map<String, String>> doInBackground(String... params) {
        mSpreadSheetFactory = SpreadSheetFactory.getInstance(new AndroidAuthenticator(mActivity));
        SpreadSheet spreadSheet = mSpreadSheetFactory.getSpreadSheet(
                CommonUtil.getSpreadsheetName(mActivity));
        if (spreadSheet == null) {
            Log.e(TAG, "Could not find spreadsheet, maybe there is no internet.");
            return null;
        }
        String sheetName = params[0];
        WorkSheet workSheet = spreadSheet.getWorkSheet(sheetName, true);
        if (workSheet == null) {
            Log.e(TAG, "Could not find the \"" + sheetName + "\" worksheet.");
            return null;
        }
        return workSheet.readValues(true);
    }

    @Override
    protected void onPostExecute(List<Map<String, String>> rows) {
        if (mListener != null) {
            mListener.onGetRowsCompleted(rows);
        }
    }
}
