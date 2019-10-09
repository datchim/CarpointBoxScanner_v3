package com.carpoint.boxscanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

import android.support.v7.app.AppCompatActivity;
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
import com.carpoint.boxscanner.main.ListDialog;
import com.carpoint.boxscanner.main.FileMan;
import com.carpoint.boxscanner.main.FormFilling;
import com.carpoint.boxscanner.main.Functions;
import com.carpoint.boxscanner.main.HTTPcomm;
import com.carpoint.boxscanner.main.NFCWrite;
import com.carpoint.boxscanner.main.PreferencesActivity;
import com.carpoint.boxscanner.main.UserDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static String language = "lang_de", lang_de = "lang_de",
            tagErrorList = "errorList", tagErrorList2 = "errorList2", tagQuestions = "questions", tagUncompleteProto = "uncompleteProtocols", tagUserList = "getUserList", tagUsers = "users",
            tagProtocols = "protocols", tagUsername = "username", tagUncomplete = "uncomplete", tagErrors = "errors", tagSharedPrefs = "com.carpoint.boxscanner_preferences",
            CARPOINT_username = "username_preference", CARPOINT_fullname = "fullname_preference";

    private final int REQUEST_PERMISSION_ID = 99;
    private final String LAST_UPDATE = "LASTUPDATE";
    public ListDialog listDialog;
    private NfcAdapter mNfcAdapter;
    private Button login;
    private boolean allowLangChange, ignoreSelection, performSend;

    /////////////////////////////////////////ACTIVITY LIFE CYCLE /////////////////////////////////

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

                    String url = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("url_preference", "");
                    String name = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(CARPOINT_fullname, "");
                    if(name.length() == 0){
                        Functions.toast(MainActivity.this, R.string.settup);
                        return;
                    }

                    if (url.length() == 0) {
                        Functions.toast(MainActivity.this, R.string.settup);
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
                        }, tagQuestions, false);
                    }
                }
            });

            ((Button) findViewById(R.id.list_error)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    downloadList(tagErrorList);
                }
            });

            ((Button) findViewById(R.id.list_error2)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    downloadList(tagErrorList2);
                }
            });

            ((Button) findViewById(R.id.list_uncompleted)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    downloadList(tagUncompleteProto);
                }
            });

            login = ((Button) findViewById(R.id.login));
            login.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(getPrefs(MainActivity.this).getString(CARPOINT_fullname, "").length() ==0){
                        downloadList(tagUserList);
                    }else{
                        SharedPreferences.Editor pEditor = getPrefs(MainActivity.this).edit();
                        pEditor.remove(CARPOINT_username);
                        pEditor.remove(CARPOINT_fullname);
                        pEditor.remove(UserDialog.CARPOINT_password);
                        pEditor.commit();
                        login.setText(R.string.login);

                        TextView textErr = (TextView) findViewById(R.id.text_errors);
                        textErr.setText("");
                        TextView textUncomplete = (TextView) findViewById(R.id.text_uncompleted);
                        textUncomplete.setText("");
                        Functions.toast(MainActivity.this, R.string.youWereLoggedOut);
                    }
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

            Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
            Bitmap croppedImage = Bitmap.createBitmap(100, 100, conf);

            croppedImage = Bitmap.createScaledBitmap(croppedImage, croppedImage.getWidth() / 2,
                    croppedImage.getHeight() / 2, false);

        } catch (Exception e) {
            Functions.err(e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh(){
        try {
            if (performSend && Functions.checkInternetEnable(MainActivity.this)) {
                performSend = false;
                new CPPsendRest(this);
            } else if (Functions.checkInternetEnable(MainActivity.this)
                    && getPreferences(MODE_PRIVATE).getInt(
                    LAST_UPDATE, 0) != (int) (new Date()
                    .getTime() * 0.001 / 3600 / 24)) {
                new CheckVersion(MainActivity.this);

                new HTTPcomm(this, null, tagQuestions, false);

                Editor e = getPreferences(MODE_PRIVATE).edit();
                e.putInt(
                        LAST_UPDATE,
                        (int) (new Date().getTime() * 0.001 / 3600 / 24));
                e.commit();

            }
            allowLangChange = true;
            Functions.setupForegroundDispatch(this, mNfcAdapter);

            refreshErrors(tagErrorList);
            refreshErrors(tagErrorList2);
            refreshErrors(tagUncompleteProto);


            setHeadTitle();
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    @Override
    protected void onPause() {
        Functions.stopForegroundDispatch(this, mNfcAdapter);
        super.onPause();
    }

    /////////////////////////NETWORK////////////////////////

    public void downloadList(final String item) {

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
                try {
                    if (response != null) {
                        if (item.equals(tagUserList)) {
                            JSONObject tmp = new JSONObject(response);
                            JSONArray users = tmp.getJSONArray(tagUsers);
                            UserDialog userDialog = new UserDialog(MainActivity.this, users);
                            userDialog.showDialog();
                            mWaitDownloadDialog.dismiss();
                        } else {
                            JSONObject tmp = new JSONObject(response);
                            JSONArray protocols = new JSONArray();
                            protocols = tmp.getJSONArray(tagProtocols);

                            listDialog = new ListDialog();
                            listDialog.showDialog(MainActivity.this, protocols, item, tmp.optBoolean("is_admin"), tmp.optJSONArray("places"));

                        }
                    }
                    mWaitDownloadDialog.dismiss();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, item, false);

    }

    public void refreshErrors(final String item) {
        try {
            //Download notify
            new HTTPcomm(MainActivity.this, new HTTPcomm.OnFinish() {
                @Override
                public void onResult(String response) {
                    if (response != null) {
                        try {
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                            String username = prefs.getString(CARPOINT_username, "");

                            JSONObject tmp = new JSONObject(response);
                            JSONArray protocols = tmp.getJSONArray(tagProtocols);

                            int countErr = 0;
                            int countUncomplete = 0;

                            for (int i = 0; i < protocols.length(); i++) {
                                if (item.startsWith(tagErrorList)) {
                                    if (protocols.optJSONObject(i).optString(tagUsername, "").equals(username)) {
                                        JSONArray errors = protocols.optJSONObject(i).optJSONArray(tagErrors);
                                        countErr += errors.length();

                                        for (int j = 0; j < errors.length(); j++) {
                                            if (errors.optJSONObject(j).optInt("is_virtual") == 1) {
                                                countErr= findManuals(errors, errors.optJSONObject(j), errors.optJSONObject(j).optInt("id_question"), countErr);
                                            }
                                        }
                                    }
                                } else {
                                    if (protocols.optJSONObject(i).optString(tagUsername, "").equals(username) &&
                                            protocols.optJSONObject(i).optInt(tagUncomplete) == 1) {
                                        countUncomplete += 1;
                                    }
                                }
                            }

                            TextView textErr = (TextView) findViewById(R.id.text_errors);
                            TextView textErr2 = (TextView) findViewById(R.id.text_errors2);
                            TextView textUncomplete = (TextView) findViewById(R.id.text_uncompleted);

                            if (item.equals(tagErrorList)) {
                                if (countErr > 0) {
                                    textErr.setText(getString(R.string.youHave) + " " + countErr + " " + getString(R.string.errors));
                                } else {
                                    textErr.setText("");
                                }
                            }else if (item.equals(tagErrorList2)) {
                                if (countErr > 0) {
                                    textErr2.setText(getString(R.string.youHave) + " " + countErr + " " + getString(R.string.errors));
                                } else {
                                    textErr2.setText("");
                                }
                            } else {
                                if (countUncomplete > 0) {
                                    textUncomplete.setText(getString(R.string.uncomplete) + " " + countUncomplete + " " + getString(R.string.protocols));
                                } else {
                                    textUncomplete.setText("");
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, item, false);
        } catch (Exception e) {
            Functions.err(e);
        }
    }
    private int findManuals(JSONArray errors, JSONObject virtual, int id_q, int count) {
        try {
            for (int i = 0; i < errors.length(); i++) {
                if (errors.optJSONObject(i).optInt("id_question") == id_q
                        && errors.optJSONObject(i).optInt("is_virtual") == 0
                        && errors.optJSONObject(i).optInt("id_group") == -1) {
                    count--;
                }
            }

        } catch (Exception e) {
            Functions.err(e);
        }
        return count;
    }

    //////////////////////INNER LOGIC/////////////////

    public void setHeadTitle() {
        String fullname = getPrefs(MainActivity.this).getString(CARPOINT_fullname, "");
        if (fullname.length() > 0) {
            MainActivity.this.setTitle(MainActivity.this.getString(R.string.logged)
                    + fullname);
        } else {
            MainActivity.this.setTitle(MainActivity.this.getString(R.string.not_logged_in));
        }
        if(login != null){

            if(getPrefs(MainActivity.this).getString(CARPOINT_fullname, "").length() ==0){
                login.setText(R.string.login);
            }else{
                login.setText(R.string.logout);
            }
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

    private SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(tagSharedPrefs, Context.MODE_PRIVATE);
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
                new HTTPcomm(this, null, tagQuestions, false);
            }
            return true;
        }
        if (id == R.id.action_exit) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void login(){
        new FileMan(MainActivity.this, "").remove("questions.json");
        if (Functions.checkInternetEnable(MainActivity.this)) {
            new HTTPcomm(this, null, tagQuestions, false);
        }
    }

    ////////////////////////PERMISSIONS////////////////////////////

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

    ////////////////////////NFC////////////////////////////

    @Override
    protected void onNewIntent(Intent intent) {
        try {
            if (listDialog != null) {

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
                if (arr.length > 1 && listDialog != null && listDialog.editType != null
                        && listDialog.editSerial != null && listDialog.btnSearch != null) {
                    listDialog.editType.setText(arr[0]);
                    listDialog.editSerial.setText(arr[1]);
                    listDialog.btnSearch.performClick();
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
    }
}