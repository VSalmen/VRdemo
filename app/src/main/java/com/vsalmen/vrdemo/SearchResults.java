package com.vsalmen.vrdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchResults extends AppCompatActivity {

    TabHost tabs;
    List<JSONObject> departuresList = new ArrayList<>();
    List<JSONObject> arrivalsList = new ArrayList<>();
    ResultsArrayAdapter departuresAdapter;
    ResultsArrayAdapter arrivalsAdapter;
    ListView departuresTabLv, arrivalsTabLv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        tabs = findViewById(R.id.tabHost);
        tabs.setup();

        TabHost.TabSpec departuresTab = tabs.newTabSpec("departures");
        departuresTab.setContent(R.id.departuresTabLv);
        departuresTab.setIndicator("Departures");
        tabs.addTab(departuresTab);

        TabHost.TabSpec arrivalsTab = tabs.newTabSpec("arrivals");
        arrivalsTab.setContent(R.id.arrivalsTabLv);
        arrivalsTab.setIndicator("Arrivals");
        tabs.addTab(arrivalsTab);

        arrivalsTabLv = findViewById(R.id.arrivalsTabLv);
        departuresTabLv = findViewById(R.id.departuresTabLv);

        Intent intent = getIntent();
        final String query = intent.getStringExtra("query");
        getSupportActionBar().setTitle(query);
        doSearch(query);
    }

    private void doSearch(final String query) {
        String url = "https://rata.digitraffic.fi/api/v1/live-trains/station/" + query;
        Intent serviceIntent = new Intent(this, ApiService.class);
        serviceIntent.putExtra(ApiService.API_URL, url);
        serviceIntent.putExtra(ApiService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
               super.onReceiveResult(resultCode, resultData);

               if (resultCode == Activity.RESULT_OK) {
                   String data = resultData.getString(ApiService.API_RESPONSE);
                   try {
                       parseResponse(new JSONArray(data), query);
                       Log.println(Log.INFO, "onReceiveResult",
                               "Departs: " + departuresList.size() + " Arrivals: " + arrivalsList.size());
                       setupAdapters();
                   } catch (Exception e) {
                       Log.println(Log.ERROR, "onRecieveResult", e.toString());
                   }

               } else {
                   // TODO: Do something on failed api call;
               }
           }
        });
        startService(serviceIntent);
    }
    private void parseResponse(JSONArray jsonArray, String stationShortCode) {
        for(int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject train = jsonArray.getJSONObject(i);
                if(!train.getBoolean("cancelled")) {

                    JSONArray timeTable = train.getJSONArray("timeTableRows");
                    JSONObject start = timeTable.getJSONObject(0);
                    JSONObject destination = timeTable.getJSONObject(timeTable.length() - 1);

                    for(int j = 0; j < timeTable.length(); j++) {
                        JSONObject instanceOfTime = timeTable.getJSONObject(j);
                        if(instanceOfTime.getString("stationShortCode").equals(stationShortCode)  // Search for a matching train
                                && instanceOfTime.getBoolean("trainStopping")
                                && instanceOfTime.getBoolean("commercialStop")) {
                            train.remove("timeTableRows");                                        // Should be safe to delete at this point in order to save some memory

                            if (instanceOfTime.getString("type").equals("DEPARTURE")) {           // Case for when the train starts at the target station
                                train.put("savedInstance", destination);
                                train.put("stationInstance", instanceOfTime);
                                train.remove("timeTableRows");
                                departuresList.add(train);

                            }
                            else if (instanceOfTime.getString("type").equals("ARRIVAL")) {        // Case for when the train arrives
                                    train.put("savedInstance", start);
                                    train.put("stationInstance", instanceOfTime);
                                    arrivalsList.add(train);

                                    if(++j < timeTable.length()) {                                      // Check if the train departs after arriving
                                        instanceOfTime = timeTable.getJSONObject(j);
                                        if(instanceOfTime.getString("stationShortCode").equals(stationShortCode)
                                                && instanceOfTime.getBoolean("trainStopping")
                                                && instanceOfTime.getBoolean("commercialStop")) {

                                            if (instanceOfTime.getString("type").equals("DEPARTURE")) {
                                                train.put("savedInstance", destination);
                                                train.put("stationInstance", instanceOfTime);
                                                departuresList.add(train);
                                            }
                                    }   }
                            }
                            break; //Start processing next train after target station has been found
                        }
                    }
                }
            } catch (Exception e) {
                // TODO
                Log.println(Log.ERROR, "MonsterParser", "Something went horribly wrong" + e.toString());
            }
        }
    }
    private void setupAdapters() {
        departuresAdapter = new ResultsArrayAdapter(this, R.layout.search_result_list_item, departuresList);
        arrivalsAdapter = new ResultsArrayAdapter(this, R.layout.search_result_list_item, arrivalsList);
        departuresTabLv.setAdapter(departuresAdapter);
        arrivalsTabLv.setAdapter(arrivalsAdapter);
    }

    private class ResultsArrayAdapter extends ArrayAdapter<JSONObject> {

        private HashMap<JSONObject, Integer> mIdMap = new HashMap<>();

        public ResultsArrayAdapter(Context context, int listItemResId, List<JSONObject> trains) {
            super(context, listItemResId, trains);
            for (int i = 0; i < trains.size(); i++) {
                mIdMap.put(trains.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            JSONObject item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                itemView = inflater.inflate(R.layout.search_result_list_item, null);
                TextView trainNum = itemView.findViewById(R.id.trainNumTv);
                TextView datetime = itemView.findViewById(R.id.scheduledTimeTv);
                TextView location = itemView.findViewById(R.id.locationTv);
                try {
                    trainNum.setText(getItem(position).getString("trainNumber"));
                    datetime.setText(getItem(position).getJSONObject("stationInstance").getString("scheduledTime"));
                    location.setText(getItem(position).getJSONObject("savedInstance").getString("stationShortCode"));
                } catch (JSONException e) {
                    Log.println(Log.ERROR, "getView", e.toString());
                    trainNum.setText("0");
                    datetime.setText("0000");
                    location.setText("Location");
                }
            } else {
                itemView = convertView;
                TextView trainNum = itemView.findViewById(R.id.trainNumTv);
                TextView datetime = itemView.findViewById(R.id.scheduledTimeTv);
                TextView location = itemView.findViewById(R.id.locationTv);
                try {
                    trainNum.setText(getItem(position).getString("trainNumber"));
                    datetime.setText(getItem(position).getJSONObject("stationInstance").getString("scheduledTime"));
                    location.setText(getItem(position).getJSONObject("savedInstance").getString("stationShortCode"));
                } catch (JSONException e) {
                    Log.println(Log.ERROR, "getView", e.toString());
                    trainNum.setText("0");
                    datetime.setText("0000");
                    location.setText("Location");
                }
            }
            return itemView;
        }
    }
}
