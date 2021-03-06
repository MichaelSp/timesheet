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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

public class TimesheetDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Timesheet";
    private static final int DATABASE_VERSION = 5;

    public TimesheetDatabase(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static String getSqlDate() {
        final Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String getSqlTime() {
        final Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d %02d:%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    @Override
    public void onCreate(SQLiteDatabase db) throws SQLException {
        String[] sqls = new String[] {
                "CREATE TABLE tasks (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, billable INTEGER, wifi TEXT, bluetooth TEXT, hidden INTEGER)",
            "CREATE TABLE time_entries (_id INTEGER PRIMARY KEY AUTOINCREMENT, task_id INTEGER, comment STRING, start_time TEXT NOT NULL, end_time TEXT)"
        };
        db.beginTransaction();
        try {
            for( String sql : sqls )
                db.execSQL(sql);
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e("DB", "Error creating Timesheet database tables" + e.toString());
            throw e;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int old_version, int new_version) {
        if (old_version == 1) {
            upgrade(db, new String[]{
                    "ALTER TABLE tasks ADD COLUMN hidden INTEGER",
                    "UPDATE tasks SET hidden = 0"
            });
        }
        if (old_version == 2) {
            upgrade(db, new String[]{
                    "ALTER TABLE time_entries ADD COLUMN comment STRING",
                    "UPDATE time_entries SET comment = ''"
            });
        }
        if (old_version == 3) {
            upgrade(db, new String[]{"ALTER TABLE tasks ADD COLUMN wifi TEXT"});
        }
        if (old_version == 4) {
            upgrade(db, new String[]{"ALTER TABLE tasks ADD COLUMN bluetooth TEXT"});
        }
    }

    private void upgrade(SQLiteDatabase db, String[] sqls) {
        db.beginTransaction();
        try {
            for (String sql : sqls)
                db.execSQL(sql);
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e("DB", "Error upgrading Timesheet database tables" + e.toString());
            throw e;
        } finally {
            db.endTransaction();
        }
    }

    public Cursor getTasks(boolean alphabetiseTasks) {
        SQLiteDatabase db = getReadableDatabase();

        // alphabetise_tasks
        String sortString = "billable DESC, _id ASC";
        if (alphabetiseTasks == true) {
            sortString = "billable DESC, title ASC";
        }

        Cursor c = db.query("tasks", new String[]{"_id", "title", "billable", "wifi", "bluetooth"}, "hidden != 1", null, null, null, sortString);
        c.moveToFirst();
        return c;
    }

    public long getFirstTaskId(boolean alphabetise_tasks) {
        Cursor c = getTasks(alphabetise_tasks);
        if (c.getCount() > 0) {
            return c.getLong(0);
        } else {
            return -1;
        }
    }

    public Cursor getTask(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("tasks", new String[]{"_id", "title", "billable", "wifi", "bluetooth"}, "_id = ?", new String[]{Long.toString(id)}, null, null, null);
        c.moveToFirst();
        return c;
    }

    public String getTaskName(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("tasks", new String[]{"title"}, "_id = ?", new String[]{Long.toString(id)}, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            String r = c.getString(0);
            c.close();
            return r;
        } else {
            c.close();
            return "";
        }
    }

    boolean isValidTask(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("tasks", new String[] {"title"}, "_id = ?", new String[] {Long.toString(id)}, null, null, null);
        return c.getCount() > 0;
    }

    public void newTask(String title, boolean billable, String wifi, String bluetooth) {
        ContentValues cv = new ContentValues();

        // Check if this task already exists, but is hidden.
        Cursor c = getReadableDatabase().query("tasks", new String[]{"_id"}, "title = ?", new String[]{title}, null, null, null);
        if (c.getCount() > 0) {
            // Un-hide the row
            c.moveToFirst();
            cv.put("hidden", false);
            try {
                getWritableDatabase().update("tasks", cv, "_id = ?", new String[] {c.getString(c.getColumnIndex("_id"))});
            } catch (SQLException e) {
                Log.e("DB", "Error un-hiding task" + e.toString());
            }
        } else {
            cv.put("title", title);
            cv.put("billable", billable);
            cv.put("wifi", wifi);
            cv.put("bluetooth", bluetooth);
            cv.put("hidden", false);
            try {
                getWritableDatabase().insert("tasks", null, cv);
            } catch (SQLException e) {
                Log.e("DB", "Error adding new task" + e.toString());
            }
        }
        c.close();
    }

    public void updateTask(long id, String title, boolean billable, String wifi, String bluetooth) {
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("billable", billable);
        cv.put("wifi", wifi);
        cv.put("bluetooth", bluetooth);
        try {
            getWritableDatabase().update("tasks", cv, "_id = ?", new String[] {Long.toString(id)});
        } catch (SQLException e) {
            Log.e("DB", "Error updating task" + e.toString());
        }
    }

    public void deleteTask(long task_id) {
        // Check if there are time entries for this task.  If so, just hide it instead of deleting
        Cursor c = getReadableDatabase().query("time_entries", new String[]{"_id"}, "task_id = ?", new String[]{Long.toString(task_id)}, null, null, null);
        if (c.getCount() > 0) {
            // Don't actually delete the task, just mark it as hidden
            ContentValues cv = new ContentValues();
            cv.put("hidden", true);
            try {
                getWritableDatabase().update("tasks", cv, "_id = ?", new String[] {Long.toString(task_id)});
            } catch (SQLException e) {
                Log.e("DB", "Error hiding task" + e.toString());
            }
        } else {
            try {
                getWritableDatabase().delete("tasks", "_id = ?", new String[] {Long.toString(task_id)});
            } catch (SQLException e) {
                Log.e("DB", "Error deleting task" + e.toString());
            }
        }
        c.close();
    }

    public Cursor getTimeEntry(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT _id, task_id, comment, date(start_time) AS start_date, strftime('%H:%M', start_time)"
                        + " AS start_time, date(ifnull(end_time, datetime('now', 'localtime'))) AS end_date,"
                        + " strftime('%H:%M', ifnull(end_time, datetime('now', 'localtime'))) AS end_time,"
                        + " round((strftime('%s', ifnull(end_time, datetime('now', 'localtime'))) - strftime('%s', start_time)) / 3600.0, 2) AS duration"
                        + " FROM time_entries WHERE _id = ? ORDER BY start_time ASC",
                new String[]{Long.toString(id)}
        );
        c.moveToFirst();
        return c;
    }

    private Cursor doTimeEntriesSql(String start_date, long task_id) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "";
        String[] selectionArgs = new String[]{start_date};
        if (task_id >= 0) {
            query = " AND tasks._id = ?";
            selectionArgs = new String[]{start_date, Long.toString(task_id)};
        }
        Cursor c = db.rawQuery(
                "SELECT time_entries._id, title, comment, strftime('%H:%M', start_time) AS start_time,"
                        + " strftime('%H:%M', ifnull(end_time, datetime('now', 'localtime'))) AS end_time,"
                        + " round((strftime('%s', ifnull(end_time, datetime('now', 'localtime'))) - strftime('%s', start_time)) / 3600.0, 2) AS duration"
                        + " FROM time_entries, tasks"
                        + " WHERE tasks._id = time_entries.task_id AND date(start_time) = ? " + query + "ORDER BY start_time ASC",
                selectionArgs
        );
        c.moveToFirst();
        return c;
    }

    private Cursor doTimeEntriesSql(String start_date) {
        return doTimeEntriesSql(start_date, -1);
    }

    public Cursor getTimeEntries() {
        return doTimeEntriesSql(getSqlDate());
    }

    public Cursor getTimeEntries(int year, int month, int day) {
        return doTimeEntriesSql(String.format("%04d-%02d-%02d", year, month, day));
    }

    public void newTimeEntry(long task_id, String comment, String start_time, String end_time) {
        String[] overlappingTimeEntry = findOverlappingTimeEntry(task_id, start_time);
        if (overlappingTimeEntry.length > 0) {
            updateTimeEntry(Long.parseLong(overlappingTimeEntry[0]), task_id, comment, overlappingTimeEntry[1], end_time);
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("task_id", task_id);
        cv.put("comment", comment);
        cv.put("start_time", start_time);
        cv.put("end_time", end_time);
        try {
            Log.d("DB", "New TaskTimeEntry " + cv);
            getWritableDatabase().insert("time_entries", null, cv);
        } catch (SQLException e) {
            Log.e("DB", "Error adding new time entry" + e.toString());
        }
    }

    private String[] findOverlappingTimeEntry(long newTaskId, String newStartTime) {
        String newTaskName = getTaskName(newTaskId);
        Cursor entries = doTimeEntriesSql(getSqlDate(), newTaskId);
        if (entries.moveToLast()) {
            String startTime = entries.getString(3);
            String endTime = entries.getString(4);
            String taskName = entries.getString(1);
            newStartTime = newStartTime.split(" ")[1];

            Log.d("DB", "find overlapping time entry: " + newTaskId + " @ " + newStartTime + " (" + newTaskName.equals(taskName) + ", " + newStartTime.equals(endTime) + ") ENDTIME: " + endTime);
            if (newTaskName.equals(taskName) && newStartTime.equals(endTime)) {
                return new String[]{entries.getString(0), startTime};
            }
        }
        return new String[]{};
    }

    public void updateTimeEntry(long id, long task_id, String comment, String start_time, String end_time) {
        ContentValues cv = new ContentValues();
        cv.put("task_id", task_id);
        cv.put("comment", comment);
        cv.put("start_time", start_time);
        cv.put("end_time", end_time);
        try {
            Log.d("DB", "Update TaskTimeEntry " + cv);
            getWritableDatabase().update("time_entries", cv, "_id = ?", new String[]{Long.toString(id)});
        } catch (SQLException e) {
            Log.e("DB", "Error updating time entry" + e.toString());
        }
    }

    public void updateTimeEntry(long id, long task_id, String comment, String start_time) {
        ContentValues cv = new ContentValues();
        cv.put("task_id", task_id);
        cv.put("comment", comment);
        cv.put("start_time", start_time);
        try {
            getWritableDatabase().update("time_entries", cv, "_id = ?", new String[] {Long.toString(id)});
        } catch (SQLException e) {
            Log.e("DB", "Error updating time entry" + e.toString());
        }
    }

    private Cursor doWeekSql(String start_date) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT time_entries._id AS _id, title, billable, comment, strftime('%w', start_time) AS day,"
                + " date(start_time) AS start_date,"
                + " sum((strftime('%s', ifnull(end_time, datetime('now', 'localtime'))) - strftime('%s', start_time)) / 3600.0) AS duration"
                + " FROM time_entries, tasks"
                + " WHERE tasks._id = time_entries.task_id"
                + " AND date(start_time) >= ?"
                + " AND date(start_time) < date(?,'+7 days')"
                + " GROUP BY title, day ORDER BY day, title ASC",
                new String[] {start_date, start_date}
        );
        c.moveToFirst();
        return c;
    }

    public Cursor getWeekEntries(int year, int month, int day) {
        return doWeekSql(String.format("%04d-%02d-%02d", year, month, day));
    }


    public Cursor getYearEntries(int year) {
        String start_date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, 1, 1);
        String end_date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, 12, 31);
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT time_entries._id AS _id, strftime('%W', start_time) AS week,"
                        + " sum((strftime('%s', ifnull(end_time, datetime('now', 'localtime'))) - strftime('%s', start_time)) / 3600.0) AS total"
                        + " FROM time_entries, tasks"
                        + " WHERE tasks._id = time_entries.task_id"
                        + " AND date(start_time) >= ?"
                        + " AND date(start_time) < ?"
                        + " GROUP BY week ORDER BY week ASC",
                new String[]{start_date, end_date}
        );
        c.moveToFirst();
        return c;
    }

    public void deleteTimeEntry(long time_entry_id) {
        try {
            getWritableDatabase().delete("time_entries", "_id=?", new String[] {Long.toString(time_entry_id)});
        } catch (SQLException e) {
            Log.e("DB", "Error deleting time entry" + e.toString());
        }
    }

    public void completeTask(long id) {
        String time = getSqlTime();
        ContentValues cv = new ContentValues();
        cv.put("end_time", time);
        try {
            getWritableDatabase().update("time_entries", cv, "_id=?", new String[]{Long.toString(id)});
        } catch (SQLException e) {
            Log.e("DB", "Error updating time entry" + e.toString());
        }
    }

    public void completeCurrentTask() {
        long current_id = getCurrentId();
        Log.d("DB", "Complete current task with entry id " + current_id);
        if (current_id == -1) {
            return;
        }
        completeTask(current_id);
    }

    public void changeTask(long id, String comment) {
        completeCurrentTask();
        newTimeEntry(id, comment, getSqlTime(), null);
    }

    public long getCurrentId() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(true, "time_entries", new String[]{"_id"}, "end_time IS NULL", null, null, null, null, null);
        if (c.getCount() == 0) {
            return -1;
        }
        c.moveToFirst();
        long id = c.getLong(0);
        c.close();
        return id;
    }

    public long getCurrentTaskId() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(true, "time_entries", new String[]{"task_id"}, "end_time IS NULL", null, null, null, null, null);
        if (c.getCount() == 0) {
            return 0;
        }
        c.moveToFirst();
        return c.getLong(0);
    }

    public String getCurrentTaskName() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT title"
                        + " FROM time_entries, tasks"
                        + " WHERE tasks._id = time_entries.task_id"
                        + " AND time_entries.end_time IS NULL",
                new String[]{}
        );
        if (c.getCount() == 0) {
            return "";
        }
        c.moveToFirst();
        return c.getString(0);
    }

    public Cursor getTimeEntries(String start_date, String end_date) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT title, billable, comment, start_time, end_time,"
                + " (strftime('%s', ifnull(end_time, datetime('now', 'localtime'))) - strftime('%s', start_time)) / 3600.0 AS duration"
                + " FROM time_entries, tasks"
                + " WHERE tasks._id = time_entries.task_id"
                + " AND date(start_time) >= ?"
                + " AND date(start_time) <= ?"
                + " ORDER BY start_time ASC",
                new String[] {start_date, end_date}
        );
        c.moveToFirst();
        return c;
    }

    public long getTaskIdForNetwork(String wifiName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("tasks", new String[]{"_id"}, "wifi = ?", new String[]{wifiName}, null, null, null);
        if (c.getCount() == 0) {
            return 0;
        }
        c.moveToFirst();
        long id = c.getLong(0);
        c.close();
        return id;
    }

    public long getTaskIdForBluetoothDevice(String deviceName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("tasks", new String[]{"_id"}, "bletooth = ?", new String[]{deviceName}, null, null, null);
        if (c.getCount() == 0) {
            return 0;
        }
        c.moveToFirst();
        long id = c.getLong(0);
        c.close();
        return id;
    }
}
