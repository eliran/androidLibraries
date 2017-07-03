package com.threeplay.core;

import java.net.*;
import java.io.*;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.String;
import java.util.Scanner;

/**
 * Created by eliranbe on 7/8/15.
 */
public class HttpCommunicationClient {
    private static final String TAG = "HttpCommunicationClient";

    private URL url;

    public HttpCommunicationClient(String url) throws MalformedURLException {
      this.url = new URL(url);
    }

    public Promise<JSONObject> jsonRequest(final JSONObject json){
        final Promise.Defer<JSONObject> deferred = new Promise.Defer<>();

        new AsyncTask<URL, Void, Boolean>() {
            @Override protected Boolean doInBackground(URL... urls) {
                try {
                    HttpURLConnection conn = (HttpURLConnection) urls[0].openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    try {
                        if ( json != null ) {
                            OutputStream os = new DataOutputStream(conn.getOutputStream());
                            os.write(json.toString().getBytes("UTF-8"));
                            os.flush();
                            os.close();
                        }
                        deferred.resolveWithResult(streamToJSON(conn.getInputStream()));
                    } catch (Exception e) {
                        deferred.rejectWithException(e);
                    } finally {
                        conn.disconnect();
                    }
                } catch (Exception e) {
                    deferred.rejectWithException(e);
                }
                return true;
            }
        }.execute(this.url);
        return deferred.promise;
    }

    private String streamToString(InputStream is) {
        Scanner stringScanner = new Scanner(new BufferedInputStream(is), "UTF-8").useDelimiter("\\A");
        return stringScanner.hasNext() ? stringScanner.next() : "";
    }

    private JSONObject streamToJSON(InputStream is) {
        try {
            return new JSONObject(streamToString(is));
        } catch (JSONException ok) {
        }
        return null;
    }
}

