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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.database.Cursor;
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
import android.widget.TabHost;

import java.util.List;
import java.util.Set;

public class TaskEditActivity extends Activity {
    TimesheetDatabase m_db;
    long m_row_id;
    int selectedWifi, selectedBluetooth = -1;
    ListView wifi_list;
    private ListView bluetooth_list;
    private ArrayAdapter<String> wifiNetworksArrayAdapter;
    private ArrayAdapter<String> bluetoothArrayAdapter;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private TabHost tabHost;

    private static int setItemChecked(String name, ArrayAdapter<String> stringArrayAdapter, ListView listView) {
        int selected_item = stringArrayAdapter.getPosition(name);
        Log.e("NETWORK_List", "Item " + name + " at pos " + selected_item);
        listView.smoothScrollToPosition(selected_item);
        listView.setItemChecked(selected_item, true);
        return selected_item;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_db = new TimesheetDatabase(this);

        Bundle b = getIntent().getExtras();
        String title;
        boolean billable;
        String wifi;
        String bluetoothName;

        if (b != null) {
            m_row_id = b.getLong("_id");
            Cursor entry = m_db.getTask(m_row_id);
            int billable_int = entry.getInt(entry.getColumnIndex("billable"));
            billable = (billable_int != 0);
            title = entry.getString(entry.getColumnIndex("title"));
            wifi = entry.getString(entry.getColumnIndex("wifi"));
            bluetoothName = entry.getString(entry.getColumnIndex("bluetooth"));
            entry.close();
        } else {
            m_row_id = -1;
            billable = false;
            title = "";
            wifi = "";
            bluetoothName = "";
        }

        setContentView(R.layout.task_edit);

        final EditText title_edit = (EditText) findViewById(R.id.task_title);
        final CheckBox billable_edit = (CheckBox) findViewById(R.id.task_billable);
        wifi_list = (ListView) findViewById(R.id.wifiList);
        bluetooth_list = (ListView) findViewById(R.id.bluetoothList);
        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();
        addTab("Wifi", R.id.tab1);
        addTab("Bluetooth", R.id.tab2);

        populateWiFiList();
        populateBluetoothList();


        title_edit.setText(title);
        billable_edit.setChecked(billable);

        selectedWifi = setItemChecked(wifi, wifiNetworksArrayAdapter, wifi_list);
        selectedBluetooth = setItemChecked(bluetoothName, bluetoothArrayAdapter, bluetooth_list);

        Button addButton = (Button) findViewById(R.id.add_button);
        if (m_row_id != -1)
            addButton.setText("Update Task");
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String title = title_edit.getText().toString();
                String wifi = null, bluetooth = null;
                if (selectedWifi > 0)
                    wifi = wifiNetworksArrayAdapter.getItem(selectedWifi);
                if (selectedBluetooth > 0)
                    bluetooth = bluetoothArrayAdapter.getItem(selectedBluetooth);
                Log.d("TaskEdit", String.format("Wifi selected(%d) = %s\tBluetooth selected(%d) = %s", selectedWifi, wifi, selectedBluetooth, bluetooth));

                if (title.length() > 0) {
                    if (m_row_id == -1) {
                        m_db.newTask(title, billable_edit.isChecked(), wifi, bluetooth);
                    } else {
                        m_db.updateTask(m_row_id, title, billable_edit.isChecked(), wifi, bluetooth);
                    }
                    setResult(RESULT_OK);
                } else {
                    setResult(RESULT_CANCELED);
                }
                finish();
            }
        });
    }

    private void addTab(String indicator, int resourceId) {
        TabHost.TabSpec spec = tabHost.newTabSpec(indicator);
        spec.setContent(resourceId);
        spec.setIndicator(indicator);
        tabHost.addTab(spec);
    }

    private void populateBluetoothList() {
        bluetoothArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, android.R.id.text1);
        bluetooth_list.setAdapter(bluetoothArrayAdapter);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        Set<BluetoothDevice> list = bluetoothManager.getAdapter().getBondedDevices();

        if (list == null || list.isEmpty())
            bluetoothArrayAdapter.add("Enable Bluetooth to see devices!");
        else {
            bluetoothArrayAdapter.add("<Not Selected>");
            for (BluetoothDevice device : list) {
                bluetoothArrayAdapter.add(device.getName());
            }
        }
        bluetooth_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setSelected(true);
                selectedBluetooth = i;
            }
        });
    }

    private void populateWiFiList() {
        wifiNetworksArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, android.R.id.text1);
        wifi_list.setAdapter(wifiNetworksArrayAdapter);

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        if (list == null || list.isEmpty()) {
            wifiNetworksArrayAdapter.add("No Network Found");
        } else {
            wifiNetworksArrayAdapter.add("<Not Selected>");
            for (WifiConfiguration config : list) {
                wifiNetworksArrayAdapter.add(config.SSID.replace("\"", ""));
            }
        }

        wifi_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setSelected(true);
                selectedWifi = i;
            }
        });
    }

    @Override
    protected void onDestroy() {
        m_db.close();
        super.onDestroy();
    }
}
