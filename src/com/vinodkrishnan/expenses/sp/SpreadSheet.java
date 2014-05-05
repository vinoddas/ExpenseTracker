package com.vinodkrishnan.expenses.sp;

import android.util.Log;
import android.util.Xml;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
*
*/
public class SpreadSheet {
    private static final String TAG = "SpreadSheet";

    public String id;
    public String title;
    public String key;

    private List<WorkSheet> mWorkSheets;
    private String mAuthToken;
    private HttpClient mHttpClient;

    public SpreadSheet(String authToken, HttpClient httpClient) {
        mAuthToken = authToken;
        mHttpClient = httpClient;
    }

    public List<WorkSheet> getWorkSheets() {
        return getWorkSheets(false);
    }

    public List<WorkSheet> getWorkSheets(boolean reload) {
        if (mWorkSheets == null || reload) {
            mWorkSheets = null;
            loadWorkSheetsFromServer();
        }
        return mWorkSheets;
    }

    public void setWorkSheets(List<WorkSheet> workSheets) {
        mWorkSheets = workSheets;
    }

    public WorkSheet getWorkSheet(String title) {
        return getWorkSheet(title, false);
    }

    public WorkSheet getWorkSheet(String title, boolean reload) {
        getWorkSheets(reload);
        if (title == null) {
            return null;
        }
        for (WorkSheet workSheet : mWorkSheets) {
            if (title.equals(workSheet.title)) {
                return workSheet;
            }
        }
        return null;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Id: ");
        buf.append(id);
        buf.append(", Title: ");
        buf.append(title);
        buf.append(", Key: ");
        buf.append(key);
        if (mWorkSheets != null) {
            buf.append(", WorkSheet {");
            for (WorkSheet workSheet : mWorkSheets) {
                buf.append("[");
                buf.append(workSheet);
                buf.append("]");
            }
            buf.append("}");
        }
        return buf.toString();
    }

    private void loadWorkSheetsFromServer() {
        try {
            HttpGet httpGet = new HttpGet("https://spreadsheets.google.com/feeds/worksheets/" + key + "/private/full");
            httpGet.setHeader("Authorization", "GoogleLogin auth="+ mAuthToken);
            httpGet.setHeader("GData-Version", "3.0");
            HttpResponse response = mHttpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                mWorkSheets = parseWorksheetInfo(entity.getContent());
                Log.d(TAG, "Found " + mWorkSheets.size() + " worksheets.");
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XmlPullParserException", e);
        }
    }

    private List<WorkSheet> parseWorksheetInfo(InputStream in) throws IOException, XmlPullParserException {
        if (in == null) {
            Log.e(TAG, "Input stream is null");
            return null;
        }
        List<WorkSheet> workSheets = new ArrayList<WorkSheet>();

        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(in, null);
        parser.nextTag();

        WorkSheet workSheet = null;
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            int eventType = parser.getEventType();
            String name = parser.getName();
            if (name == null) {
                continue;
            }
            // Starts by looking for the entry tag
            if (name.equals("entry")) {
                if (eventType == XmlPullParser.START_TAG) {
                    workSheet = new WorkSheet(mAuthToken, mHttpClient);
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (workSheet != null) {
                        workSheet.spreadSheetKey = key;
                        Log.d(TAG, "Parsed workSheet: " + workSheet);
                        workSheets.add(workSheet);
                        workSheet = null;
                    }
                }
            } else if (workSheet != null && eventType == XmlPullParser.START_TAG) {
                if (name.equals("title") && parser.next() == XmlPullParser.TEXT) {
                    workSheet.title = parser.getText();
                    parser.nextTag();
                } else if (name.equals("id") && parser.next() == XmlPullParser.TEXT) {
                    String id = parser.getText();
                    workSheet.id = id;
                    workSheet.key = id.substring(id.lastIndexOf("/") + 1);
                    parser.nextTag();
                } else if (name.equals("gs:rowCount") && parser.next() == XmlPullParser.TEXT) {
                    workSheet.rowCount = Integer.parseInt(parser.getText());
                    parser.nextTag();
                } else if (name.equals("gs:colCount") && parser.next() == XmlPullParser.TEXT) {
                    workSheet.colCount = Integer.parseInt(parser.getText());
                    parser.nextTag();
                }
            }
        }
        in.close();

        return workSheets;
    }
}
