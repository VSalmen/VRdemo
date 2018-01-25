package com.vsalmen.vrdemo;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class ApiService extends IntentService {

    public final static String API_URL = "url";
    public final static String BUNDLED_LISTENER = "listener";
    public final static String API_RESPONSE = "apiResponse";

    public ApiService() {
        super("ApiService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        String url = intent.getStringExtra(ApiService.API_URL);
        ResultReceiver receiver = intent.getParcelableExtra(ApiService.BUNDLED_LISTENER);

        Bundle bundle = new Bundle();
        bundle.putString(ApiService.API_RESPONSE, getData(url));
        receiver.send(Activity.RESULT_OK, bundle);
    }

    public String getData(String url) {
        try {
            URL apiUrl = new URL(url);
            URLConnection URLConnection = apiUrl.openConnection();
            URLConnection.connect();
            InputStream apiResponse = URLConnection.getInputStream();

            return convertStreamToString(apiResponse);
        } catch (Exception e) {
            // TODO: Proper Exception handling
            Log.println(Log.ERROR, "getData", e.toString());
            return "{}";
        }
    }

    static private String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
