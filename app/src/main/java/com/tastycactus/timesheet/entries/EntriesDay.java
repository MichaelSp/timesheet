package com.tastycactus.timesheet.entries;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.tastycactus.timesheet.R;
import com.tastycactus.timesheet.TimeEntriesActivity;
import com.tastycactus.timesheet.TimeEntryEditActivity;

public class EntriesDay extends EntriesBase {
    private Cursor m_day_cursor;
    private SimpleCursorAdapter m_day_ca;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entries_day, container, false);
        setup(view);
        return view;
    }

    @Override
    protected void setup(View view) {
        super.setup(view);
        m_day_cursor = m_db.getTimeEntries();

        ListView time_entry_list = (ListView) view.findViewById(R.id.entries_byday);
        m_day_ca = new SimpleCursorAdapter(view.getContext(),
                R.layout.time_entry,
                m_day_cursor,
                new String[]{"title", "comment", "start_time", "end_time", "duration"},
                new int[]{R.id.time_entry_title, R.id.time_entry_comment, R.id.time_entry_start, R.id.time_entry_end, R.id.time_entry_duration});
        time_entry_list.setAdapter(m_day_ca);
        time_entry_list.setChoiceMode(ListView.CHOICE_MODE_NONE);

        time_entry_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(parent.getContext(), TimeEntryEditActivity.class);
                i.putExtra("_id", id);
                startActivityForResult(i, TimeEntriesActivity.ACTIVITY_EDIT);
            }
        });

        registerForContextMenu(time_entry_list);
    }

    @Override
    public void onResume() {
        super.onResume();
        m_day_cursor.requery();
        m_day_ca.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        m_day_cursor.close();
        super.onDestroy();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        super.onDateSet(view, year, month, day);
        m_day_cursor.close();
        m_day_cursor = m_db.getTimeEntries(year, month + 1, day);
        m_day_ca.changeCursor(m_day_cursor);
    }

    @Override
    public void activityCreate() {
        m_day_cursor.requery();
    }

    @Override
    public void activityEdit() {
        m_day_cursor.requery();
    }

    @Override
    public void deleteTimeEntry(long id) {
        m_db.deleteTimeEntry(id);
        m_day_cursor.requery();
    }
}
