package com.vsalmen.vrdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class SearchResults extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        Intent intent = getIntent();
        String query = intent.getStringExtra("query");
        doSearch(query);
    }

    private void doSearch(String query) {
        String url = "https://rata.digitraffic.fi/api/v1/live-trains/station/" + query;
        Intent serviceIntent = new Intent(this, ApiService.class);
        serviceIntent.putExtra(ApiService.API_URL, url);
        serviceIntent.putExtra(ApiService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
               super.onReceiveResult(resultCode, resultData);

               if (resultCode == Activity.RESULT_OK) {
                   String data = resultData.getString(ApiService.API_RESPONSE);

                   TextView tw = (TextView) findViewById(R.id.textView);
                   tw.setText(data);
               } else {
                   // TODO: Do something on failed api call;
               }
           }
        });
        startService(serviceIntent);


    }
}
