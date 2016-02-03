package com.tastycactus.timesheet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class NetworkListener extends BroadcastReceiver {

    public static void startup(Context context) {
        if (isConnected(context))
            connected(context);
    }

    private static String getNetworkName(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getSSID().replace("\"", "");
    }

    private static boolean isConnected(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getSupplicantState() == SupplicantState.COMPLETED;
    }

    private static void connected(Context context) {
        TimesheetDatabase db = new TimesheetDatabase(context);

        String wifiName = getNetworkName(context);

        long taskId = db.getTaskIdForNetwork(wifiName);
        Log.d("WIFI", "Connected to " + wifiName + ", TaskID " + taskId + " current task id=" + db.getCurrentTaskId());
        if (taskId >= 0) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putLong("app_task", taskId);
            edit.apply();

            Intent intent = new Intent(context, TimesheetAppWidgetProvider.ToggleActiveService.class);
            intent.putExtra("TASK_ID", taskId);
            intent.putExtra("NETWORK_NAME", wifiName);
            intent.putExtra("COMMENT", "Logged into Wifi " + wifiName);

            context.startService(intent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("WIFI", "received action " + action + " data=" + intent.getDataString() + " type=" + intent.getType());

        wifiEvent(context, intent);
    }

    private void wifiEvent(Context context, Intent intent) {
        SupplicantState supplicantState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
        if (supplicantState == SupplicantState.COMPLETED) {
            connected(context);
        } else if (supplicantState == SupplicantState.DISCONNECTED)
            disconnected(context);
    }

    private void disconnected(Context context) {
        Log.d("WIFI", "Network connection lost.");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong("app_task", -1);
        edit.apply();

        Intent intent = new Intent(context, TimesheetAppWidgetProvider.ToggleActiveService.class);
        intent.putExtra("TASK_ID", -1);

        context.startService(new Intent(context, TimesheetAppWidgetProvider.ToggleActiveService.class));
    }

}
