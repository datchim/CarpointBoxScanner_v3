package com.carpoint.boxscanner.main;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.carpoint.boxscanner.MainActivity;
import com.carpoint.boxscanner.R;
import com.kyanogen.signatureview.SignatureView;
import com.mindorks.paracamera.Camera;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class FormFilling extends AppCompatActivity {


    private AlertDialog mWaitDialog;
    private ErrorDialog dialog, dialogVirtErr;
    private Camera camera;

    private String planName, serialNumber, prefillFullName;

    private LinearLayout llQuestions, ll2, llscroll, llserial2, llsign;
    private int actualLayout = 0, layoutCount, idLastProtocol = 0;
    private String actualLang = MainActivity.language;

    private JSONObject plans, selectedPlan, head;
    private JSONArray answers, errorCodesArray, errorAnswers, answersPrefill, errorCodesArraySolved, errorAnswersSolved;
    private JSONArray questionGroups;

    private Button btnSend, btnNext, btnClear, btnMenu;

    private SignatureView signatureView;
    private Bitmap bitmapSign, bitmap;
    private boolean doubleBackToExitPressedOnce = false, isController, is_universal, isFinishing;

    private ArrayList<Pair<Integer, Bitmap>> photos = new ArrayList<>();
    private ArrayList<Pair<String, Bitmap>> photosErr = new ArrayList<>();
    private ArrayList<Pair<JSONArray, Bitmap>> errorToSolve = new ArrayList<>();
    private LocationListener mLocationListener;

    private int photo_id_q, photo_id_g, selectedPlanIndex = -1;
    private NfcAdapter mNfcAdapter;
    private ActionBar actionbar;

    /////////////////////////////////////////ACTIVITY LIFE CYCLE /////////////////////////////////

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form_filling);

        actionbar = getSupportActionBar();
        getSupportActionBar().setDisplayOptions
                (actionbar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar);


        btnSend = (Button) findViewById(R.id.btnSend);
        btnMenu = (Button) findViewById(R.id.btnMenu);
        btnNext = (Button) findViewById(R.id.btnNext);
        btnClear = (Button) findViewById(R.id.btnClear);
        llQuestions = (LinearLayout) findViewById(R.id.questions);
        signatureView = (SignatureView) findViewById(R.id.signature_view);


        llscroll = (LinearLayout) findViewById(R.id.llscroll);
        llserial2 = (LinearLayout) findViewById(R.id.llserial2);
        llsign = (LinearLayout) findViewById(R.id.llsign);

        final EditText textViewSerial = (EditText) findViewById(R.id.editSerial);


        head = new JSONObject();
        answers = new JSONArray();
        errorAnswers = new JSONArray();
        errorAnswersSolved = new JSONArray();
        errorCodesArraySolved = new JSONArray();


        try {
            FileMan fileMan = new FileMan(FormFilling.this, "");
            String data = fileMan.getDoc("questions.json");
            plans = new JSONObject(data);

        } catch (JSONException e) {
            Functions.toast(this, R.string.msg_error_json);
            finish();
            return;
        }


        JSONObject login = plans.optJSONObject(T.tagLogin);
        isController = login.optBoolean(T.tagControler);

        //geting Types
        final ArrayList<String> paletTypes = new ArrayList<>();
        JSONArray tmp = plans.optJSONArray(T.tagPlans);
        for (int i = 0; i < tmp.length(); i++) {
            paletTypes.add(tmp.optJSONObject(i).optString(T.tagName) + " (" + tmp.optJSONObject(i).optString(T.tagDrawDate) + ")");
        }

        //geting Error Codes

        try {
            FileMan fileMan = new FileMan(FormFilling.this, "PAUSE");
            if (fileMan.fileExists("data")) {
                JSONObject obj = new JSONObject(fileMan.getDoc("data"));

                planName = obj.optString("planName");
                serialNumber = obj.optString("serialNumber");
                prefillFullName = obj.optString("prefillFullName");
                actualLang = obj.optString("actualLang");
                actualLayout = obj.optInt("actualLayout");
                idLastProtocol = obj.optInt("idLastProtocol");
                photo_id_q = obj.optInt("photo_id_q");
                photo_id_g = obj.optInt("photo_id_g");
                selectedPlanIndex = obj.optInt("selectedPlanIndex");
                selectedPlan = obj.optJSONObject("selectedPlan");
                head = obj.optJSONObject("head");
                answers = obj.optJSONArray("answers");
                errorCodesArray = obj.optJSONArray("errorCodesArray");
                errorAnswers = obj.optJSONArray("errorAnswers");
                answersPrefill = obj.optJSONArray("answersPrefill");
                errorCodesArraySolved = obj.optJSONArray("errorCodesArraySolved");
                questionGroups = obj.optJSONArray("questionGroups");

                ((TextView) findViewById(R.id.action_bar_title)).setText(planName + "/" + serialNumber);

                layoutCount = questionGroups.length() + 3;
                is_universal = selectedPlan.optInt(T.tagIsUniversal, 0) == 1;

                ArrayList<String> imgs = fileMan.getAllDocs();
                for (int i = 0; i < imgs.size(); i++) {
                    if (imgs.get(i).startsWith("img_"))
                        photos.add(new Pair<Integer, Bitmap>(Integer.parseInt(imgs.get(i).replace("img_", "")),
                                fileMan.getBitmap(imgs.get(i))));
                    if (imgs.get(i).startsWith("err_"))
                        photosErr.add(new Pair<String, Bitmap>(imgs.get(i).replace("err_", ""),
                                fileMan.getBitmap(imgs.get(i))));
                }

                refresh();
            } else {
                errorCodesArray = plans.optJSONArray(T.tagError);
                errorCodesArraySolved = new JSONArray(errorCodesArray.toString());
            }
        } catch (Exception e) {
            Functions.toast(this, R.string.msg_no_error_codes);
            errorCodesArray = new JSONArray();
        }


        final AutoCompleteTextView textVievType = (AutoCompleteTextView) findViewById(R.id.autocompletetextview);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this,
                        R.layout.item_auto, paletTypes);
        textVievType.setAdapter(adapter);
        textVievType.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    textVievType.showDropDown();
                }
            }
        });

        textVievType.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String item = parent.getItemAtPosition(position).toString();
                position = paletTypes.indexOf(item);
                selectedPlanIndex = position;
                JSONArray tmp = plans.optJSONArray(T.tagPlans);
                textVievType.setText(tmp.optJSONObject(position).optString(T.tagName));
            }
        });


        btnNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    boolean filled = true;

                    try {
                        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                                getWindow().getDecorView().getRootView().getWindowToken(), 0);
                    } catch (Exception e) {

                    }
                    if (actualLayout == 0) {
                        ((TextView) findViewById(R.id.action_bar_title)).setText(R.string.title_activity_scanning);
                        planName = textVievType.getText().toString();
                        serialNumber = textViewSerial.getText().toString();
                        filled = planName.length() > 0 && serialNumber.length() > 0;
                        if (filled) {
                            ((TextView) findViewById(R.id.action_bar_title)).setText(planName + "/" + serialNumber);

                            JSONArray tmp = plans.optJSONArray(T.tagPlans);
                            if (selectedPlanIndex > -1) {
                                selectedPlan = tmp.optJSONObject(selectedPlanIndex);
                            } else {
                                for (int i = tmp.length() - 1; i > -1; i--) {
                                    if (tmp.optJSONObject(i).optString(T.tagName).equals(planName)) {
                                        selectedPlan = tmp.optJSONObject(i);
                                        break;
                                    }
                                }
                            }

                            if (selectedPlan != null) {
                                questionGroups = selectedPlan.optJSONArray(T.tagQuestionsGroups);
                                layoutCount = questionGroups.length() + 3;
                                is_universal = selectedPlan.optInt(T.tagIsUniversal, 0) == 1;


                                JSONObject params = new JSONObject();
                                params.put(T.tagName, planName);
                                params.put(T.tagSerial, serialNumber);

                                if (selectedPlanIndex > -1) {
                                    params.put(T.tagIdPlan, tmp.optJSONObject(selectedPlanIndex).optInt(T.tagIdPlan, -1));
                                }

                                waitDialog(true);

                                getProtocol(params);

                            } else {
                                Toast.makeText(FormFilling.this, R.string.noScenary, Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else if (actualLayout == layoutCount - 1) {
                        filled = head.has(T.tagLastQ);
                    } else {
                        filled = isPageFilled(questionGroups.getJSONObject(actualLayout - 2));
                    }

                    if (filled) {
                        actualLayout++;
                        refresh();
                    } else {
                        Toast.makeText(FormFilling.this, R.string.fillAll, Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    Functions.err(e);
                }
            }
        });

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actualLayout = 1;
                refresh();
            }
        });


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                bitmapSign = signatureView.getSignatureBitmap();
                if (bitmapSign == null) {
                    Toast.makeText(FormFilling.this, R.string.fillAll, Toast.LENGTH_SHORT).show();
                    return;
                }
                sendProtocol(bitmapSign);
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signatureView.clearCanvas();
            }
        });

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                try {
                    if (location != null && head != null) {
                        head.put(T.tagLocLat, location.getLatitude());
                        head.put(T.tagLocLng, location.getLongitude());
                        if (mLocationListener != null) {
                            LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                            mLocationManager.removeUpdates(mLocationListener);
                            mLocationListener = null;
                        }
                    }
                } catch (Exception e) {
                    Functions.err(e);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000,
                10, mLocationListener);
        Location last = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (last != null && head != null) {
            try {
                head.put(T.tagLocLat, last.getLatitude());
                head.put(T.tagLocLng, last.getLongitude());
            } catch (Exception e) {
                Functions.err(e);
            }
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            textViewSerial.setText(extras.getString(T.tagSerial));
            textVievType.setText(extras.getString(T.tagType));
            int idPlan = extras.getInt(T.tagIdPlan);

            JSONArray temp = plans.optJSONArray(T.tagPlans);
            for (int i = 0; i < temp.length(); i++) {
                if (temp.optJSONObject(i).optInt(T.tagIdPlan) == idPlan) {
                    selectedPlanIndex = i;
                    break;
                }
            }
            btnNext.callOnClick();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode != RESULT_CANCELED) {
                bitmap = camera.getCameraBitmap();

                ImageView cameraPhoto = (ImageView) findViewById(R.id.camera_image);
                if (bitmap != null && cameraPhoto != null) {
                    cameraPhoto.setImageBitmap(bitmap);
                    boolean found = false;
                    for (int i = 0; i < photos.size(); i++) {
                        if (photos.get(i).first == photo_id_q) {
                            photos.set(i, new Pair<Integer, Bitmap>(photo_id_q, bitmap));
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        photos.add(new Pair<Integer, Bitmap>(photo_id_q, bitmap));
                    putAnswer(photo_id_q, photo_id_g, T.tagPhoto, "photo_" + photo_id_q, false);
                }
            }
        }
        if (requestCode == 2) {
            if (resultCode != RESULT_CANCELED) {

                bitmap = dialog.getCamera().getCameraBitmap();

                try {
                    if (bitmap != null && dialog != null) {
                        int idQ = dialog.photoIdQuestion;
                        int idErr = dialog.photoIdError;
                        boolean found = false;
                        for (int i = 0; i < photos.size(); i++) {
                            if (photosErr.get(i).first.equals(idQ + "_" + idErr)) {
                                photosErr.set(i, new Pair<String, Bitmap>(idQ + "_" + idErr, bitmap));
                                found = true;
                                break;
                            }
                        }
                        if (!found)
                            photosErr.add(new Pair<String, Bitmap>(idQ + "_" + idErr, bitmap));
                        Functions.toast(FormFilling.this, R.string.photo_save);
                        if (!dialog.isDialog) refresh();
                    }
                } catch (Exception e) {
                    Functions.err(e);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (actualLayout > 1) {

                actualLayout = 1;
                refresh();
            } else {

                if (doubleBackToExitPressedOnce) {
                    isFinishing = true;
                    new FileMan(FormFilling.this, "").removeFolder("PAUSE");
                    super.onBackPressed();
                    return;
                }

                this.doubleBackToExitPressedOnce = true;
                Toast.makeText(this, R.string.backHit, Toast.LENGTH_SHORT).show();

                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        doubleBackToExitPressedOnce = false;
                    }
                }, 2000);
            }
        } catch (Exception e) {
            Functions.err(e);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera != null) camera.deleteImage();
        if (mLocationListener != null) {
            LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            mLocationManager.removeUpdates(mLocationListener);
            mLocationListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            Functions.setupForegroundDispatch(this, mNfcAdapter);
            new FileMan(FormFilling.this, "").removeFolder("PAUSE");
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    @Override
    protected void onPause() {
        try {
            if (!isFinishing && answers != null && answers.length() > 0) {

                JSONObject toSave = new JSONObject();
                toSave.put("planName", planName);
                toSave.put("serialNumber", serialNumber);
                toSave.put("prefillFullName", prefillFullName);
                toSave.put("actualLayout", actualLayout);
                toSave.put("idLastProtocol", idLastProtocol);
                toSave.put("actualLang", actualLang);
                toSave.put("selectedPlan", selectedPlan);
                toSave.put("head", head);
                toSave.put("answers", answers);
                toSave.put("errorCodesArray", errorCodesArray);
                toSave.put("errorAnswers", errorAnswers);
                toSave.put("answersPrefill", answersPrefill);
                toSave.put("errorCodesArraySolved", errorCodesArraySolved);
                toSave.put("errorAnswersSolved", errorAnswersSolved);
                toSave.put("questionGroups", questionGroups);
                toSave.put("isController", isController);
                toSave.put("is_universal", is_universal);
                toSave.put("photo_id_q", photo_id_q);
                toSave.put("photo_id_g", photo_id_g);
                toSave.put("selectedPlanIndex", selectedPlanIndex);

                FileMan f = new FileMan(FormFilling.this, "PAUSE");
                f.saveDoc("data", toSave);

                for (int i = 0; i < photos.size(); i++) {
                    f.saveBitmap("img_" + photos.get(i).first, photos.get(i).second);
                }
                for (int i = 0; i < photosErr.size(); i++) {
                    f.saveBitmap("err_" + photosErr.get(i).first, photosErr.get(i).second);
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
        Functions.stopForegroundDispatch(this, mNfcAdapter);
        super.onPause();
    }

    /////////////////////////////////////////DISPLAYING///////////////////////////////////////////////

    public void refresh() {
        try {
            llQuestions.removeAllViews();


            if (actualLayout == 0)
                ((TextView) findViewById(R.id.action_bar_title)).setText(R.string.title_activity_scanning);
            if (actualLayout > 0 && actualLayout < questionGroups.length() + 2) {
                llscroll.setVisibility(View.VISIBLE);

                if (actualLayout == 1) {
                    llscroll.setVisibility(View.VISIBLE);
                    displayList();
                } else {
                    JSONObject group = questionGroups.optJSONObject(actualLayout - 2);
                    displayQuestionsGroups(group);

                    Button bt = (Button) getLayoutInflater().inflate(R.layout.item_button, llQuestions, false);
                    bt.setText(R.string.menu);
                    bt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            actualLayout = 1;
                            refresh();
                        }
                    });
                    llQuestions.addView(bt);
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ScrollView scrollView =
                                ((ScrollView) findViewById(R.id.ScrollView));
                        if (scrollView != null) scrollView.smoothScrollTo(0, 0);
                    }
                }, 100);

            } else if (actualLayout == layoutCount - 1) {
                if (isController) {
                    llscroll.setVisibility(View.VISIBLE);

                    JSONObject last = selectedPlan.optJSONObject(T.tagLastQ);

                    displayQuestion(last, true);

                    Button bt = (Button) getLayoutInflater().inflate(R.layout.item_button, llQuestions, false);
                    bt.setText(R.string.menu);
                    bt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            actualLayout = 1;
                            refresh();
                        }
                    });
                    llQuestions.addView(bt);
                }

            } else {
                llscroll.setVisibility(View.GONE);
            }

            llserial2.setVisibility(actualLayout == 0 ? View.VISIBLE : View.GONE);

            llsign.setVisibility(actualLayout == layoutCount ? View.VISIBLE : View.GONE);

        } catch (Exception e) {
            Functions.err(e);
        }
    }

    private void displayList() {
        try {
            boolean isAllOk = true;

            getLayoutInflater().inflate(R.layout.item_signposthead, llQuestions, true);
            for (int i = 0; i < questionGroups.length(); i++) {
                final JSONObject question = questionGroups.optJSONObject(i);
                if (!isController && question.optInt(T.tagOnlyController) == 1) {
                    continue;
                }

                LinearLayout ll = (LinearLayout) getLayoutInflater().inflate(R.layout.item_signpost, llQuestions, false);

                Button btnQuNa = ll.findViewById(R.id.question_name);

                TextView state = ll.findViewById(R.id.state);
                Button btnErr = ll.findViewById(R.id.btn_error);
                TextView txtErrCount = ll.findViewById(R.id.cout_err);
                int counter = countGroupErrors(errorAnswers, questionGroups.optJSONObject(i)).second;
                final Pair<JSONArray, Integer> counterrSolved = countGroupErrors(errorAnswersSolved, questionGroups.optJSONObject(i));

                if (isPageFilled(questionGroups.getJSONObject(i)) && counterrSolved.second == counter) {
                    state.setText(R.string.state_ok);
                    state.setBackgroundColor(Color.GREEN);

                } else {
                    if (isAllOk) isAllOk = false;
                }

                if (counterrSolved.second != counter) {
                    state.setText(R.string.state_nok);
                    state.setBackgroundColor(Color.RED);
                    txtErrCount.setVisibility(View.GONE);
                    btnErr.setText(String.valueOf(counter));
                    btnErr.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (question.has(T.tagQuestions)) {
                                RepairErrorDialog rDialog = new RepairErrorDialog(FormFilling.this, errorCodesArray, errorAnswers, errorCodesArraySolved, errorAnswersSolved,
                                        question.optJSONArray(T.tagQuestions));
                                rDialog.showDialog();
                            } else {
                                RepairErrorDialog rDialog = new RepairErrorDialog(FormFilling.this, errorCodesArray, errorAnswers, errorCodesArraySolved, errorAnswersSolved,
                                        question);
                                rDialog.showDialog();
                            }
                        }
                    });
                } else {
                    btnErr.setVisibility(View.GONE);
                    txtErrCount.setText((String.valueOf(counter)));
                }

                TextView repa = ll.findViewById(R.id.cout_repaired);
                repa.setText(String.valueOf(counterrSolved.second));
                repa.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new RepairedDialog(FormFilling.this, counterrSolved.first, errorCodesArraySolved);
                    }
                });

                btnQuNa.setText(question.optString(actualLang, ""));
                final int finalI = i;
                btnQuNa.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        actualLayout = finalI + 2;
                        refresh();
                    }
                });
                llQuestions.addView(ll);
            }

            LinearLayout llBtn = (LinearLayout) getLayoutInflater().inflate(R.layout.form_filling_bottom, llQuestions, false);

            Button btnBlock = llBtn.findViewById(R.id.btnMenuBlock);
            Button btnUnrelease = llBtn.findViewById(R.id.btnMenuUnrelease);
            Button btnRelease = llBtn.findViewById(R.id.btnMenuRelease);

            btnRelease.setEnabled(isAllOk);

            btnRelease.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    putAnswer(-3, -3, T.tagYesNo, T.tagPositiveAnswer, true);
                    actualLayout = layoutCount;
                    refresh();
                }
            });
            btnUnrelease.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    putAnswer(-3, -3, T.tagYesNo, T.tagNegativeAnswer, true);
                    actualLayout = layoutCount;
                    refresh();
                }
            });

            btnBlock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDialog(false);
                }
            });

            llQuestions.addView(llBtn);

        } catch (Exception e) {
            Functions.err(e);
        }
    }

    public void displayQuestionsGroups(JSONObject group) {

        if (group.has(T.tagQuestions)) {
            LinearLayout ll = (LinearLayout) getLayoutInflater().inflate(R.layout.item_head, llQuestions, false);
            ((TextView) ll.findViewById(R.id.text)).setText(group.optString(actualLang, ""));
            llQuestions.addView(ll);

            JSONArray questions = group.optJSONArray(T.tagQuestions);
            for (int i = 0; i < questions.length(); i++) {
                displayQuestionsGroups(questions.optJSONObject(i));
            }
        } else {
            displayQuestion(group, false);
        }
    }

    public void displayQuestion(final JSONObject q, final boolean isLast) {

        if (!isController && q.optInt("only_controller") == 1) {
            return;
        }

        final int q_id = q.optInt(T.tagIdQuestion, -1);
        final int g_id = q.optInt(T.tagIdGroup, -1);
        final String type = q.optString(T.tagType, "");
        final String commonErrors = q.optString(T.tagCmnError);

        ll2 = null;
        switch (type) {
            case T.tagYesNo:
            case T.tagNumberYes:
                ll2 = (LinearLayout) getLayoutInflater().inflate(R.layout.item_question, llQuestions, false);
                break;
            case T.tagText:
            case T.tagNumber:
                ll2 = (LinearLayout) getLayoutInflater().inflate(R.layout.item_question_input, llQuestions, false);
                break;
            case T.tagPhoto:
                ll2 = (LinearLayout) getLayoutInflater().inflate(R.layout.item_question_photo, llQuestions, false);
                break;
        }
        if (type.equals(T.tagText) || (type.equals(T.tagNumber)) || (type.equals(T.tagYesNo)) || (type.equals(T.tagNumberYes))) {
            ll2.findViewById(R.id.btn_error).setVisibility(isController ? View.VISIBLE : View.GONE);
            ll2.findViewById(R.id.btn_make_photo).setVisibility(isController ? View.VISIBLE : View.GONE);


            final Button btnErr = ll2.findViewById(R.id.btn_error);
            btnErr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog = new ErrorDialog(FormFilling.this, q_id, commonErrors, errorCodesArray, errorAnswers, photosErr);
                    dialog.showDialog();
                }
            });

            final Button btnAdErrPhoto = ll2.findViewById(R.id.btn_make_photo);
            btnAdErrPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog = new ErrorDialog(FormFilling.this, q_id, commonErrors, errorCodesArray, errorAnswers, photosErr);
                    dialog.addError(FormFilling.this.getString(R.string.photoError), true, 0, false);
                    dialog.takePicture("err_photo_");
                }
            });
        }

        ((TextView) ll2.findViewById(R.id.text)).setText(q.optString(actualLang, ""));

        prefillQuestion((TextView) ll2.findViewById(R.id.textPrefill), q_id);
        if (isController && !type.equals(T.tagPhoto)) {
            countErrors((Button) ll2.findViewById(R.id.btn_error), q_id, ll2);
        }

        if (type.equals(T.tagYesNo) || type.equals(T.tagNumberYes)) {
            llQuestions.addView(ll2);
            final TextView text = ll2.findViewById(R.id.text);
            final MyEditText editAnswer = ll2.findViewById(R.id.editAnswer);
            final RadioGroup rg = ll2.findViewById(R.id.radioGroup);
            final RadioButton rgNo = ll2.findViewById(R.id.rgNo);
            final RadioButton rgYes = ll2.findViewById(R.id.rgYes);
            final RadioButton rgNa = ll2.findViewById(R.id.rgNa);
            final Button btnErr = ll2.findViewById(R.id.btn_error);
            final LinearLayout ll = ll2;

            String answer;
            answer = getAnswer(q_id);

            switch (type) {
                case T.tagYesNo:
                    if (answer.equals(T.tagPositiveAnswer)) {
                        rg.check(R.id.rgYes);
                    } else if (answer.equals(T.tagNegativeAnswer)) {
                        rg.check(R.id.rgNo);
                    } else if (is_universal && answer.equals(T.tagNaAnswer))
                        rg.check(R.id.rgNa);
                    break;
                case T.tagNumberYes:
                    if (answer.equals(T.tagYes)) {
                        rg.check(R.id.rgYes);
                    } else if (!answer.equals("")) {
                        rg.check(R.id.rgNo);
                        editAnswer.setVisibility(View.VISIBLE);
                        editAnswer.setText(answer);
                        makeColor(q, q_id, text, editAnswer, Integer.parseInt(answer), false);
                    }
                    break;
            }


            rgNo.setOnClickListener(new DoubleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    putAnswer(q_id, g_id, type, T.tagNegativeAnswer, isLast);

                    if (type.equals(T.tagNumberYes)) {
                        editAnswer.setVisibility(View.VISIBLE);
                        editAnswer.setText("");
                    } else {
                        dialogVirtErr = new ErrorDialog(FormFilling.this, q_id, errorAnswers, errorCodesArray);
                        dialogVirtErr.addError(q.optString(actualLang, "") + " - " + (FormFilling.this.getString(R.string.no).toUpperCase()), false, -2, true);

                    }
                    countErrors(btnErr, q_id, ll);
                }

                @Override
                public void onDoubleClick(View v) {
                    putAnswer(q_id, g_id, type, T.tagNone, isLast);
                    dialogVirtErr = new ErrorDialog(FormFilling.this, q_id, errorAnswers, errorCodesArray);
                    dialogVirtErr.removeError(-2, q_id);
                    dialogVirtErr.removeVirtualCode(q_id);
                    countErrors(btnErr, q_id, ll);
                    if (type.equals(T.tagNumberYes)) {
                        editAnswer.setVisibility(View.GONE);
                        text.setTextColor(Color.BLACK);
                    }
                    rg.clearCheck();

                }
            });

            rgYes.setOnClickListener(new DoubleClickListener() {

                @Override
                public void onSingleClick(View v) {
                    if (type.equals(T.tagNumberYes)) {
                        editAnswer.setVisibility(View.GONE);
                        text.setTextColor(Color.BLACK);
                        putAnswer(q_id, g_id, type, T.tagYes, isLast);
                    } else {
                        putAnswer(q_id, g_id, type, T.tagPositiveAnswer, isLast);
                    }
                    try {
                        if (dialogVirtErr != null) {
                            dialogVirtErr.removeError(-2, q_id);
                            dialogVirtErr.removeVirtualCode(q_id);
                            countErrors(btnErr, q_id, ll);
                        }
                    } catch (Exception e) {
                        Functions.err(e);
                    }
                }

                @Override
                public void onDoubleClick(View v) {
                    putAnswer(q_id, g_id, type, T.tagNone, isLast);
                    rg.clearCheck();
                }
            });

            if (is_universal) {
                rgNa.setVisibility(View.VISIBLE);
                rgNa.setOnClickListener(new DoubleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        try {
                            putAnswer(q_id, g_id, type, T.tagNaAnswer, isLast);
                            if (dialogVirtErr != null) {
                                dialogVirtErr.removeError(-2, q_id);
                                dialogVirtErr.removeVirtualCode(q_id);
                                countErrors(btnErr, q_id, ll);
                            }
                        } catch (Exception e) {
                            Functions.err(e);
                        }
                    }

                    @Override
                    public void onDoubleClick(View v) {
                        putAnswer(q_id, g_id, type, T.tagNone, isLast);
                        rg.clearCheck();
                        countErrors(btnErr, q_id, ll);
                    }
                });
            }


            editAnswer.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {

                    dialogVirtErr = new ErrorDialog(FormFilling.this, q_id, errorAnswers, errorCodesArray);
                    try {
                        dialogVirtErr.removeError(-2, q_id);
                        dialogVirtErr.removeVirtualCode(q_id);
                    } catch (Exception e) {
                        Functions.err(e);
                    }
                    if (s.length() > 0)
                        makeColor(q, q_id, text, editAnswer, Integer.parseInt(s.toString()), true);
                    putAnswer(q_id, g_id, T.tagNumberYes, s.toString(), false);

                    if (s.toString().trim().equals("")) {
                        dialogVirtErr.removeError(-2, q_id);
                        dialogVirtErr.removeVirtualCode(q_id);
                    }
                    countErrors(btnErr, q_id, ll);
                }
            });
        }

        if (type.equals(T.tagNumber) || (type.equals(T.tagText))) {
            llQuestions.addView(ll2);
            final MyEditText editAnswer = ll2.findViewById(R.id.editAnswer);
            final TextView text = ll2.findViewById(R.id.text);
            final Button btnErr = ll2.findViewById(R.id.btn_error);
            final LinearLayout ll = ll2;

            editAnswer.setText(getAnswer(q_id));
            if (type.equals(T.tagNumber)) {
                editAnswer.setInputType(InputType.TYPE_CLASS_NUMBER);
                String answerNum = getAnswer(q_id);
                if (!answerNum.equals("")) {
                    int answerNumber = Integer.parseInt(answerNum);
                    makeColor(q, q_id, text, editAnswer, answerNumber, false);
                }
            } else {
                editAnswer.setInputType(InputType.TYPE_CLASS_TEXT);
            }

            editAnswer.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        if (type.equals(T.tagNumber)) {
                            makeColor(q, q_id, text, editAnswer, (Integer.parseInt(s.toString())), true);
                            countErrors(btnErr, q_id, ll);
                        }
                        if (answers != null) {
                            putAnswer(q_id, g_id, type, s.toString(), isLast);
                        }
                    } catch (Exception e) {
                        Functions.err(e);
                    }
                }
            });
        }

        if (type.equals(T.tagPhoto)) {
            ll2 = (LinearLayout) getLayoutInflater().inflate(R.layout.item_question_photo, llQuestions, false);
            ((TextView) ll2.findViewById(R.id.text)).setText(q.optString(actualLang, ""));
            llQuestions.addView(ll2);

            for (int i = 0; i < photos.size(); i++) {
                if (photos.get(i).first == q_id) {
                    ImageView cameraPhoto = ll2.findViewById(R.id.camera_image);
                    cameraPhoto.setImageBitmap(photos.get(i).second);
                    break;
                }
            }
            ImageView tmp = ll2.findViewById(R.id.camera_image);
            tmp.getDrawable();
            if (tmp.getDrawable() == null) Log.e("img", "null");
            else Log.e("img", "have image");
            if (idLastProtocol != 0 && !getAnswer(q_id).equals("") && ((ImageView) ll2.findViewById(R.id.camera_image)).getDrawable() == null) {

                final AlertDialog mWaitDownloadDialog = new AlertDialog.Builder(FormFilling.this)
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

                new HTTPcomm(FormFilling.this, idLastProtocol, getAnswer(q_id), new HTTPcomm.OnFinishBitmap() {
                    @Override
                    public void onResult(Bitmap response) {
                        mWaitDownloadDialog.dismiss();
                        if (response != null) {
                            ImageView cameraPhoto = ll2.findViewById(R.id.camera_image);
                            cameraPhoto.setImageBitmap(response);

                        }
                    }
                });
            }

            Button makePhoto = ll2.findViewById(R.id.btn_make_photo);
            makePhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    photo_id_q = q_id;
                    photo_id_g = g_id;
                    try {
                        camera = new Camera.Builder()
                                .resetToCorrectOrientation(true)
                                .setTakePhotoRequestCode(1)
                                .setDirectory("BoxScannerPics")
                                .setName("photo_" + System.currentTimeMillis())
                                .setImageFormat(Camera.IMAGE_JPEG)
                                .setCompression(75)
                                .build(FormFilling.this);
                        camera.takePicture();
                    } catch (Exception e) {
                        Functions.err(e);
                    }
                }
            });


        }
    }

    private void prefillQuestion(TextView text, int id_q) {
        try {
            if (answersPrefill != null) {
                for (int i = 0; i < answersPrefill.length(); i++) {
                    if (answersPrefill.optJSONObject(i).optInt(T.tagIdQuestion) == id_q) {

                        String answerP = answersPrefill.optJSONObject(i).optString(T.tagAnswer);
                        if (answersPrefill.optJSONObject(i).optString(T.tagType).equals(T.tagYesNo)) {
                            if (answerP.equals(T.tagPositiveAnswer)) {
                                answerP = getString(R.string.yes);
                            } else if (is_universal && answerP.equals(T.tagNaAnswer)) {
                                answerP = getString(R.string.notavailable);
                            } else {
                                answerP = getString(R.string.no);
                            }
                        } else if (answersPrefill.optJSONObject(i).optString(T.tagType).equals(T.tagNumberYes)) {
                            answerP = answerP.equals(T.tagYes) ? getString(R.string.yes) : answersPrefill.optJSONObject(i).optString(T.tagAnswer);
                        }
                        if (answerP.length() > 0 && answersPrefill.optJSONObject(i).has(T.tagCreatedBy)) {
                            text.setVisibility(View.VISIBLE);
                            text.setText(answersPrefill.optJSONObject(i).optString(T.tagFullname) + " " + getString(R.string.filled) + ": " + answerP);
                            return;
                        }
                    }
                }
            }
            text.setVisibility(View.GONE);
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    /////////////////////////////////////////INNER LOGIC///////////////////////////////////////////////

    private void makeColor(JSONObject q, int q_id, TextView text, EditText editAnswer, int s, boolean isErr) {
        try {

            if (q.has(T.tagLimitPlus) || q.has(T.tagLimitMinus)) {

                final int limitMinus = (q.has(T.tagLimitMinus) ? q.getInt(T.tagLimitMinus) : 0);
                final int limitPlus = (q.has(T.tagLimitPlus) ? q.getInt(T.tagLimitPlus) : Integer.MAX_VALUE);


                if ((s > limitPlus) || (s < limitMinus)) {
                    text.setTextColor(getResources().getColor(R.color.red, getResources().newTheme()));
                    editAnswer.setTextColor(getResources().getColor(R.color.red, getResources().newTheme()));
                    if (isErr) {
                        dialog = new ErrorDialog(FormFilling.this, q.optInt(T.tagIdQuestion), errorAnswers, errorCodesArray);
                        dialog.addError(q.optString(actualLang, "") + " - " + FormFilling.this.getString(R.string.out_of_limit) + ": " + s, false, -2, true);
                    }
                } else {
                    text.setTextColor(getResources().getColor(R.color.black, getResources().newTheme()));
                    editAnswer.setTextColor(getResources().getColor(R.color.black, getResources().newTheme()));
                    if (isErr) {
                        if (dialog != null) {
                            dialog.removeError(-2, q_id);
                            dialog.removeVirtualCode(q_id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }

    }

    private boolean isPageFilled(JSONObject group) {
        boolean result = true;
        try {
            if (group.has(T.tagQuestions)) {
                JSONArray tmp = group.optJSONArray(T.tagQuestions);
                for (int i = 0; i < tmp.length(); i++) {
                    result &= isPageFilled(tmp.optJSONObject(i));
                }
            } else {
                if (!isController && group.optInt(T.tagOnlyController) == 1) {
                    return true;
                }

                if (group.optString(T.tagType).equals(T.tagPhoto)) {
                    result = getAnswer(group.optInt(T.tagIdQuestion, -1)).length() > 0
                            || group.optInt(T.tagPhotoReq, -1) == 0;
                } else {
                    result = getAnswer(group.optInt(T.tagIdQuestion, -1)).length() > 0;
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
        return result;
    }


    private String getAnswer(int id_q) {
        try {
            for (int i = 0; i < answers.length(); i++) {
                if (answers.optJSONObject(i).optInt(T.tagIdQuestion) == id_q)
                    return answers.optJSONObject(i).optString(T.tagAnswer);
            }
        } catch (Exception e) {
            Functions.err(e);
        }
        return "";
    }

    private void putAnswer(int id_question, int id_group, String type, String answer, boolean isLast) {
        try {
            JSONObject tmp = new JSONObject();
            tmp.put(T.tagIdQuestion, id_question);
            if (id_group != -1)
                tmp.put(T.tagIdGroup, id_group);
            tmp.put(T.tagType, type);
            tmp.put(T.tagAnswer, answer);

            if (isLast) {
                head.put(T.tagLastQ, tmp);
            } else {
                boolean found = false;
                for (int i = 0; i < answers.length(); i++) {
                    if (answers.optJSONObject(i).optInt(T.tagIdQuestion) == id_question) {
                        found = true;
                        answers.optJSONObject(i).put(T.tagAnswer, answer);
                        break;
                    }
                }
                if (!found)
                    answers.put(tmp);
            }
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    private void countErrors(Button btn, int id_q, LinearLayout ll) {
        try {
            if (errorAnswers != null) {

                for (int i = 0; i < errorAnswers.length(); i++) {
                    if (errorAnswers.optJSONObject(i).optInt(T.tagIdQuestion) == id_q) {
                        JSONArray errors = errorAnswers.optJSONObject(i).optJSONArray(T.tagErrors);
                        boolean hasManual = false;
                        int countNonManual = 0;
                        if (errors.length() > 0) {

                            for (int k = 0; k < errors.length(); k++) {

                                final JSONObject error = errors.optJSONObject(k);
                                if (error.optInt(T.tagVirtual) == 1 || error.optInt(T.tagIdError) < 0) {
                                    if (!hasManual) hasManual = true;
                                }
                                if (error.optInt(T.tagIdError) > 0) {
                                    countNonManual++;
                                }
                            }
                            int counterReal = (hasManual ? 1 : 0) + countNonManual;
                            if (counterReal > 0) {
                                btn.setText(counterReal + "x ");
                                ll.setBackgroundResource(R.color.lightred);

                            }

                        } else {
                            btn.setText("");
                            ll.setBackgroundColor(Color.TRANSPARENT);
                        }
                        return;
                    }
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    private Pair<JSONArray, Integer> countGroupErrors(JSONArray errorArray, JSONObject group) {
        JSONArray tmp = new JSONArray();
        int counterReal = 0;
        try {
            if (errorArray != null && errorArray.length() > 0) {
                if (group.has(T.tagIdQuestion)) {
                    int id_que = group.optInt(T.tagIdQuestion);
                    for (int i = 0; i < errorArray.length(); i++) {
                        if (errorArray.optJSONObject(i).optInt(T.tagIdQuestion) == id_que) {
                            JSONArray errors = errorArray.optJSONObject(i).optJSONArray(T.tagErrors);
                            boolean hasManual = false;
                            int countNonManual = 0;
                            for (int k = 0; k < errors.length(); k++) {

                                final JSONObject error = errors.optJSONObject(k);
                                if (error.optInt(T.tagVirtual) == 1 || error.optInt(T.tagIdError) < 0) {
                                    if (!hasManual) hasManual = true;
                                }
                                if (error.optInt(T.tagIdError) > 0) {
                                    countNonManual++;
                                }
                                tmp.put(error);
                            }
                            counterReal = (hasManual ? 1 : 0) + countNonManual;

                            return new Pair(tmp, counterReal);
                        }
                    }
                } else {
                    JSONArray questions = group.optJSONArray(T.tagQuestions);
                    for (int z = 0; z < questions.length(); z++) {
                        int id_que = questions.optJSONObject(z).optInt(T.tagIdQuestion);
                        for (int i = 0; i < errorArray.length(); i++) {

                            if (errorArray.optJSONObject(i).optInt(T.tagIdQuestion) == id_que) {
                                JSONArray errors = errorArray.optJSONObject(i).optJSONArray(T.tagErrors);
                                boolean hasManual = false;
                                int countNonManual = 0;
                                for (int k = 0; k < errors.length(); k++) {

                                    final JSONObject error = errors.optJSONObject(k);
                                    if (error.optInt(T.tagVirtual) == 1 || error.optInt(T.tagIdError) < 0) {
                                        if (!hasManual) hasManual = true;
                                    }
                                    if (error.optInt(T.tagIdError) > 0) {
                                        countNonManual++;
                                    }
                                    tmp.put(error);
                                }
                                counterReal += (hasManual ? 1 : 0) + countNonManual;

                                //return new Pair(tmp, counterReal);
                            }
                        }
                    }
                    return new Pair(tmp, counterReal);
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
        return new Pair(tmp, counterReal);
    }


    /////////////////////////////////////////NETWORK///////////////////////////////////////////////

    private void getProtocol(JSONObject params) {
        new HTTPcomm(FormFilling.this, params, new HTTPcomm.OnFinish() {

            @Override
            public void onResult(String response) {
                if (response != null) {
                    try {

                        JSONObject tmp = new JSONObject(response);
                        answersPrefill = new JSONArray();
                        prefillFullName = tmp.optString(T.tagFullname);
                        boolean hasUnsolvedErrors = false;

                        if (tmp.has(T.tagAnswers2)) {
                            answersPrefill = new JSONObject(response).optJSONArray(T.tagAnswers2);
                        }
                        if (tmp.has(T.tagAnswers)) {
                            answersPrefill = new JSONObject(response).optJSONArray(T.tagAnswers);
                            answers = new JSONArray();
                            for (int i = 0; i < answersPrefill.length(); i++) {
                                if ((answersPrefill.optJSONObject(i).optInt("is_controller") == 1) ==
                                        isController)
                                    answers.put(new JSONObject(answersPrefill.optJSONObject(i).toString()));
                            }


                            if (tmp.has(T.tagErrors) && errorAnswers.length() == 0) {
                                JSONArray errors = tmp.optJSONArray(T.tagErrors);

                                for (int i = 0; i < errors.length(); i++) {
                                    JSONObject error = errors.optJSONObject(i);

                                    if (error.optInt("resolved_by", -1) == -1) {
                                        hasUnsolvedErrors = true;
                                    } else {
                                        //ADD solved error
                                        error = new JSONObject(error.toString());
                                        dialogVirtErr = new ErrorDialog(FormFilling.this, error.optInt(T.tagIdQuestion), errorAnswersSolved, errorCodesArraySolved);
                                        dialogVirtErr.id_pr_err = error.optInt(T.tagIdError);
                                        if (error.optInt(T.tagIdGroup) == -1) {
                                            dialogVirtErr.addError(error.optString(T.tagType).equals(T.tagPhoto) ? error.optString(T.tagAnswer) : error.optString(T.tagManual), error.optString(T.tagType).equals(T.tagPhoto),
                                                    error.optInt(T.tagIdError), error.optInt(T.tagIdError) == -2, error.optString("resolved_by", "")
                                                    , error.optString("resolved_time", ""), error.optString("id_photo_sign", ""));
                                        } else {
                                            dialogVirtErr.putAnswer(error.optInt(T.tagIdError), error.optInt(T.tagIdGroup),
                                                    error.optString(T.tagType), error.optString(T.tagAnswer), error.optString(T.tagManual), error.optInt(T.tagVirtual), error.optString("resolved_by", "")
                                                    , error.optString("resolved_time", ""), error.optString("id_photo_sign", ""));
                                        }
                                    }

                                    if (isController || error.optInt("is_virtual") == 1) {
                                        dialogVirtErr = new ErrorDialog(FormFilling.this, error.optInt(T.tagIdQuestion), errorAnswers, errorCodesArray);
                                        dialogVirtErr.id_pr_err = error.optInt(T.tagIdError);
                                        if (error.optInt(T.tagIdGroup) == -1) {
                                            dialogVirtErr.addError(error.optString(T.tagType).equals(T.tagPhoto) ? error.optString(T.tagAnswer) : error.optString(T.tagManual), error.optString(T.tagType).equals(T.tagPhoto),
                                                    error.optInt(T.tagIdError), error.optInt(T.tagIdError) == -2, error.optString("resolved_by", "")
                                                    , error.optString("resolved_time", ""), error.optString("id_photo_sign", ""));
                                        } else {
                                            dialogVirtErr.putAnswer(error.optInt(T.tagIdError), error.optInt(T.tagIdGroup),
                                                    error.optString(T.tagType), error.optString(T.tagAnswer), error.optString(T.tagManual), error.optInt(T.tagVirtual), error.optString("resolved_by", "")
                                                    , error.optString("resolved_time", ""), error.optString("id_photo_sign", ""));
                                        }
                                    }


                                }
                            }

                          /*  if (!isController && hasUnsolvedErrors && tmp.optInt("uncomplete") != 1 && !(tmp.optInt("id_protocol_solved") >0)) {

                                Functions.toast(FormFilling.this, R.string.first_finish_errors);
                                if (mWaitDialog != null)
                                    mWaitDialog.cancel();
                                isFinishing = true;
                                new FileMan(FormFilling.this, "").removeFolder("PAUSE");
                                finish();
                                return;
                            }*/
                            if (tmp.has(T.tagIdLastProto))
                                idLastProtocol = tmp.optInt(T.tagIdLastProto);
                            Functions.toast(FormFilling.this, R.string.protocol_downloaded);
                            refresh();
                            if (tmp.optInt("status") == 1) {
                                AlertDialog alertDialog = new AlertDialog.Builder(FormFilling.this).create();
                                alertDialog.setMessage(getString(R.string.paletReleased));
                                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }
                        }
                        if (mWaitDialog != null)
                            mWaitDialog.dismiss();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void sendProtocol(final Bitmap sign) {
        try {
            head.put(T.tagName, selectedPlan.optString(T.tagName));
            head.put(T.tagSerial, serialNumber);
            head.put(T.tagIdPlan, selectedPlan.optString(T.tagIdPlan));
            head.put(T.tagUncomplete, ((sign == null) ? 1 : 0));

            if (idLastProtocol != 0) head.put(T.tagIdLastProto, idLastProtocol);

            waitDialog(false);

            new HTTPcomm(FormFilling.this, head, answers, errorAnswers, new HTTPcomm.OnFinish() {
                @Override
                public void onResult(String response) {
                    mWaitDialog.dismiss();
                    if (response != null && response.length() > 0)
                        showResponseDialog(true);
                    else {
                        FileMan f = new FileMan(FormFilling.this, "to_send_" + new Date().getTime());
                        f.saveDoc("head.json", head);
                        f.saveDoc("answers.json", answers);
                        f.saveDoc("error answers.json", errorAnswers);
                        f.saveBitmap("sign.jpg", sign);
                        for (int i = 0; i < photos.size(); i++) {
                            f.saveBitmap("photo_" + photos.get(i).first + ".jpg", photos.get(i).second);
                        }
                        for (int i = 0; i < photosErr.size(); i++) {
                            f.saveBitmap("err_photo_" + photosErr.get(i).first + ".jpg", photosErr.get(i).second);
                        }
                        showResponseDialog(false);
                    }
                }
            }, photos, photosErr, sign);
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    /*private void sendResolvedErors(Bitmap bitmapSign){
        new HTTPcomm(FormFilling.this, ids, bitmapSign, new HTTPcomm.OnFinish() {
            @Override
            public void onResult(String response) {
                if (response != null) {
                    Functions.toast(FormFilling.this, R.string.success);
                }

                mWaitDialog.dismiss();

            }
        });
    }*/

    /////////////////////////////////////////DIALOGS///////////////////////////////////////////////

    private void showResponseDialog(boolean success) {
        String title, message;
        if (success) {
            title = this.getApplicationContext().getString(R.string.success);
            message = this.getApplicationContext().getString(R.string.pdfCreated);
        } else {
            title = this.getApplicationContext().getString(R.string.error);
            message = this.getApplicationContext().getString(R.string.filesSaved);
        }
        new AlertDialog.Builder(FormFilling.this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        isFinishing = true;
                        new FileMan(FormFilling.this, "").removeFolder("PAUSE");
                        FormFilling.this.finish();
                    }
                }).create().show();

    }

    private void waitDialog(boolean download) {
        String message;
        if (download) {
            message = this.getApplicationContext().getString(R.string.waitDownload);
        } else {
            message = this.getApplicationContext().getString(R.string.waitUpload);
        }
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(FormFilling.this);
        LayoutInflater inflater = FormFilling.this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_wait, null);
        TextView textView = dialogView.findViewById(R.id.message);
        textView.setText(message);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mWaitDialog != null)
                    mWaitDialog.cancel();
            }
        });
        mWaitDialog = dialogBuilder.create();
        mWaitDialog.show();
    }

    private void confirmDialog(final boolean save) {

        AlertDialog alertDialog = new AlertDialog.Builder(FormFilling.this, R.style.MyDialog).create();
        alertDialog.setTitle(FormFilling.this.getString(R.string.confirm));
        alertDialog.setMessage(save ? FormFilling.this.getString(R.string.mess_save_uncomplete) : FormFilling.this.getString(R.string.mess_block));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, FormFilling.this.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (save) {
                            sendProtocol(null);
                        } else {
                            putAnswer(-3, -3, T.tagYesNo, T.tagNaAnswer, true);
                            actualLayout = layoutCount;
                            refresh();
                        }

                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, FormFilling.this.getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        alertDialog.show();

        TextView mess = alertDialog.findViewById(android.R.id.message);
        mess.setTextSize(30);

    }

    /////////////////////////////////////////NFC /////////////////////////////////

    @Override
    protected void onNewIntent(Intent intent) {
        try {
            if (actualLayout == 0) {
                String action = intent.getAction();
                if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

                    String type = intent.getType();
                    if (T.MIME_TEXT_PLAIN.equals(type)) {

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
            Functions.err(e);
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
                if (arr.length > 1) {
                    ((AutoCompleteTextView) findViewById(R.id.autocompletetextview)).setText(arr[0]);
                    ((TextView) findViewById(R.id.editSerial)).setText(arr[1]);
                    findViewById(R.id.btnNext).performClick();
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
    }
}

