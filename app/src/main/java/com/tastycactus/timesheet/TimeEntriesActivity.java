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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.tastycactus.timesheet.entries.EntriesBase;
import com.tastycactus.timesheet.entries.EntriesDay;
import com.tastycactus.timesheet.entries.EntriesWeek;
import com.tastycactus.timesheet.entries.EntriesYear;

public class TimeEntriesActivity extends FragmentActivity
{
    public static final int ACTIVITY_EDIT = 1;
    public static final int ADD_TIME_ENTRY_MENU_ITEM    = Menu.FIRST;
    public static final int DELETE_TIME_ENTRY_MENU_ITEM = Menu.FIRST + 1;
    public static final int EDIT_TIME_ENTRY_MENU_ITEM   = Menu.FIRST + 2;
    public static final int EXPORT_MENU_ITEM            = Menu.FIRST + 3;
    public static final int EMAIL_MENU_ITEM             = Menu.FIRST + 4;
    protected static final int ACTIVITY_CREATE = 0;
    private MyPagerAdapter fragmentAdapter = new MyPagerAdapter();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entries);

        ViewPager vpPager = (ViewPager) findViewById(R.id.pager);
        vpPager.setAdapter(fragmentAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, ADD_TIME_ENTRY_MENU_ITEM, Menu.NONE, "Add Time Entry").setIcon(android.R.drawable.ic_menu_add);
        menu.add(Menu.NONE, EXPORT_MENU_ITEM, Menu.NONE, "Export to CSV").setIcon(android.R.drawable.ic_menu_save);
        menu.add(Menu.NONE, EMAIL_MENU_ITEM, Menu.NONE, "Send Email").setIcon(android.R.drawable.ic_menu_send);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent i;
        switch (item.getItemId()) {
            case ADD_TIME_ENTRY_MENU_ITEM:
                i = new Intent(this, TimeEntryEditActivity.class);
                startActivityForResult(i, ACTIVITY_CREATE);
                return true;
            case EXPORT_MENU_ITEM:
                i = new Intent(this, ExportActivity.class);
                i.putExtra("type", "csv");
                startActivityForResult(i, ACTIVITY_CREATE);
                return true;
            case EMAIL_MENU_ITEM:
                i = new Intent(this, ExportActivity.class);
                i.putExtra("type", "email");
                startActivityForResult(i, ACTIVITY_CREATE);
                return true;
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, DELETE_TIME_ENTRY_MENU_ITEM, Menu.NONE, "Delete Time Entry");
        menu.add(Menu.NONE, EDIT_TIME_ENTRY_MENU_ITEM, Menu.NONE, "Edit Time Entry");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case DELETE_TIME_ENTRY_MENU_ITEM:
                for (EntriesBase fragment : fragmentAdapter.getFragments())
                    fragment.deleteTimeEntry(info.id);
                // Update the App Widget in case we deleted the currently-active
                // time entry
                startService(new Intent(this, TimesheetAppWidgetProvider.UpdateService.class));
                return true;
            case EDIT_TIME_ENTRY_MENU_ITEM:
                Intent i = new Intent(this, TimeEntryEditActivity.class);
                i.putExtra("_id", info.id);
                startActivityForResult(i, ACTIVITY_EDIT);
                return true;
        }
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode) {
            case ACTIVITY_CREATE:
                if (resultCode == RESULT_OK) {
                    for (EntriesBase fragment : fragmentAdapter.getFragments())
                        fragment.activityCreate();
                }
                break;
            case ACTIVITY_EDIT:
                if (resultCode == RESULT_OK) {
                    for (EntriesBase fragment : fragmentAdapter.getFragments())
                        fragment.activityEdit();
                }
                break;
        }
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {
        EntriesBase[] fragments = new EntriesBase[3];

        public MyPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            EntriesBase fragment;
            switch (position) {
                case 0:
                    fragment = new EntriesDay();
                    break;
                case 1:
                    fragment = new EntriesWeek();
                    break;
                case 2:
                    fragment = new EntriesYear();
                    break;
                default:
                    return null;
            }
            fragments[position] = fragment;
            return fragment;
            }

        public EntriesBase[] getFragments() {
            return fragments;
        }

        @Override
        public int getCount() {
            return fragments.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Day";
                case 1:
                    return "Week";
                case 2:
                    return "Year";
                default:
                    return null;
            }
        }
    }
}
