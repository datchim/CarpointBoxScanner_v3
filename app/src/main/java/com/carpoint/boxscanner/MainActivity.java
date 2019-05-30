package com.carpoint.boxscanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.carpoint.boxscanner.main.CPPsendRest;
import com.carpoint.boxscanner.main.CheckVersion;
import com.carpoint.boxscanner.main.ErrorListDialog;
import com.carpoint.boxscanner.main.FileMan;
import com.carpoint.boxscanner.main.FormFilling;
import com.carpoint.boxscanner.main.Functions;
import com.carpoint.boxscanner.main.HTTPcomm;
import com.carpoint.boxscanner.main.NFCWrite;
import com.carpoint.boxscanner.main.PreferencesActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class MainActivity extends ActionBarActivity {

    public static String language = "lang_de", lang_de = "lang_de";

    private final int REQUEST_PERMISSION_ID = 99;
    private final String LAST_UPDATE = "LASTUPDATE";
    public ErrorListDialog errorListDialog;
    private NfcAdapter mNfcAdapter;
    private boolean allowLangChange, ignoreSelection, performSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            ((Button) findViewById(R.id.start_scanning)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!Functions.checkUpdateAndHWEnable(MainActivity.this)) {
                        return;
                    }

                    FileMan fileMan = new FileMan(MainActivity.this, "");
                    if (fileMan.fileExists("questions.json")) {
                        Intent intent = new Intent(MainActivity.this, FormFilling.class);
                        startActivity(intent);
                    } else {
                        Functions.toast(MainActivity.this, R.string.noQuestion);
                        new HTTPcomm(MainActivity.this, new HTTPcomm.OnFinish() {
                            @Override
                            public void onResult(String response) {
                                if (response != null) {
                                    Intent intent = new Intent(MainActivity.this, FormFilling.class);
                                    intent.putExtra("json", response);
                                    startActivity(intent);
                                }
                            }
                        },true);
                    }
                }
            });
            ((Button) findViewById(R.id.list_error)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    final AlertDialog mWaitDownloadDialog = new AlertDialog.Builder(MainActivity.this)
                            .setView(getLayoutInflater().inflate(R.layout.wait_download, null))
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            if (dialog != null)
                                                dialog.cancel();
                                        }
                                    }).create();
                    mWaitDownloadDialog.setCancelable(false);
                    mWaitDownloadDialog.show();
                    new HTTPcomm(MainActivity.this, new HTTPcomm.OnFinish() {
                        @Override
                        public void onResult(String response) {
                            if (response != null) {
                                try {
                                    mWaitDownloadDialog.dismiss();
                                    JSONObject tmp = new JSONObject(response);
                                    JSONArray protocols = new JSONArray();
                                    protocols = tmp.getJSONArray("protocols");


                                    errorListDialog = new ErrorListDialog();
                                    errorListDialog.showDialog(MainActivity.this, protocols);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }, false );
                }
            });

            ((Button) findViewById(R.id.nfc_write)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, NFCWrite.class);
                    startActivity(intent);
                }
            });


            String[] langs = new String[]{getString(R.string.lang_de), getString(R.string.lang_en), getString(R.string.lang_cs), getString(R.string.lang_ro)};
            Spinner langSpinner = ((Spinner) findViewById(R.id.spinnerLang));
            langSpinner.setAdapter(new ArrayAdapter<String>(this, R.layout.item_spinner, Arrays.asList(langs)));


            Configuration conf = getResources().getConfiguration();
            String locale = getResources().getConfiguration().getLocales().get(0).toString();
            locale = locale.split("_")[0];
            ignoreSelection = true;
            if (locale.equals("de")) {
                langSpinner.setSelection(0);
            } else if (locale.equals("cs")) {
                langSpinner.setSelection(2);
            } else if (locale.equals("ro")) {
                langSpinner.setSelection(3);
            } else {
                langSpinner.setSelection(1);
            }
            language = "lang_" + locale;

            langSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        if (ignoreSelection) {
                            ignoreSelection = false;
                            return;
                        }
                        if (allowLangChange) {
                            allowLangChange = false;
                            Log.e("sdsd", "gsdgdfg");
                            switch (position) {
                                case 0:
                                    setLocale("de");
                                    break;
                                case 1:
                                    setLocale("en");
                                    break;
                                case 2:
                                    setLocale("cs");
                                    break;
                                case 3:
                                    setLocale("ro");
                                    break;
                            }
                        }

                    } catch (Exception e) {
                        Functions.err(e);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                    && !checkPermissionsGranted())
                return;

            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
            Functions.checkUpdateAndHWEnable(this);

            performSend = true;
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    public void setLocale(String lang) {

        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.setLocale(myLocale);
        res.updateConfiguration(conf, dm);
        Intent refresh = new Intent(this, MainActivity.class);
        startActivity(refresh);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (performSend && Functions.checkInternetEnable(MainActivity.this)) {
                performSend = false;
                new CPPsendRest(this);
            } else if (Functions.checkInternetEnable(MainActivity.this)
                    && getPreferences(MODE_PRIVATE).getInt(
                    LAST_UPDATE, 0) != (int) (new Date()
                    .getTime() * 0.001 / 3600 / 24)) {
                new CheckVersion(MainActivity.this);

                new HTTPcomm(this,null,true);

                Editor e = getPreferences(MODE_PRIVATE).edit();
                e.putInt(
                        LAST_UPDATE,
                        (int) (new Date().getTime() * 0.001 / 3600 / 24));
                e.commit();

            }

            allowLangChange = true;
            Functions.setupForegroundDispatch(this, mNfcAdapter);

            refreshErrors();
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    public void refreshErrors() {
        try {
            //Download notify
            new HTTPcomm(MainActivity.this, new HTTPcomm.OnFinish() {
                @Override
                public void onResult(String response) {
                    if (response != null) {
                        try {
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                            String username = prefs.getString("username_preference", "");

                            JSONObject tmp = new JSONObject(response);
                            JSONArray protocols = new JSONArray();
                            protocols = tmp.getJSONArray("protocols");

                            int count = 0;
                            for (int i = 0; i < protocols.length(); i++) {
                                if (protocols.optJSONObject(i).optString("username", "").equals(username)) {
                                    JSONArray errors = protocols.optJSONObject(i).optJSONArray("errors");
                                    count += errors.length();
                                }
                            }

                            TextView text = (TextView) findViewById(R.id.text_errors);
                            if (count > 0) {
                                text.setText(getString(R.string.youHave) + " " + count + " " + getString(R.string.errors));
                            } else {
                                text.setText("");

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            },false);
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    @Override
    protected void onPause() {
        Functions.stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }


    // ////////////////////////// OPTIONS ////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, PreferencesActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.download_questions) {
            if (Functions.checkInternetEnable(MainActivity.this)) {
                new HTTPcomm(this,null,true);
            }
            return true;
        }
        if (id == R.id.action_exit) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // //////////////////PERMISSIONS/////////////////
    @SuppressLint("NewApi")
    private boolean checkPermissionsGranted() {
        LinkedList<String> missing_permissions = new LinkedList<String>();
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (checkSelfPermission(Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.NFC);
        }
        if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.INTERNET);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            missing_permissions.add(Manifest.permission.CAMERA);
        }
        if (!missing_permissions.isEmpty()) {
            String[] permissions = new String[missing_permissions.size()];
            requestPermissions(missing_permissions.toArray(permissions),
                    REQUEST_PERMISSION_ID);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_ID) {
            for (int i = 0; i < grantResults.length; i++) {

                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                            getString(R.string.permissions_needed),
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            Functions.checkUpdateAndHWEnable(this);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        try {
            if (errorListDialog != null) {

                String action = intent.getAction();
                if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

                    String type = intent.getType();
                    if (FormFilling.MIME_TEXT_PLAIN.equals(type)) {

                        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                        Ndef ndef = Ndef.get(tag);
                        if (ndef == null) {
                            return;
                        }

                        NdefMessage ndefMessage = ndef.getCachedNdefMessage();

                        NdefRecord[] records = ndefMessage.getRecords();
                        for (NdefRecord ndefRecord : records) {
                            if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN
                                    && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                                readText(ndefRecord);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
    }

    private void readText(NdefRecord record) {
        try {
            byte[] payload = record.getPayload();

            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8"
                    : "UTF-16";

            int languageCodeLength = payload[0] & 0063;

            String resultstring = new String(payload,
                    languageCodeLength + 1, payload.length
                    - languageCodeLength - 1, textEncoding);

            resultstring = resultstring.replace(" ", "");

            if (resultstring.length() > 0) {
                String[] arr = resultstring.split(";;;");
                if (arr.length > 1 && errorListDialog != null && errorListDialog.editType != null
                        && errorListDialog.editSerial != null && errorListDialog.btnSearch != null) {
                    errorListDialog.editType.setText(arr[0]);
                    errorListDialog.editSerial.setText(arr[1]);
                    errorListDialog.btnSearch.performClick();
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
    }
}