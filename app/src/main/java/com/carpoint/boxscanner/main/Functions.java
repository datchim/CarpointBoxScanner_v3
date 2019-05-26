package com.carpoint.boxscanner.main;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.carpoint.boxscanner.R;

import java.util.Arrays;


public class Functions {


    public static boolean checkUrlAndLogin(Context context) {
        if (context != null) {

            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(context);
            if (prefs.getString("username_preference", "").length() == 0)
                return false;
            if (prefs.getString("passwort_preference", "").length() == 0)
                return false;
            if (prefs.getString("url_preference", "").length() == 0)
                return false;
        }
        return true;
    }

    public static boolean checkUpdateAndHWEnable(Context context) {

        // Check GPS
        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(context,
                    context.getString(R.string.msg_GPS_is_disable),
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return checkInternetEnable(context);
    }

    public static boolean checkInternetEnable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            Toast.makeText(context,
                    context.getString(R.string.msg_Internet_is_disable),
                    Toast.LENGTH_LONG).show();
            return false;
        } else
            return true;
    }
    public static void toast(final Context c, final int text) {
        try {
            if (c != null) {
                ((Activity) c).runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(c, text, Toast.LENGTH_LONG)
                                .show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("CP", Arrays.toString(e.getStackTrace()));
        }
    }

    public static void err(Exception e) {
        Log.e("CP", Arrays.toString(e.getStackTrace()));
    }


    public static void setupForegroundDispatch(final Activity activity,
                                               NfcAdapter adapter) {
        if (activity != null && adapter != null) {
            final Intent intent = new Intent(activity.getApplicationContext(),
                    activity.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            final PendingIntent pendingIntent = PendingIntent.getActivity(
                    activity.getApplicationContext(), 0, intent, 0);

            IntentFilter[] filters = new IntentFilter[2];
            String[][] techList = new String[][] {};

            filters[0] = new IntentFilter();
            filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
            filters[0].addCategory(Intent.CATEGORY_DEFAULT);
            try {
                filters[0].addDataType("text/plain");
            } catch (MalformedMimeTypeException e) {
                throw new RuntimeException("Check your mime type.");
            }

            filters[1] = new IntentFilter();
            filters[1].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
            filters[1].addCategory(Intent.CATEGORY_DEFAULT);

            adapter.enableForegroundDispatch(activity, pendingIntent, filters,
                    techList);
        }
    }

    public static void stopForegroundDispatch(final Activity activity,
                                              NfcAdapter adapter) {
        if (activity != null && adapter != null)
            adapter.disableForegroundDispatch(activity);
    }
}
