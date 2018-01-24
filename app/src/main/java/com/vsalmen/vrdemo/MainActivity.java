package com.vsalmen.vrdemo;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;

public class MainActivity extends AppCompatActivity {

    SearchView stationSv; // SearchView used for searching station
    Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        setContentView(R.layout.activity_main);

        stationSv  = (SearchView) findViewById(R.id.stationSv);
        stationSv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                String query = stationSv.getQuery().toString();
                Intent searchIntent = new Intent(ctx, SearchResults.class);
                searchIntent.putExtra("query", query);
                startActivity(searchIntent);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {

                return false;
            }
        });

    }
}
