/*
 * Copyright (c) 2009-2010 Tasty Cactus Software, LLC
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Aaron Brice <aaron@tastycactus.com>
 *
 */

package com.tastycactus.timesheet;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.List;

public class TaskEditActivity extends Activity {
    TimesheetDatabase m_db;
    long m_row_id;
    ListView wifi_list;
    int selected_item;
    private ArrayAdapter<String> wifiNetworksArrayAdapter;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_db = new TimesheetDatabase(this);

        Bundle b = getIntent().getExtras();
        String title;
        boolean billable;
        String wifi;

        if (b != null) {
            m_row_id = b.getLong("_id");
            Cursor entry = m_db.getTask(m_row_id);
            int billable_int = entry.getInt(entry.getColumnIndex("billable"));
            billable = (billable_int != 0);
            title = entry.getString(entry.getColumnIndex("title"));
            wifi = entry.getString(entry.getColumnIndex("wifi"));
            entry.close();
        } else {
            m_row_id = -1;
            billable = false;
            title = "";
            wifi = "";
        }

        setContentView(R.layout.task_edit);

        final EditText title_edit = (EditText) findViewById(R.id.task_title);
        final CheckBox billable_edit = (CheckBox) findViewById(R.id.task_billable);
        wifi_list = (ListView) findViewById(R.id.wifiList);

        populateWiFiList();

        title_edit.setText(title);
        billable_edit.setChecked(billable);
        selected_item = wifiNetworksArrayAdapter.getPosition(wifi);
        Log.e("WIFI List", "Item " + wifi + " at pos " + selected_item);
        wifi_list.smoothScrollToPosition(selected_item);
        wifi_list.setItemChecked(selected_item, true);

        Button addButton = (Button) findViewById(R.id.add_button);
        if (m_row_id != -1)
            addButton.setText("Update Task");
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String title = title_edit.getText().toString();
                String wifi = wifiNetworksArrayAdapter.getItem(selected_item);

                if (title.length() > 0) {
                    if (m_row_id == -1) {
                        m_db.newTask(title, billable_edit.isChecked(), wifi);
                    } else {
                        m_db.updateTask(m_row_id, title, billable_edit.isChecked(), wifi);
                    }
                    setResult(RESULT_OK);
                } else {
                    setResult(RESULT_CANCELED);
                }
                finish();
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void populateWiFiList() {

        wifiNetworksArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, android.R.id.text1);
        wifi_list.setAdapter(wifiNetworksArrayAdapter);

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        if (list == null || list.isEmpty()) {
            wifiNetworksArrayAdapter.add("No Network Found");
        } else
            for (WifiConfiguration config : list) {
                wifiNetworksArrayAdapter.add(config.SSID.replace("\"", ""));
            }

        wifi_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setSelected(true);
                selected_item = i;
            }
        });
    }

    @Override
    protected void onDestroy() {
        m_db.close();
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "TaskEdit Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.tastycactus.timesheet/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "TaskEdit Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.tastycactus.timesheet/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
