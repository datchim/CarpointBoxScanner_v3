package com.carpoint.boxscanner.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.carpoint.boxscanner.R;

import org.json.JSONObject;

public class CheckVersion extends AsyncTask<Void, Void, Integer> {

    private final String ADDRESS = "https://www.carpointpartners.eu/android_apk_pallet_v.txt";

    private String serverResponse;
    private Context context;
    private int actualVersion;

    public CheckVersion(Context context) {
        try {
            this.context = context;

            PackageInfo pInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            actualVersion = pInfo.versionCode;

            this.execute();
        } catch (Exception e) {
            Log.e("CP", Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    protected Integer doInBackground(Void... param) {
        try {
            // Stahnuti txt verze
            InputStream input;
            serverResponse = "";
            try {
                URLConnection con = new URL(ADDRESS).openConnection();
                con.setConnectTimeout(10000);
                input = con.getInputStream();
            } catch (IOException e) {
                // Soubor neexistuje
                return 0;
            }

            if (input == null)
                return 0;

            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            String tmp;
            while ((tmp = in.readLine()) != null) {
                serverResponse += tmp;
            }
            in.close();

            // Zjisteni zdali nova verze
            int newVersion = 0;
            if (serverResponse.length() > 0) {
                newVersion = Integer.parseInt(serverResponse);
            }

            if (newVersion <= actualVersion)
                return 1;
            else
                return 2;

        } catch (Exception e) {
            // Fce.err_menu(e);
            return 0;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        try {
            if(result == 2){
                new AlertDialog.Builder(context)
                        .setMessage(R.string.msg_actualize)
                        .setPositiveButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {

                                        final String appPackageName = context
                                                .getPackageName();
                                        try {
                                            context.startActivity(new Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("market://details?id="
                                                            + appPackageName)));
                                        } catch (android.content.ActivityNotFoundException anfe) {
                                            context.startActivity(new Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://play.google.com/store/apps/details?id="
                                                            + appPackageName)));
                                        }
                                    }
                                }).create().show();

            }
        } catch (Exception e) {
            Log.e("CP", Arrays.toString(e.getStackTrace()));
        }
    }

}
