package com.tastycactus.timesheet.layout;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.tastycactus.timesheet.MergeAdapter;
import com.tastycactus.timesheet.R;
import com.tastycactus.timesheet.TimeEntriesWeeklyData;

import java.util.Calendar;

public class EntriesWeek extends EntriesBase implements DatePickerDialog.OnDateSetListener {

    private TimeEntriesWeeklyData m_week_data;
    private SimpleAdapter[] m_week_adapters = new SimpleAdapter[7];
    private MergeAdapter m_merge_adapter;
    private SimpleAdapter m_totals_adapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_entries_week, container, false);
        setup(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        m_week_data.requery();
        m_merge_adapter.notifyDataSetChanged();
        m_totals_adapter.notifyDataSetChanged();
    }

    @Override
    protected void setup(View view) {
        super.setup(view);
        final Calendar c = Calendar.getInstance();
        m_week_data = new TimeEntriesWeeklyData(getContext(), m_db,
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));

        ListView week_list = (ListView) view.findViewById(R.id.entries_byweek);

        for (int i = 0; i < 7; ++i) {
            m_week_adapters[i] = new SimpleAdapter(getContext(),
                    m_week_data.entries(i),
                    R.layout.week_entry,
                    new String[]{"title", "duration"},
                    new int[]{R.id.week_entry_title, R.id.week_entry_duration});
        }

        m_merge_adapter = new MergeAdapter(getContext(), R.layout.header, R.id.header, m_week_adapters, m_week_data.headers());
        week_list.setAdapter(m_merge_adapter);
        week_list.setChoiceMode(ListView.CHOICE_MODE_NONE);
        week_list.setItemsCanFocus(false);

        ListView total_view = (ListView) view.findViewById(R.id.entries_week_totals);
        m_totals_adapter = new SimpleAdapter(view.getContext(),
                m_week_data.totals(),
                R.layout.week_entry,
                new String[]{"title", "duration"},
                new int[]{R.id.week_entry_title, R.id.week_entry_duration});
        total_view.setAdapter(m_totals_adapter);
        total_view.setChoiceMode(ListView.CHOICE_MODE_NONE);
        total_view.setItemsCanFocus(false);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        super.onDateSet(view, year, monthOfYear, dayOfMonth);
        m_week_data.setDate(year, monthOfYear + 1, dayOfMonth);
        m_week_data.requery();
        m_merge_adapter.notifyDataSetChanged();
        m_totals_adapter.notifyDataSetChanged();
    }

    @Override
    public void activityCreate() {
        m_week_data.requery();
        m_merge_adapter.notifyDataSetChanged();
        m_totals_adapter.notifyDataSetChanged();
    }

    @Override
    public void activityEdit() {
        m_week_data.requery();
        m_merge_adapter.notifyDataSetChanged();
        m_totals_adapter.notifyDataSetChanged();
    }

    @Override
    public void deleteTimeEntry(long id) {
        m_db.deleteTimeEntry(id);
        m_week_data.requery();
        m_merge_adapter.notifyDataSetChanged();
        m_totals_adapter.notifyDataSetChanged();
    }
}
