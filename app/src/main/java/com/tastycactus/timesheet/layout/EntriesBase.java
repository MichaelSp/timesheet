package com.tastycactus.timesheet.layout;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;

import com.tastycactus.timesheet.R;
import com.tastycactus.timesheet.TimeEntriesActivity;
import com.tastycactus.timesheet.TimesheetDatabase;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public abstract class EntriesBase extends Fragment implements DatePickerDialog.OnDateSetListener {

    protected TimesheetDatabase m_db;

    private Button m_date_select_button;
    private TimeEntriesActivity activity;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_db = new TimesheetDatabase(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDestroy() {
        m_db.close();
        super.onDestroy();
    }

    protected void setup(View view) {
        final Calendar c = Calendar.getInstance();

        m_date_select_button = (Button) view.findViewById(R.id.week_selection_button);
        if (m_date_select_button == null)
            m_date_select_button = (Button) view.findViewById(R.id.day_selection_button);
        m_date_select_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new DatePickerDialog(getContext(), EntriesBase.this,
                        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            }
        });

        m_date_select_button.setText(DateFormat.getDateFormat(getContext()).format(new Date()));
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        GregorianCalendar date = new GregorianCalendar(year, month, day);
        m_date_select_button.setText(DateFormat.getDateFormat(getContext()).format(date));
    }

    public void setActivity(TimeEntriesActivity activity) {
        this.activity = activity;
    }

    public abstract void activityCreate();

    public abstract void activityEdit();

    public abstract void deleteTimeEntry(long id);
}
