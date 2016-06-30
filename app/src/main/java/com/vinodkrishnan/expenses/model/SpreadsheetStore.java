package com.vinodkrishnan.expenses.model;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.model.AppendCellsRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.vinodkrishnan.expenses.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model for spreadsheet
 */
public class SpreadsheetStore {
    private String TAG = "SpreadsheetStore";

    private static SpreadsheetStore mInstance;

    private Activity mActivity;
    private Sheets mService;

    private SpreadsheetStore(Activity activity) {
        mActivity = activity;
    }

    public synchronized static SpreadsheetStore getInstance(Activity activity) {
        if (mInstance == null && activity != null) {
            mInstance = new SpreadsheetStore(activity);
        }
        return mInstance;
    }

    public List<Map<String, String>> getValues(String spreadSheetId, String sheetName)
            throws IOException {
        ValueRange response = getService().spreadsheets().values().get(spreadSheetId, sheetName)
                .execute();
        List<List<Object>> values = response.getValues();
        if (values != null && values.size() > 1) {
            List<Map<String, String>> mapValues = new ArrayList<>(values.size()-1);

            List<Object> firstRowList = values.get(0);
            for (int i = 1; i < values.size(); i++) {
                Map<String, String> rowMap = new HashMap<>(firstRowList.size());
                List<Object> nRowList = values.get(i);
                for (int j = 0; j < nRowList.size(); j++) {
                    rowMap.put((String)firstRowList.get(j), (String)nRowList.get(j));
                }
                mapValues.add(rowMap);
            }

            return mapValues;
        }
        return null;
    }

    public void appendRow(String spreadSheetId, String sheetName, List<CellData> cellDataList)
        throws IOException {
        int sheetId = getSheetId(spreadSheetId, sheetName);
        if (sheetId == -1) {
            Log.e(TAG, "Could not find the sheetId for " + sheetName);
            return;
        }

        final BatchUpdateSpreadsheetRequest req = new BatchUpdateSpreadsheetRequest()
                .setRequests(Arrays.asList(new Request[] {
                        new Request().setAppendCells(new AppendCellsRequest().setSheetId(sheetId)
                                .setFields("*")
                                .setRows(Arrays.asList(new RowData[] {
                                        new RowData().setValues(cellDataList)
                                })))
                }));

        getService().spreadsheets().batchUpdate(spreadSheetId, req).execute();
    }

    private Sheets getService() {
        if (mService == null) {
            GoogleAccountCredential credential = CredentialStore.getInstance(mActivity)
                    .getCredential();
            if (credential == null) {
                Log.e(TAG, "Credentials are not ready yet.");
            }
            mService = new Sheets.Builder(AndroidHttp.newCompatibleTransport(),
                    JacksonFactory.getDefaultInstance(), credential)
                    .setApplicationName(mActivity.getString(R.string.app_name))
                    .build();
        }
        return mService;
    }

    private int getSheetId(final String spreadSheetId, final String sheetName)
            throws IOException {
        List<Sheet> sheetsList =
                getService().spreadsheets().get(spreadSheetId).execute().getSheets();
        if (sheetsList != null) {
            for (int i = 0; i < sheetsList.size(); i++) {
                SheetProperties props = sheetsList.get(i).getProperties();
                if (TextUtils.equals(sheetName, props.getTitle())) {
                    return props.getSheetId();
                }
            }
        }
        return -1;
    }
}
