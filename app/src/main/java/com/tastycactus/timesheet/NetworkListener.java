package com.tastycactus.timesheet;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

    public static final String LOG = "EVENT_LISTENER";

    public static void startup(Context context) {
        if (isConnected(context))
            connected(context);
    }

    private static String getNetworkName(Context context) {
        return getWifiInfo(context).getSSID().replace("\"", "");
    }

    private static boolean isConnected(Context context) {
        return getWifiInfo(context).getSupplicantState() == SupplicantState.COMPLETED;
    }

    private static WifiInfo getWifiInfo(Context context) {
        return ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
    }

    private static void connected(Context context) {
        TimesheetDatabase db = new TimesheetDatabase(context);

        String wifiName = getNetworkName(context);

        long taskId = db.getTaskIdForNetwork(wifiName);
        Log.d(LOG, "Connected to " + wifiName + ", TaskID " + taskId + " current task id=" + db.getCurrentTaskId());
        if (taskId >= 0) {
            setCurrentTask(context, taskId, wifiName);

        }
    }

    private static void setCurrentTask(Context context, long taskId, String wifiName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong("app_task", taskId);
        edit.apply();


        Intent intent = new Intent(context, TimesheetAppWidgetProvider.ToggleActiveService.class);
        intent.putExtra("TASK_ID", taskId);
        if (taskId >= 0) {
            intent.putExtra("NETWORK_NAME", wifiName);
            intent.putExtra("COMMENT", "Logged into Wifi " + wifiName);
        }

        context.startService(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(LOG, "received action " + action + " data=" + intent.getDataString() + " type=" + intent.getType());

        if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION))
            wifiEvent(context, intent);
        else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED))
            bluetoothEvent(context, intent);
        else
            Log.w(LOG, "Unkown Broadcast received: " + action);
    }

    private void bluetoothEvent(Context context, Intent intent) {
        int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED);
        if (newState == BluetoothAdapter.STATE_CONNECTED) {
            BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.w(LOG, "remoteDevice.getName() = " + remoteDevice.getName());
        }
    }

    private void wifiEvent(Context context, Intent intent) {
        SupplicantState supplicantState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
        if (supplicantState == SupplicantState.COMPLETED) {
            connected(context);
        } else if (supplicantState == SupplicantState.DISCONNECTED)
            disconnected(context);
    }

    private void disconnected(Context context) {
        Log.d(LOG, "Network connection lost.");

        setCurrentTask(context, -1, "");
    }
}
