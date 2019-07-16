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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    public static final String MIME_TEXT_PLAIN = "text/plain";

    private static final String tagIdPlan="id_plan", tagIdQuestion = "id_question", tagIdGroup = "id_group",  tagPlans = "plans", tagLogin = "LOGIN", tagControler = "is_controller", tagName = "name", tagDrawDate = "draw_date",
            tagError = "ERROR", tagErrors = "errors",tagQuestionsGroups = "questions_groups", tagSerial = "serial", tagFullname = "fullname", tagAnswers = "answers", tagAnswer = "answer",
            tagLastQ = "last_question", tagQuestions = "questions", tagType = "type", tagIdError = "id_error", tagCmnError = "common_id_errors", tagYesNo = "yesno", tagPhoto = "photo",
            tagText = "text", tagNumber = "number", tagPhotoReq = "photo_required", tagPositiveAnswer = "1" , tagNegativeAnswer= "0", tagLocLat = "loc_lat", tagLocLng = "log_lng",
            tagLimitPlus = "limit_plus", tagLimitMinus = "limit_minus";


    private AlertDialog mWaitDialog;
    private ErrorDialog dialog;
    private Camera camera;

    private String serialNumber, prefillFullName;

    private LinearLayout llQuestions, ll2, llscroll,llserial2,llsign;
    private int actualLayout = 0, layoutCount;
    private String actualLang = MainActivity.language;

    private JSONObject plans, selectedPlan, head, login;
    private JSONArray answers, errorCodesArray, errorAnswers, answersPrefill;
    private JSONArray questionGroups;

    private Button btnBack, btnSend, btnNext, btnClear ;

    private SignatureView signatureView;
    private Bitmap bitmapSign, bitmap;
    private boolean doubleBackToExitPressedOnce = false, controller, btnBackClicked;

    private ArrayList<Pair<Integer, Bitmap>> photos = new ArrayList<>();
    private ArrayList<Pair<String, Bitmap>> photosErr = new ArrayList<>();
    private LocationListener mLocationListener;

    private int photo_id_q, photo_id_g, selectedPlanIndex =-1;
    private NfcAdapter mNfcAdapter;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form_filling);

        btnBack = (Button) findViewById(R.id.btnBack);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnNext = (Button) findViewById(R.id.btnNext);
        btnClear = (Button) findViewById(R.id.btnClear);

        llscroll = (LinearLayout) findViewById(R.id.llscroll);
        llserial2 = (LinearLayout) findViewById(R.id.llserial2);
        llsign = (LinearLayout) findViewById(R.id.llsign);

        final EditText textViewSerial = (EditText) findViewById(R.id.editSerial);

        btnBack.setVisibility(View.INVISIBLE);

        head = new JSONObject();
        answers = new JSONArray();
        login = new JSONObject();


        try {
            FileMan fileMan = new FileMan(FormFilling.this, "");
            String data = fileMan.getDoc("questions.json");
            plans = new JSONObject(data);

        } catch (JSONException e) {
            Functions.toast(this, R.string.msg_error_json);
            finish();
            return;
        }

        JSONObject login = plans.optJSONObject(tagLogin);
        controller = login.optBoolean(tagControler);

        //geting Types
        ArrayList<String> paletTypes = new ArrayList<>();
        JSONArray tmp = plans.optJSONArray(tagPlans);
        for (int i = 0; i < tmp.length(); i++) {
            paletTypes.add(tmp.optJSONObject(i).optString(tagName)+" ("+tmp.optJSONObject(i).optString(tagDrawDate)+")");
        }

        //geting Error Codes and Array for error answers

        errorAnswers = new JSONArray();
        try {

            errorCodesArray = new JSONArray();
            errorCodesArray = plans.optJSONArray(tagError);

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
                selectedPlanIndex = position;
                JSONArray tmp = plans.optJSONArray(tagPlans);
                textVievType.setText(tmp.optJSONObject(position).optString(tagName));
            }
        });

        llQuestions = (LinearLayout) findViewById(R.id.questions);
        signatureView = (SignatureView) findViewById(R.id.signature_view);

        //    Button btnNext = (Button) findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    btnBackClicked = false;
                    boolean filled = true;

                    try {
                        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                                getWindow().getDecorView().getRootView().getWindowToken(), 0);
                    } catch (Exception e) {

                    }
                    if (actualLayout == 0) {
                        String planName = textVievType.getText().toString();
                        serialNumber = textViewSerial.getText().toString();
                        filled = planName.length() > 0 && serialNumber.length() > 0;
                        if (filled) {
                            if (selectedPlan != null) {
                                //reset
                                answers = new JSONArray();
                                photos = new ArrayList();
                                photosErr = new ArrayList<>();
                                answersPrefill = new JSONArray();
                            }

                            JSONArray tmp = plans.optJSONArray(tagPlans);
                            if(selectedPlanIndex > -1){
                                selectedPlan = tmp.optJSONObject(selectedPlanIndex);
                            }else {
                                for (int i = tmp.length()-1; i >-1; i--) {
                                    if(tmp.optJSONObject(i).optString(tagName).equals(planName)){
                                        selectedPlan = tmp.optJSONObject(i);
                                        break;
                                    }
                                }
                            }

                            if (selectedPlan != null) {
                                questionGroups = selectedPlan.optJSONArray(tagQuestionsGroups);
                                actualLayout = 0;
                                layoutCount = questionGroups.length() + 2;

                                if (controller) {


                                    JSONObject params = new JSONObject();
                                    params.put(tagName, planName);
                                    params.put(tagSerial, serialNumber);
                                    if(selectedPlanIndex > -1){;
                                        params.put(tagIdPlan, tmp.optJSONObject(selectedPlanIndex).optInt(tagIdPlan,-1));
                                    }
                                    waitDialog(true);

                                    new HTTPcomm(FormFilling.this, params, new HTTPcomm.OnFinish() {

                                        @Override
                                        public void onResult(String response) {
                                            if (response != null) {
                                                try {
                                                    JSONObject tmp = new JSONObject(response);
                                                    answersPrefill = new JSONArray();
                                                    prefillFullName = tmp.optString(tagFullname);
                                                    if (tmp.has(tagAnswers)) {
                                                        answersPrefill = tmp.optJSONArray(tagAnswers);
                                                        refresh();
                                                        Functions.toast(FormFilling.this, R.string.protocol_downloaded);
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
                            } else {
                                Toast.makeText(FormFilling.this, R.string.noScenary, Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else if (actualLayout == layoutCount - 1) {
                        filled = head.has(tagLastQ);
                    } else {
                        filled = isPageFilled(questionGroups.getJSONObject(actualLayout - 1));
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


        //    Button btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                btnBackClicked = true;
                if (actualLayout > 0) actualLayout--;
                refresh();
            }
        });


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    bitmapSign = signatureView.getSignatureBitmap();
                    if (bitmapSign == null) {

                        Toast.makeText(FormFilling.this, R.string.fillAll, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    head.put(tagName, selectedPlan.optString(tagName));
                    head.put(tagSerial, serialNumber);
                    head.put(tagIdPlan, selectedPlan.optString(tagIdPlan));
                    Log.e(tagAnswers, answers.toString());

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
                                f.saveBitmap("sign.jpg", bitmapSign);
                                for (int i = 0; i < photos.size(); i++) {
                                    f.saveBitmap("photo_" + photos.get(i).first + ".jpg", photos.get(i).second);
                                }
                                for (int i = 0; i < photosErr.size(); i++) {
                                    f.saveBitmap("err_photo_" + photosErr.get(i).first + ".jpg", photosErr.get(i).second);
                                }
                                showResponseDialog(false);
                            }
                        }
                    }, photos, photosErr, bitmapSign);

                } catch (Exception e) {
                    Functions.err(e);
                }
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
                        head.put(tagLocLat, location.getLatitude());
                        head.put(tagLocLng, location.getLongitude());
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
                head.put(tagLocLat, last.getLatitude());
                head.put(tagLocLng, last.getLongitude());
            } catch (Exception e) {
                Functions.err(e);
            }
        }
    }


    public void refresh() {
        try {
            llQuestions.removeAllViews();

            btnBack.setVisibility(actualLayout > 0 ? View.VISIBLE : View.INVISIBLE);
            btnSend.setVisibility(actualLayout == layoutCount ? View.VISIBLE : View.GONE);
            btnNext.setVisibility(actualLayout != layoutCount ? View.VISIBLE : View.GONE);

            if (actualLayout > 0 && actualLayout < questionGroups.length() + 1) {
                llscroll.setVisibility(View.VISIBLE);

                JSONObject group = questionGroups.optJSONObject(actualLayout - 1);

                displayGroup(group);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ScrollView scrollView =
                                ((ScrollView) findViewById(R.id.ScrollView));
                        if (scrollView != null) scrollView.smoothScrollTo(0, 0);
                    }
                }, 100);
            } else if (actualLayout == layoutCount - 1) {
                if (controller) {
                    llscroll.setVisibility(View.VISIBLE);

                    JSONObject last = selectedPlan.optJSONObject(tagLastQ);

                    displayQuestion(last, true);
                } else {
                    if (btnBackClicked) {
                        actualLayout--;
                        refresh();
                    } else {
                        actualLayout++;
                        refresh();
                    }
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


    public void displayGroup(JSONObject group) {
        if (group.has(tagQuestions)) {
            LinearLayout ll = (LinearLayout) getLayoutInflater().inflate(R.layout.item_head, llQuestions, false);
            ((TextView) ll.findViewById(R.id.text)).setText(group.optString(actualLang, ""));
            llQuestions.addView(ll);

            JSONArray questions = group.optJSONArray(tagQuestions);
            for (int i = 0; i < questions.length(); i++) {

                displayGroup(questions.optJSONObject(i));
            }
        } else {
            displayQuestion(group, false);
        }
    }

    public void displayQuestion(final JSONObject q, final boolean isLast) {

        final int q_id = q.optInt(tagIdQuestion, -1);
        final int g_id = q.optInt(tagIdGroup, -1);
        final String type = q.optString(tagType, "");
        final int err_id = q.optInt(tagIdError, 0);
        final String commonErrors = q.optString(tagCmnError);

        ll2 = null;
        switch (type){
            case tagYesNo:
                ll2 = (LinearLayout) getLayoutInflater().inflate(R.layout.item_question, llQuestions, false);
                break;
            case tagPhoto:
                 ll2 = (LinearLayout) getLayoutInflater().inflate(R.layout.item_photo, llQuestions, false);
                // nefunguje, je třeba stejného kódu - řádek 559,560
                break;

        }
        if (type.equals(tagText) || (type.equals(tagNumber))){
            ll2 = (LinearLayout) getLayoutInflater().inflate(R.layout.item_question_input, llQuestions, false);
        }
        if (type.equals(tagText) || (type.equals(tagNumber)) || (type.equals(tagYesNo))){
            ll2.findViewById(R.id.btn_error).setVisibility(controller ? View.VISIBLE : View.GONE);
            final ImageButton btnErr = ll2.findViewById(R.id.btn_error);
            btnErr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog = new ErrorDialog();
                    dialog.showDialog(FormFilling.this, q_id, commonErrors, errorCodesArray, errorAnswers, photosErr);
                }
            });
        }

        ((TextView) ll2.findViewById(R.id.text)).setText(q.optString(actualLang, ""));

        ll2.findViewById(R.id.textPrefill).setVisibility(controller ? View.VISIBLE : View.GONE);

        if (controller) {
            prefillQuestion((TextView) ll2.findViewById(R.id.textPrefill), q_id);
            countErrors((TextView) ll2.findViewById(R.id.textErrCount), q_id,ll2);
        }



        if (type.equals(tagYesNo)) {

            llQuestions.addView(ll2);

            final RadioGroup rg = (RadioGroup) ll2.findViewById(R.id.radioGroup);
            String answer;
            if (isLast) {
                ll2.findViewById(R.id.btn_error).setVisibility(View.GONE);
                answer = getLastAnswer();
            } else answer = getAnswer(q_id);

            if (answer.equals(tagPositiveAnswer)) {
                rg.check(R.id.rgYes);
            } else if (answer.equals(tagNegativeAnswer)) {
                rg.check(R.id.rgNo);
            }
            rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    try {
                        switch (checkedId) {
                            case R.id.rgNo:
                                putAnswer(q_id, g_id, type, tagNegativeAnswer, isLast);
                                break;
                            case R.id.rgYes:
                                if (isLast) {
                                    if (checkErrors()) {
                                        putAnswer(q_id, g_id, type, tagPositiveAnswer, isLast);
                                    } else {
                                        rg.clearCheck();
                                        Functions.toast(FormFilling.this, R.string.product_have_error);
                                    }

                                } else {
                                    putAnswer(q_id, g_id, type, tagPositiveAnswer, isLast);
                                }

                                break;
                        }
                    } catch (Exception e) {
                        Functions.err(e);
                    }
                }
            });

        }
        if (type.equals(tagPhoto)) {
            if (controller) {
                ll2 = (LinearLayout) getLayoutInflater().inflate(R.layout.item_photo, llQuestions, false);
                ((TextView) ll2.findViewById(R.id.text)).setText(q.optString(actualLang, ""));
                llQuestions.addView(ll2);

                Button makePhoto = (Button) findViewById(R.id.btn_make_photo);
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


                for (int i = 0; i < photos.size(); i++) {
                    if (photos.get(i).first == q_id) {
                        ImageView cameraPhoto = (ImageView) ll2.findViewById(R.id.camera_image);
                        cameraPhoto.setImageBitmap(photos.get(i).second);
                        break;
                    }
                }
            } else {
                if (btnBackClicked) {
                    actualLayout--;
                    refresh();
                } else {
                    actualLayout++;
                    refresh();
                }
            }

        }
        if (type.equals(tagNumber) || (type.equals(tagText))) {
            llQuestions.addView(ll2);
            final MyEditText editAnswer = (MyEditText) ll2.findViewById(R.id.editAnswer);

            if (type.equals(tagNumber))
                editAnswer.setInputType(InputType.TYPE_CLASS_NUMBER);
            else
                editAnswer.setInputType(InputType.TYPE_CLASS_TEXT);

            editAnswer.setText(getAnswer(q_id));
            editAnswer.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    int limitPlus = 0;
                    int limitMinus = 0;

                    try {
                        if (type.equals(tagNumber)){
                            if (q.has(tagLimitPlus)) {
                                limitPlus = q.getInt(tagLimitPlus);
                            }
                            if (q.has(tagLimitMinus)) {
                                limitMinus = q.getInt(tagLimitMinus);
                            }

                            int newS = Integer.parseInt(s.toString());

                            if ((newS > limitPlus) || (newS < limitMinus)) {
                                ((TextView) ll2.findViewById(R.id.text)).setTextColor(getResources().getColor(R.color.red, getResources().newTheme()));
                                editAnswer.setTextColor(getResources().getColor(R.color.red, getResources().newTheme()));
                            } else {
                                ((TextView) ll2.findViewById(R.id.text)).setTextColor(getResources().getColor(R.color.black, getResources().newTheme()));
                                editAnswer.setTextColor(getResources().getColor(R.color.black, getResources().newTheme()));
                            }
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

    }

    private void prefillQuestion(TextView text, int id_q) {
        try {
            if(answersPrefill != null){
                for (int i = 0; i < answersPrefill.length(); i++) {
                    if (answersPrefill.optJSONObject(i).optInt(tagIdQuestion) == id_q) {

                        String answer = answersPrefill.optJSONObject(i).optString(tagAnswer);
                        if (answersPrefill.optJSONObject(i).optString(tagType).equals(tagYesNo)) {
                            answer = answer.equals(tagPositiveAnswer) ? getString(R.string.yes) : getString(R.string.no);
                        }
                        if (answer.length() > 0) {
                            text.setVisibility(View.VISIBLE);
                            text.setText(prefillFullName + " " + getString(R.string.filled) + ": " + answer);
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

    private void countErrors(TextView text, int id_q, LinearLayout ll) {
        try {
            if(errorAnswers != null){

                for (int i = 0; i < errorAnswers.length(); i++) {
                    if (errorAnswers.optJSONObject(i).optInt(tagIdQuestion) == id_q) {
                        if (errorAnswers.optJSONObject(i).optJSONArray(tagErrors).length()>0) {
                            text.setText(errorAnswers.optJSONObject(i).optJSONArray(tagErrors).length()+"x");
                            ll.setBackgroundResource(R.color.lightred);
                        }else{
                            text.setText("");
                            ll.setBackgroundColor(Color.TRANSPARENT);
                        }

                        return;
                    }
                }
            }
            text.setVisibility(View.GONE);
        } catch (Exception e) {
            Functions.err(e);
        }
    }
    private boolean isPageFilled(JSONObject group) {
        boolean result = true;
        try {
            if (group.has(tagQuestions)) {
                JSONArray tmp = group.optJSONArray(tagQuestions);
                for (int i = 0; i < tmp.length(); i++) {
                    result &= isPageFilled(tmp.optJSONObject(i));
                }
            } else {
                if (group.optString(tagType).equals(tagPhoto)) {
                    Log.e("aa", getAnswer(group.optInt(tagIdQuestion, -1)));
                    result = getAnswer(group.optInt(tagIdQuestion, -1)).length() > 0
                            || group.optInt(tagPhotoReq, -1) == 0;
                } else {
                    result = getAnswer(group.optInt(tagIdQuestion, -1)).length() > 0;
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
                if (answers.optJSONObject(i).optInt(tagIdQuestion) == id_q)
                    return answers.optJSONObject(i).optString(tagAnswer);
            }
        } catch (Exception e) {
            Functions.err(e);
        }
        return "";
    }

    private String getLastAnswer() {
        try {
            JSONObject tmp = head.optJSONObject(tagLastQ);
            if (tmp != null)
                return tmp.optString(tagAnswer);

        } catch (Exception e) {
            Functions.err(e);
        }
        return "";
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
                    putAnswer(photo_id_q, photo_id_g, tagPhoto, "photo_" + photo_id_q, false);
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
                    }
                } catch (Exception e) {
                    Functions.err(e);
                }
            }
        }
    }


    private void putAnswer(int id_question, int id_group, String type, String answer, boolean isLast) {
        try {
            JSONObject tmp = new JSONObject();
            tmp.put(tagIdQuestion, id_question);
            if (id_group != -1)
                tmp.put(tagIdGroup, id_group);
            tmp.put(tagType, type);
            tmp.put(tagAnswer, answer);

            if (isLast) {
                head.put(tagLastQ, tmp);
            } else {
                boolean found = false;
                for (int i = 0; i < answers.length(); i++) {
                    if (answers.optJSONObject(i).optInt(tagIdQuestion) == id_question) {
                        found = true;
                        answers.put(i, tmp);
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

    private void showResponseDialog(boolean success) {
        String title,message;
        if (success) {
            title = this.getApplicationContext().getString(R.string.success);
            message = this.getApplicationContext().getString(R.string.pdfCreated);
        }else {
            title = this.getApplicationContext().getString(R.string.error);
            message = this.getApplicationContext().getString(R.string.filesSaved);
        }
        new AlertDialog.Builder(FormFilling.this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        FormFilling.this.finish();
                    }
                }).create().show();

    }

    private void waitDialog(boolean download){
        String message;
        if (download) {
            message = this.getApplicationContext().getString(R.string.waitDownload);
        }else {
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


    private boolean checkErrors() {
        JSONArray errArray;
        try {
            for (int i = 0; i < errorAnswers.length(); i++) {

                errArray = errorAnswers.optJSONObject(i).optJSONArray(tagErrors);

                for (int x = 0; x < errArray.length(); x++) {

                    errArray.optJSONObject(x);
                    int answer = errArray.optJSONObject(x).optInt(tagAnswer);

                    if (answer!=0) return false;
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
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
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    @Override
    protected void onPause() {
        Functions.stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        try {
            if (actualLayout == 0) {
                String action = intent.getAction();
                if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

                    String type = intent.getType();
                    if (MIME_TEXT_PLAIN.equals(type)) {

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
                    ((Button) findViewById(R.id.btnNext)).performClick();
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
    }
}

