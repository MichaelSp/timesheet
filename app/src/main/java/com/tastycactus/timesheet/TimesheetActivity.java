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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class TimesheetActivity extends ListActivity {
    public static final int ADD_TASK_MENU_ITEM = Menu.FIRST;
    public static final int DELETE_TASK_MENU_ITEM = Menu.FIRST + 1;
    public static final int LIST_ENTRIES_MENU_ITEM = Menu.FIRST + 2;
    public static final int EDIT_TASK_MENU_ITEM = Menu.FIRST + 3;
    public static final int PREFERENCES_MENU_ITEM = Menu.FIRST + 4;
    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;
    TimesheetDatabase m_db;
    Cursor m_task_cursor;
    SimpleCursorAdapter m_ca;
    private String[] mFrom;
    private int[] mTo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        m_db = new TimesheetDatabase(this);
        m_task_cursor = m_db.getTasks(prefs.getBoolean("alphabetise_tasks", false));
        startManagingCursor(m_task_cursor);

        setContentView(R.layout.main);
        mFrom = new String[]{"title", "wifi", "bluetooth"};
        mTo = new int[]{android.R.id.text1, android.R.id.text2};
        m_ca = new MySimpleCursorAdapter();

        m_ca.registerDataSetObserver(new DataSetObserver() {
            public void onChanged() {
                updateCheckedItem();
            }
        });

        setListAdapter(m_ca);

        registerForContextMenu(getListView());

        setDefaultPreferences(prefs);

        updateCheckedItem();

        findViewById(R.id.add_task).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTask();
            }
        });
    }

    private void setDefaultPreferences(SharedPreferences prefs) {
        if (!prefs.contains("alphabetise_tasks")) {
            prefs.edit().putBoolean("alphabetise_tasks", false);
        }
        if (!prefs.contains("weekly_billable_only")) {
            prefs.edit().putBoolean("weekly_billable_only", true);
        }
        if (!prefs.contains("week_start")) {
            prefs.edit().putString("week_start", "2");
        }
        if (!prefs.contains("default_email")) {
            prefs.edit().putString("default_email", "");
        }
        prefs.edit().apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NetworkListener.startup(this);
    }

    @Override
    protected void onDestroy() {
        m_task_cursor.close();
        m_db.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, ADD_TASK_MENU_ITEM, Menu.NONE, "Add Task").setIcon(android.R.drawable.ic_menu_add);
        menu.add(Menu.NONE, LIST_ENTRIES_MENU_ITEM, Menu.NONE, "List Entries").setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, PREFERENCES_MENU_ITEM, Menu.NONE, "Preferences").setIcon(android.R.drawable.ic_menu_preferences);
        return result;
    }

    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
        RadioButton radio = (RadioButton) v.findViewById(R.id.radio);
        if (id == m_db.getCurrentTaskId()) {
            m_db.completeCurrentTask();
            getListView().clearChoices();
            getListView().requestLayout();
            v.setSelected(false);
            getListView().setItemChecked(position, false);
            radio.setChecked(false);
        } else {
            m_db.changeTask(id, "");
            v.setSelected(true);
            getListView().setItemChecked(position, true);
            radio.setChecked(true);
        }

        // Update the App Widget
        startService(new Intent(this, TimesheetAppWidgetProvider.UpdateService.class));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ADD_TASK_MENU_ITEM:
                addTask();
                return true;
            case LIST_ENTRIES_MENU_ITEM:
                listEntries();
                return true;
            case PREFERENCES_MENU_ITEM:
                preferences();
                return true;
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, EDIT_TASK_MENU_ITEM, Menu.NONE, "Edit Task");
        menu.add(Menu.NONE, DELETE_TASK_MENU_ITEM, Menu.NONE, "Delete Task");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case EDIT_TASK_MENU_ITEM:
                Intent i = new Intent(this, TaskEditActivity.class);
                i.putExtra("_id", info.id);
                startActivityForResult(i, ACTIVITY_EDIT);
                return true;
            case DELETE_TASK_MENU_ITEM:
                m_db.deleteTask(info.id);
                m_task_cursor.requery();
                // Update the App Widget, if necessary
                startService(new Intent(this, TimesheetAppWidgetProvider.UpdateService.class));
                return true;
        }
        return false;
    }

    private void addTask() {
        startActivityForResult(new Intent(this, TaskEditActivity.class), ACTIVITY_CREATE);
    }

    private void listEntries() {
        startActivity(new Intent(this, TimeEntriesActivity.class));
    }

    private void preferences() {
        startActivity(new Intent(this, TimesheetPreferences.class));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            m_task_cursor.requery();
            // Update the App Widget, if necessary
            startService(new Intent(this, TimesheetAppWidgetProvider.UpdateService.class));
        }
    }

    private void updateCheckedItem() {
        long current_id = m_db.getCurrentTaskId();
        Log.d("MAIN", String.format("UpdateCheckedItem: currentTaskId=%d", m_db.getCurrentTaskId()));
        if (current_id == 0) {
            getListView().clearChoices();
        } else {
            int count = getListView().getCount();
            for (int i = 0; i < count; ++i) {
                if (m_ca.getItemId(i) == current_id) {
                    Log.d("MAIN", String.format("UpdateCheckedItem: setItemChecked(%d)", i));
                }
            }
        }
    }

    class MySimpleCursorAdapter extends SimpleCursorAdapter {

        public MySimpleCursorAdapter() {
            super(TimesheetActivity.this, R.layout.simple_list_item_2_single_choice,
                    TimesheetActivity.this.m_task_cursor,
                    TimesheetActivity.this.mFrom,
                    TimesheetActivity.this.mTo, FLAG_REGISTER_CONTENT_OBSERVER);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ViewBinder binder = getViewBinder();
            final int count = mTo.length;
            final int[] from = new int[]{cursor.getColumnIndexOrThrow(mFrom[0]), cursor.getColumnIndexOrThrow(mFrom[1]), cursor.getColumnIndexOrThrow(mFrom[2])};
            final int[] to = mTo;

            RadioButton radio = (RadioButton) view.findViewById(R.id.radio);
            radio.setChecked(view.isSelected());

            for (int i = 0; i < count; i++) {
                final View v = view.findViewById(to[i]);
                if (v != null) {
                    boolean bound = false;
                    if (binder != null) {
                        bound = binder.setViewValue(v, cursor, from[i]);
                    }

                    if (!bound) {
                        String text = cursor.getString(from[i]);
                        if (text == null) {
                            text = "<Not Selected>";
                        }
                        if (i == 1) {
                            String bluetoothName = cursor.getString(from[2]);
                            if (bluetoothName == null)
                                bluetoothName = "<Not Selected>";
                            text += " / " + bluetoothName;
                        }

                        if (v instanceof TextView) {
                            setViewText((TextView) v, text);
                        } else if (v instanceof ImageView) {
                            setViewImage((ImageView) v, text);
                        } else {
                            throw new IllegalStateException(v.getClass().getName() + " is not a " +
                                    " view that can be bounds by this SimpleCursorAdapter");
                        }
                    }
                }
            }
        }
    }
}
