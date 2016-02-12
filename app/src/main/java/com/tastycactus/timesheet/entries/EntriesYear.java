package com.tastycactus.timesheet.entries;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.GridView;
import android.widget.SimpleCursorAdapter;

import com.tastycactus.timesheet.R;

import java.util.Calendar;

public class EntriesYear extends EntriesBase implements DatePickerDialog.OnDateSetListener {

    SimpleCursorAdapter mAdapter;
    private int mCurrentYear;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entries_year, container, false);
        setup(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void setup(View view) {
        super.setup(view);

        Button year_selection_button = (Button) view.findViewById(R.id.year_selection_button);
        year_selection_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mCurrentYear = Calendar.getInstance().get(Calendar.YEAR);
        Cursor cursor = m_db.getYearEntries(mCurrentYear);

        mAdapter = new SimpleCursorAdapter(view.getContext(),
                android.R.layout.simple_list_item_2,
                cursor,
                new String[]{"total", "week"},
                new int[]{android.R.id.text1, android.R.id.text2}, 0);

        GridView yearGrid = (GridView) view.findViewById(R.id.gridView);
        yearGrid.setAdapter(mAdapter);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        super.onDateSet(view, year, monthOfYear, dayOfMonth);
    }

    @Override
    public void activityCreate() {
    }

    @Override
    public void activityEdit() {
    }

    @Override
    public void deleteTimeEntry(long id) {
    }

}
