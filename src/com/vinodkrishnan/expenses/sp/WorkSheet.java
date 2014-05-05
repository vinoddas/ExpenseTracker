package com.vinodkrishnan.expenses.sp;

import android.util.Log;
import android.util.Xml;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
*
*/
public class WorkSheet {
    private static final String TAG = "WorkSheet";
    public String id;
    public String title;
    public String spreadSheetKey;
    public String key;
    public int rowCount;
    public int colCount;
    public List<Map<String, String>> values;

    private String mAuthToken;
    private HttpClient mHttpClient;

    public WorkSheet(String authToken, HttpClient httpClient) {
        mAuthToken = authToken;
        mHttpClient = httpClient;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Id: ");
        buf.append(id);
        buf.append(", Title: ");
        buf.append(title);
        buf.append(", Key: ");
        buf.append(key);
        buf.append(", Row Count: ");
        buf.append(rowCount);
        buf.append(", Col Count: ");
        buf.append(colCount);
        return buf.toString();
    }

    public boolean addRow(Map<String, String> values) {
        if (values == null || values.size() == 0) {
            Log.e(TAG, "Row data cannot be null or empty.");
            return false;
        }
        try {
            String url = "https://spreadsheets.google.com/feeds/list/" + spreadSheetKey + "/" + key + "/private/full";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "GoogleLogin auth="+ mAuthToken);
            httpPost.setHeader("GData-Version", "3.0");
            httpPost.setHeader("Content-Type", "application/atom+xml");
            // Set the Post Data
            StringBuffer postData = new StringBuffer();
            postData.append("<entry xmlns=\"http://www.w3.org/2005/Atom\" " +
                    "xmlns:gsx=\"http://schemas.google.com/spreadsheets/2006/extended\">");
            for (String valueKey : values.keySet()) {
                postData.append(" <gsx:" + valueKey + ">" + values.get(valueKey) + "</gsx:" + valueKey + ">");
            }
            postData.append("</entry>") ;
            httpPost.setEntity(new StringEntity(postData.toString()));

            HttpResponse response = mHttpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                java.util.Scanner s = new java.util.Scanner(response.getEntity().getContent()).useDelimiter("\\A");
                Log.e(TAG, s.hasNext() ? s.next() : "");
            }
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED;
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }
        return false;
    }

    public List<Map<String, String>> readValues(boolean refresh) {
        if (values != null && !refresh)
            return values;
        values = new ArrayList<Map<String, String>>(rowCount);
        try {
            HttpGet httpGet = new HttpGet("https://spreadsheets.google.com/feeds/list/" +
                    spreadSheetKey + "/" + key + "/private/full");
            httpGet.setHeader("Authorization", "GoogleLogin auth="+ mAuthToken);
            httpGet.setHeader("GData-Version", "3.0");
            HttpResponse response = new DefaultHttpClient().execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(entity.getContent(), null);
                parser.nextTag();

                boolean inEntry = false;
                Map<String, String> rowValues = null;
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.getEventType() != XmlPullParser.START_TAG &&
                        parser.getEventType() != XmlPullParser.END_TAG) {
                        continue;
                    }
                    String tagName = parser.getName();
                    if (tagName.equals("entry")) {
                        if (parser.getEventType() == XmlPullParser.START_TAG) {
                            inEntry = true;
                            rowValues = new HashMap<String, String>();
                        } else {
                            inEntry = false;
                            values.add(rowValues);
                        }
                    } else if (inEntry) {
                        if (tagName.startsWith("gsx:") && parser.getEventType() == XmlPullParser.START_TAG &&
                            parser.next() == XmlPullParser.TEXT) {
                            String colName = tagName.substring(4);
                            rowValues.put(colName, parser.getText());
                            parser.nextTag();
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XmlPullParserException", e);
        }
        return values;
    }
}
