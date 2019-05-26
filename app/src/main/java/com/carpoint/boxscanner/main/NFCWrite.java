package com.carpoint.boxscanner.main;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.carpoint.boxscanner.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

public class NFCWrite extends AppCompatActivity {

    private NfcAdapter mNfcAdapter;
    private AlertDialog mWaitNFCDialog;
    private AutoCompleteTextView editType;
    private EditText editSerial;
    private Boolean mWriteMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_write);

        // NFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this,
                    getString(R.string.noNFC),
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Check NFC
        if (NfcAdapter.getDefaultAdapter(this) != null && !NfcAdapter.getDefaultAdapter(this).isEnabled()) {
            Toast.makeText(this, getString(R.string.disableNFC),
                    Toast.LENGTH_LONG).show();
            finish();
        }

        JSONObject plans = null;
        try {
            FileMan fileMan = new FileMan(this, "");
            String data = fileMan.getDoc("questions.json");
            plans = new JSONObject(data);

        } catch (JSONException e) {
            Functions.toast(this, R.string.msg_error_json);
            finish();
            return;
        }

        //geting Types
        ArrayList<String> paletTypes = new ArrayList<>();

        JSONArray tmp = plans.optJSONArray("plans");
        for (int i = 0; i < tmp.length(); i++) {
            String name = tmp.optJSONObject(i).optString("name");
            boolean contains = false;
            for (int j = 0; j < paletTypes.size(); j++) {
                if (paletTypes.get(j).equals(name)) {
                    contains = true;
                    break;
                }
            }
            if (!contains)
                paletTypes.add(name);
        }


        editSerial = (EditText) findViewById(R.id.editSerial);
        editType = (AutoCompleteTextView) findViewById(R.id.autocompletetextview);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this,
                        R.layout.item_auto, paletTypes);
        editType.setAdapter(adapter);
        editType.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    editType.showDropDown();
                }
            }
        });

        Button btnWrite = (Button) findViewById(R.id.btnWrite);
        btnWrite.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                try {

                    showWriteDialog();
                } catch (Exception e) {
                    Functions.err(e);
                }
            }
        });

        Button btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        Functions.setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause() {
        Functions.stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }

    public void showWriteDialog() {
        // NFC WAIT DIALOG
        mWriteMode = true;
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.please_touch_tag, null);
        mWaitNFCDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        Toast.makeText(NFCWrite.this,
                                getString(R.string.registration_cancelled),
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                if (mWaitNFCDialog != null)
                                    mWaitNFCDialog.cancel();
                            }
                        }).create();
        mWaitNFCDialog.setCancelable(false);
        mWaitNFCDialog.show();
        Functions.setupForegroundDispatch(this, mNfcAdapter);
    }

    public void dismissNFCWaitDialog() {
        mWriteMode = false;
        if (mWaitNFCDialog != null) {
            mWaitNFCDialog.dismiss();
        }

    }

    public NdefMessage getTextAsNDEF() {
        String msg = editType.getText() + ";;;" + editSerial.getText();

        // create the message in according with the standard
        String lang = "en";
        byte[] textBytes = msg.getBytes();
        byte[] langBytes = null;
        try {
            langBytes = lang.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int langLength = langBytes.length;
        int textLength = textBytes.length;

        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, new byte[]{}, payload);
        return new NdefMessage(new NdefRecord[]{textRecord});
    }

    /*
     * Writes an NdefMessage to a NFC tag
     */
    public boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    return false;
                }

                ndef.writeNdefMessage(message);
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    public void clearFields() {
        if (editType != null) {
            editType.setText("");
        }
        if (editSerial != null) {
            editSerial.setText("");
        }
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (mWriteMode) {
            if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
                Tag detectedTag = intent
                        .getParcelableExtra(NfcAdapter.EXTRA_TAG);

                if (writeTag(getTextAsNDEF(),
                        detectedTag)) {
                    Toast.makeText(this,
                            getString(R.string.msg_write_VINCODE_successful),
                            Toast.LENGTH_SHORT).show();

                    dismissNFCWaitDialog();
                } else {
                    Toast.makeText(this,
                            getString(R.string.msg_write_VINCODE_failed),
                            Toast.LENGTH_SHORT).show();
                }
            } else if (mWriteMode
                    && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
                Tag detectedTag = intent
                        .getParcelableExtra(NfcAdapter.EXTRA_TAG);

                if (writeTag(getTextAsNDEF(),
                        detectedTag)) {
                    Toast.makeText(this,
                            getString(R.string.msg_write_VINCODE_successful),
                            Toast.LENGTH_SHORT).show();


                    dismissNFCWaitDialog();
                    clearFields();

                } else {
                    Toast.makeText(this,
                            getString(R.string.msg_write_VINCODE_failed),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Tag writing mode
        handleIntent(intent);

    }
}
