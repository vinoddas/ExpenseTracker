package com.vinodkrishnan.expenses.sp;

import android.util.Log;
import android.util.Xml;
import com.vinodkrishnan.expenses.auth.Authenticator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

/**
 *
 */
public class SpreadSheetFactory {
    private String TAG = "SpreadSheetFactory";

    private final String SP_GET_LIST_URL = "https://spreadsheets.google.com/feeds/spreadsheets/private/full";
    private final static String SPREADSHEET_API_SERVICE_NAME = "wise";

    private static SpreadSheetFactory mInstance;

    private Authenticator mAuthenticator;
    private HttpClient mHttpClient;

    private String mAuthToken;


    private SpreadSheetFactory(Authenticator authenticator) {
        mAuthenticator = authenticator;
        mHttpClient = new DefaultHttpClient();
    }

    public static SpreadSheetFactory getInstance(Authenticator authenticator) {
        if (mInstance == null && authenticator != null) {
            mInstance = new SpreadSheetFactory(authenticator);
        }
        return mInstance;
    }

    public String getAuthToken() {
        return mAuthToken;
    }

    public SpreadSheet getSpreadSheet(String title) {
        mAuthToken = mAuthenticator.getAuthToken(SPREADSHEET_API_SERVICE_NAME, true);

        SpreadSheet spreadSheet = null;
        try {
            HttpGet httpGet = new HttpGet(SP_GET_LIST_URL + "?title=" + URLEncoder.encode(title, "UTF-8") + "&title-exact=true");
            httpGet.setHeader("Authorization", "GoogleLogin auth=" + mAuthToken);
            httpGet.setHeader("GData-Version", "3.0");
            HttpResponse response = mHttpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && entity != null) {
                spreadSheet = getSpreadSheetInfo(entity.getContent());
                Log.d(TAG, "SpreadSheet: "+ spreadSheet);
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XmlPullParserException", e);
        }

        return spreadSheet;
    }

    private SpreadSheet getSpreadSheetInfo(InputStream in) throws IOException, XmlPullParserException {
        if (in == null) {
            Log.e(TAG, "Input stream is null.");
            return null;
        }

        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(in, null);
        parser.nextTag();

        boolean entryFound = false;
        SpreadSheet parsedSpreadsheet = new SpreadSheet(mAuthToken, mHttpClient);
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("entry")) {
                entryFound = true;
            } else if (name.equals("title") && parser.next() == XmlPullParser.TEXT) {
                if (entryFound) {
                    parsedSpreadsheet.title = parser.getText();
                    parser.nextTag();
                }
            } else if (name.equals("id") && parser.next() == XmlPullParser.TEXT) {
                if (entryFound) {
                    String id = parser.getText();
                    parsedSpreadsheet.id = id;
                    parsedSpreadsheet.key = id.substring(id.lastIndexOf("/") + 1);
                    parser.nextTag();
                }
            }
        }

        in.close();
        return parsedSpreadsheet;
    }
}
