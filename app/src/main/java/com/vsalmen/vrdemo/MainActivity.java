package com.vsalmen.vrdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    SearchView stationSv; // SearchView used for searching station
    ListView suggestionsLv;
    Context ctx;
    List<JSONObject> stationsList = new ArrayList<>();
    SuggestionsArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        setContentView(R.layout.activity_main);
        suggestionsLv = findViewById(R.id.suggestionsLv);
        getStations();


        stationSv  = findViewById(R.id.stationSv);
        stationSv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                String convertedQuery = guessAndConvertQuery(s);
                launchSearchResultsActivity(convertedQuery);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (adapter != null)
                    adapter.updateSuggestion(s);
                return true;
            }
        });

    } // end of OnCreate

    private void launchSearchResultsActivity(String targetStation) {
        // TODO: Passing whole station JSONObject may be beneficial for additional features
        Intent searchIntent = new Intent(ctx, SearchResults.class);
        searchIntent.putExtra("query", targetStation);
        startActivity(searchIntent);
    }

    private String guessAndConvertQuery(String query) {
        //Compares query to all stations and picks the last hit. TODO: A lot of room for optimizations here
        String latestMatch = "TPE"; //Defaults to Tampere TODO: Default to nearest station (GPS)
        for (int i = 0; i < stationsList.size(); i++) {
            try {
                if(stationsList.get(i).getString("stationName").toLowerCase().contains(query.toLowerCase()))
                    latestMatch = stationsList.get(i).getString("stationShortCode");
            } catch (Exception e) {
                stationsList.remove(i);
            }
        }
        return latestMatch; // Returns the stationShortCode string which is used by the API
    }

    private void getStations() {
        String url = "https://rata.digitraffic.fi/api/v1/metadata/stations";
        Intent serviceIntent = new Intent(this, ApiService.class);
        serviceIntent.putExtra(ApiService.API_URL, url);
        serviceIntent.putExtra(ApiService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
           @Override
           protected void onReceiveResult(int resultCode, Bundle resultData) {
               super.onReceiveResult(resultCode, resultData);

               if (resultCode == Activity.RESULT_OK) {
                   try {
                       jsonArrayToStationsList(new JSONArray(resultData.getString(ApiService.API_RESPONSE)));
                   } catch (JSONException e) {
                       // Very unlikely exception, because all problems should handled in ApiService (resultCode check)
                       Log.println(Log.ERROR, "getStations", e.toString());
                   }

                    adapter = new SuggestionsArrayAdapter(ctx,
                           android.R.layout.simple_list_item_1, stationsList);
                   suggestionsLv.setAdapter(adapter);

                   suggestionsLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                       @Override
                       public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                           final JSONObject station = (JSONObject) adapterView.getItemAtPosition(i);
                           try {
                               launchSearchResultsActivity(station.getString("stationShortCode"));
                           } catch (JSONException e) {
                               // TODO: Better handling;
                               launchSearchResultsActivity("TPE");
                           }

                       }
                   });

               } else {
                   // TODO: Do something on failed api call;
               }
           }
        });
        startService(serviceIntent);
    }
    private void jsonArrayToStationsList(JSONArray jArray) {
        if (jArray != null) {
            for (int i = 0; i < jArray.length(); i++) {
                try {
                    if (jArray.getJSONObject(i).getBoolean("passengerTraffic") &&
                            jArray.getJSONObject(i).getString("type").equals("STATION")) {
                        stationsList.add(jArray.getJSONObject(i));
                    }
                } catch (JSONException e) {
                    Log.println(Log.INFO, "jsonArrayToStationList", "Faulty JSONObject found, continuing cloning");
                }

            }
            Log.println(Log.INFO, "jsonArrayToStationList", "Stations: " + stationsList.size());
        }
    }
    private class SuggestionsArrayAdapter extends ArrayAdapter<JSONObject> {
        private HashMap<JSONObject, Integer> mIdMap = new HashMap<>();
        private List<JSONObject> mRemovedStations = new ArrayList<>();

        public SuggestionsArrayAdapter(Context context, int listItemResId, List<JSONObject> stations) {
            super(context, listItemResId, stations);
            for (int i = 0; i < stations.size(); i++) {
                mIdMap.put(stations.get(i), i);
            }

        }
        public void updateSuggestion(String query) {
            // Remove non-matching Stations from suggestions
            for (int i = 0; i < getCount(); i++) {
                JSONObject item = getItem(i);
                try {

                    if (!item.getString("stationName").toLowerCase()
                            .contains(query.toLowerCase())) {

                        remove(item);
                        i--;
                        mRemovedStations.add(item);
                    }
                } catch (Exception e) {
                    Log.println(Log.ERROR, "UpdateSuggestion", e.toString());
                    remove(item);
                }
            }
            // Add matching stations back to suggestions
            if(query.equals("")) {
                addAll(mRemovedStations);
                mRemovedStations.clear();
            } else {
                for (int i = 0; i < mRemovedStations.size(); i++) {
                    try {
                        if (mRemovedStations.get(i).getString("stationName").toLowerCase()
                                .contains(query.toLowerCase())) {

                            add(mRemovedStations.get(i));
                            mRemovedStations.remove(i);
                        }
                    } catch (Exception e) {
                        Log.println(Log.ERROR, "UpdateSuggestion", e.toString());
                        mRemovedStations.remove(i);
                    }
                }
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
                itemView = inflater.inflate(android.R.layout.simple_list_item_1, null);
                TextView tw = itemView.findViewById(android.R.id.text1);
                try {
                    tw.setText(getItem(position).getString("stationName"));
                } catch (JSONException e) {
                    tw.setText(" ");
                }
            }
            else {
                itemView = convertView;
                TextView tw = (TextView) itemView.findViewById(android.R.id.text1);
                try {
                    tw.setText(getItem(position).getString("stationName"));
                } catch (JSONException e) {
                    tw.setText(" ");
                }
            }
            return itemView;
        }
    }
}

