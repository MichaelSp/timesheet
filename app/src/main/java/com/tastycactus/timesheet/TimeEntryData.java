package com.tastycactus.timesheet;

import android.database.Cursor;

import java.util.Calendar;

class TimeEntryData {
    String m_start_date, m_end_date;
    String m_start_time, m_end_time;
    String m_comment;
    long m_task_id, m_row_id;

    public TimeEntryData() {
        final Calendar c = Calendar.getInstance();
        m_start_date = formatDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        m_start_time = formatTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        m_end_date = m_start_date;
        m_end_time = m_start_time;
        m_comment = "";
        m_task_id = -1;
        m_row_id = -1;
    }

    public TimeEntryData(Cursor c, long row_id) {
        m_start_date = c.getString(c.getColumnIndex("start_date"));
        m_start_time = c.getString(c.getColumnIndex("start_time"));
        m_end_date = c.getString(c.getColumnIndex("end_date"));
        m_end_time = c.getString(c.getColumnIndex("end_time"));
        m_task_id = c.getLong(c.getColumnIndex("task_id"));
        m_comment = c.getString(c.getColumnIndex("comment"));
        m_row_id = row_id;
    }

    public long task_id() {
        return m_task_id;
    }

    public void set_start_date(int year, int month, int day) {
        m_start_date = formatDate(year, month, day);
    }

    public void set_start_time(int hour, int minute) {
        m_start_time = formatTime(hour, minute);
    }

    public void set_end_date(int year, int month, int day) {
        m_end_date = formatDate(year, month, day);
    }

    public void set_end_time(int hour, int minute) {
        m_end_time = formatTime(hour, minute);
    }

    public String start_date() {
        return m_start_date;
    }

    public String start_time() {
        return m_start_time;
    }

    public String end_date() {
        return m_end_date;
    }

    public String end_time() {
        return m_end_time;
    }

    public String comment() {
        return m_comment;
    }

    public long row() {
        return m_row_id;
    }

    private String formatDate(int year, int month, int day) {
        return String.format("%04d-%02d-%02d", year, month + 1, day);
    }

    private String formatTime(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }
}
