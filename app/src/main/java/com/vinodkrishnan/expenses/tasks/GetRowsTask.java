package com.vinodkrishnan.expenses.tasks;

import android.app.Activity;

import com.vinodkrishnan.expenses.model.SpreadsheetStore;
import com.vinodkrishnan.expenses.util.CommonUtil;

import java.util.List;
import java.util.Map;

public class GetRowsTask extends BaseTask<String, Integer, List<Map<String, String>>>  {
    private GetRowsListener mListener;
    public GetRowsTask(Activity activity, GetRowsListener listener) {
        super(activity);
        mListener = listener;
    }

    @Override
    protected List<Map<String, String>> doInBackground(String... params) {
        try {
            return SpreadsheetStore.getInstance(mActivity).getValues(
                    CommonUtil.getSpreadsheetId(mActivity), params[0]);
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<Map<String, String>> rows) {
        if (mListener != null) {
            mListener.onGetRowsCompleted(rows);
        }
    }

    public interface GetRowsListener {
        void onGetRowsCompleted(List<Map<String, String>> rows);
    }
}
