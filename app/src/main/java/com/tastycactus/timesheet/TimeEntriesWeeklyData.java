package com.tastycactus.timesheet;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class TimeEntriesWeeklyData {
    private final String DAY_LABEL[] =
            new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    TimesheetDatabase m_db;
    int m_year, m_month, m_day, m_start_of_week;
    String[] m_headers;
    boolean m_weekly_billable_only;
    // In perl: $data[$day][$i] = { _id => $id, title => $title, duration => $duration }
    Vector<Vector<HashMap<String, String>>> m_data = new Vector<Vector<HashMap<String, String>>>();
    Vector<HashMap<String, String>> m_totals = new Vector<HashMap<String, String>>();

    public TimeEntriesWeeklyData(Context ctx, TimesheetDatabase db, int year, int month, int day) {
        m_db = db;
        m_year = year;
        m_month = month;
        m_day = day;
        for (int i = 0; i < 7; ++i) {
            m_data.add(i, new Vector<HashMap<String, String>>());
        }

        m_headers = new String[7];

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        m_start_of_week = new Integer(prefs.getString("week_start", "2"));
        m_weekly_billable_only = prefs.getBoolean("weekly_billable_only", true);

        adjustDate();
        requery();
    }

    public void setDate(int year, int month, int day) {
        m_year = year;
        m_month = month;
        m_day = day;
        adjustDate();
    }

    private void adjustDate() {
        Calendar c = Calendar.getInstance();
        c.set(m_year, m_month - 1, m_day);

        // Rewind the calendar to the start of the week
        while (c.get(Calendar.DAY_OF_WEEK) != m_start_of_week) {
            c.add(Calendar.DAY_OF_YEAR, -1);
        }

        m_year = c.get(Calendar.YEAR);
        m_month = c.get(Calendar.MONTH) + 1;
        m_day = c.get(Calendar.DAY_OF_MONTH);
    }

    public void requery() {
        for (int i = 0; i < 7; ++i) {
            m_data.get(i).clear();
        }
        m_totals.clear();

        Cursor c = m_db.getWeekEntries(m_year, m_month, m_day);

        HashMap<String, Float> total_map = new HashMap<String, Float>();
        while (!c.isAfterLast()) {
            HashMap<String, String> row_data = new HashMap<String, String>();
            int billable = c.getInt(c.getColumnIndex("billable"));
            if (billable == 1 || !m_weekly_billable_only) {
                int day = c.getInt(c.getColumnIndex("day"));
                row_data.put("_id", c.getString(c.getColumnIndex("_id")));
                String title = c.getString(c.getColumnIndex("title"));
                row_data.put("title", title);
                String comment = ": " + c.getString(c.getColumnIndex("comment"));
                row_data.put("comment", comment);
                float duration = c.getFloat(c.getColumnIndex("duration"));
                row_data.put("duration", String.format("%1.2f", duration));
                m_data.get(day).add(row_data);

                // Track the total durations
                if (total_map.containsKey(title)) {
                    total_map.put(title, total_map.get(title) + duration);
                } else {
                    total_map.put(title, duration);
                }
            }

            c.moveToNext();
        }
        for (Map.Entry<String, Float> entry : total_map.entrySet()) {
            HashMap<String, String> row = new HashMap<String, String>();
            row.put("title", entry.getKey());
            row.put("duration", String.format("%1.2f", entry.getValue()));
            m_totals.add(row);
        }
        c.close();
        headers();
    }

    public Vector<HashMap<String, String>> entries(int idx) {
        int day = (idx + m_start_of_week - 1) % 7;
        return m_data.get(day);
    }

    public Vector<HashMap<String, String>> totals() {
        return m_totals;
    }

    public String[] headers() {
        Calendar c = Calendar.getInstance();
        c.set(m_year, m_month - 1, m_day);

        for (int i = 0; i < 7; ++i) {
            m_headers[i] = String.format("%04d-%02d-%02d - %s",
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                    DAY_LABEL[c.get(Calendar.DAY_OF_WEEK) - 1]);
            c.add(Calendar.DAY_OF_YEAR, 1);
        }

        return m_headers;
    }
}
