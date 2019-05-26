package com.carpoint.boxscanner.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.carpoint.boxscanner.MainActivity;
import com.carpoint.boxscanner.R;
import com.mindorks.paracamera.Camera;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;

public class ErrorDialog {

    public Camera camera;
    public int photoIdQuestion, photoIdError;
    private JSONArray errorCodes, errorAnswers;
    private String commonErr[];
    private Dialog dialog;
    private LinearLayout llErrors;
    private Activity mActivity;
    private String actualLang = MainActivity.language, common_id_errors, searchString;
    private int newErrG, newErrId, newErrPos, q_id;
    private ArrayList<Pair<String, Bitmap>> photosErr;

    public ErrorDialog() {
    }

    public void showDialog(final Activity activity, int question_id, String common_errors, JSONArray passedErrors,
                           JSONArray error_answers, ArrayList<Pair<String, Bitmap>> errorPhotos) {
        try {
            mActivity = activity;
            q_id = question_id;

            common_id_errors = common_errors;
            errorAnswers = error_answers;
            dialog = new Dialog(activity);
            errorCodes = new JSONArray();
            errorCodes = passedErrors;
            photosErr = errorPhotos;
            searchString = "";
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.dilalog_errors);

            Button btnOK = dialog.findViewById(R.id.btnOK);
            Button btnAdError = dialog.findViewById(R.id.btnAddError);
            Button btnAdPhoto = dialog.findViewById(R.id.btnAddErrorPhoto);
            llErrors = dialog.findViewById(R.id.erros);


            WindowManager.LayoutParams lWindowParams = new WindowManager.LayoutParams();
            lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            lWindowParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setAttributes(lWindowParams);


            getScrollHandler();
            refreshErros();

            btnOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    ((FormFilling)mActivity).refresh();
                }
            });
            btnAdError.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    addNewErrorDialog(activity);

                }
            });

            btnAdPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        int errID =(int) Math.round(Math.random()*Integer.MIN_VALUE);
                        photoIdQuestion = q_id;
                        photoIdError = errID;
                        camera = new Camera.Builder()
                                .resetToCorrectOrientation(true)
                                .setTakePhotoRequestCode(2)
                                .setDirectory("BoxScannerPics")
                                .setName("Error photo_" + System.currentTimeMillis())
                                .setImageFormat(Camera.IMAGE_JPEG)
                                .setCompression(75)
                                .build(mActivity);
                        camera.takePicture();


                        putAnswer(errID, -1, "photo", "err_photo_" + q_id + "_" + errID, null);
                    }catch (Exception e){
                        Functions.err(e);
                    }
                }
            });

            ((EditText)dialog.findViewById(R.id.editSearch)).addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchString = s.toString();
                    refreshErros();
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            dialog.show();
        } catch (Exception e) {
            Functions.err(e);
        }

    }

    private void refreshErros() {
        try {
            llErrors.removeAllViews();


            if (common_id_errors.length() > 0 && searchString.length() == 0) {
                LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_head, llErrors, false);

                llErrors.addView(ll);
                ((TextView) ll.findViewById(R.id.text)).setText(R.string.common_errors);
                for (int i = 0; i < errorCodes.length(); i++) {
                    JSONObject obj = errorCodes.optJSONObject(i);

                    JSONArray errors = obj.optJSONArray("errors");

                    for (int x = 0; x < errors.length(); x++) {
                        displayCommonError(errors.optJSONObject(x));
                    }

                }
            }


            for (int i = 0; i < errorCodes.length(); i++) {
                JSONObject obj = errorCodes.optJSONObject(i);
                Log.e("obj", obj.toString());

                if(searchString.length() > 0 && !searchString.toUpperCase().contains(obj.optString("code", "").toUpperCase())){
                    continue;
                }

                JSONArray errors = obj.optJSONArray("errors");
                LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_head, llErrors, false);
                llErrors.addView(ll);

                ((TextView) ll.findViewById(R.id.text)).setText(obj.optString("code", "")+"-"+obj.optString(actualLang, ""));

                for (int x = 0; x < errors.length(); x++) {
                    displayError(errors.optJSONObject(x));
                }

            }


        } catch (Exception e) {
            Functions.err(e);
        }
    }

    private void displayError(JSONObject group) {
        newErrId = group.optInt("id_error", -1);
        newErrG = group.optInt("id_group", -1);

        final String type = group.optString("type", "");
        final String photoRequired = group.optString("photo_required");
        Log.e("type: ", type);

        if(searchString.length() > 1 && !searchString.contains(group.optString("position", ""))){
            return;
        }

        if (photoRequired.equals("0")) {
            final int errID = group.optInt("id_error", -1);
            final int errG = group.optInt("id_group", -1);

            LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error, llErrors, false);
            ((TextView) ll.findViewById(R.id.text)).setText(group.optString("position", "")+") "+group.optString(actualLang, ""));
            llErrors.addView(ll);

            CheckBox chk = (CheckBox) ll.findViewById(R.id.checkbox);

            String answer = getAnswer(errID);

            if (answer.equals("1")) {
                chk.setChecked(true);
            } else if (answer.equals("0")) {
                chk.setChecked(false);
            }

            chk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (((CheckBox) v).isChecked()) {
                        putAnswer(errID, errG, "yesno", "1", null);
                    } else {
                        putAnswer(errID, errG, "yesno", "0", null);
                    }

                }
            });
        }
        if (photoRequired.equals("1")) {

            final int errID = group.optInt("id_error", -1);
            final int errG = group.optInt("id_group", -1);

            LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error_photo, llErrors, false);
            ((TextView) ll.findViewById(R.id.text)).setText(group.optString("position", "")+") "+group.optString(actualLang, ""));
            llErrors.addView(ll);

            final CheckBox chk = (CheckBox) ll.findViewById(R.id.checkbox);
            ImageButton btnMakePhoto = ll.findViewById(R.id.btn_make_photo);

            btnMakePhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        photoIdError = errID;
                        photoIdQuestion = q_id;
                        camera = new Camera.Builder()
                                .resetToCorrectOrientation(true)
                                .setTakePhotoRequestCode(2)
                                .setDirectory("BoxScannerPics")
                                .setName("Error photo_" + System.currentTimeMillis())
                                .setImageFormat(Camera.IMAGE_JPEG)
                                .setCompression(75)
                                .build(mActivity);
                        camera.takePicture();

                        chk.setChecked(true);
                        putAnswer(errID, errG, "photo", "err_photo_" + q_id + "_" + errID, null);
                    } catch (Exception e) {
                        Functions.err(e);
                    }
                }
            });


            String answer = getAnswer(errID);

            if (answer.equals("1")) {
                chk.setChecked(true);
            } else if (answer.equals("0")) {
                chk.setChecked(false);
            }

            chk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!((CheckBox) v).isChecked()) {
                        putAnswer(errID, errG, type, "0", null);
                        String photoName = q_id + "_" + errID;
                        removeErrorPhoto(photoName);
                    }

                }
            });

        }

    }

    private void displayCommonError(JSONObject group) {
        newErrId = group.optInt("id_error", -1);
        newErrG = group.optInt("id_group", -1);
        if (common_id_errors.contains(group.optString("id_error"))) {
            final String type = group.optString("type", "");
            final String photoRequired = group.optString("photo_required");
            Log.e("type: ", type);
            if (photoRequired.equals("0")) {
                final int errID = group.optInt("id_error", -1);
                final int errG = group.optInt("id_group", -1);

                LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error, llErrors, false);
                ((TextView) ll.findViewById(R.id.text)).setText(group.optString(actualLang, ""));
                llErrors.addView(ll);

                CheckBox chk = (CheckBox) ll.findViewById(R.id.checkbox);

                String answer = getAnswer(errID);

                if (answer.equals("1")) {
                    chk.setChecked(true);
                } else if (answer.equals("0")) {
                    chk.setChecked(false);
                }

                chk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (((CheckBox) v).isChecked()) {
                            putAnswer(errID, errG, "yesno", "1", null);
                        } else {
                            putAnswer(errID, errG, "yesno", "0", null);
                        }

                    }
                });
            }
            if (photoRequired.equals("1")) {

                final int errID = group.optInt("id_error", -1);
                final int errG = group.optInt("id_group", -1);

                LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error_photo, llErrors, false);
                ((TextView) ll.findViewById(R.id.text)).setText(group.optString(actualLang, ""));
                llErrors.addView(ll);

                final CheckBox chk = (CheckBox) ll.findViewById(R.id.checkbox);
                ImageButton btnMakePhoto = ll.findViewById(R.id.btn_make_photo);

                btnMakePhoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            photoIdError = errID;
                            photoIdQuestion = q_id;
                            camera = new Camera.Builder()
                                    .resetToCorrectOrientation(true)
                                    .setTakePhotoRequestCode(2)
                                    .setDirectory("BoxScannerPics")
                                    .setName("err_photo_" + System.currentTimeMillis())
                                    .setImageFormat(Camera.IMAGE_JPEG)
                                    .setCompression(75)
                                    .build(mActivity);
                            camera.takePicture();

                            chk.setChecked(true);
                            putAnswer(errID, errG, "photo", "err_photo_" + q_id + "_" + errID, null);
                        } catch (Exception e) {
                            Functions.err(e);
                        }
                    }
                });


                String answer = getAnswer(errID);

                if (answer.equals("1")) {
                    chk.setChecked(true);
                } else if (answer.equals("0")) {
                    chk.setChecked(false);
                }

                chk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!((CheckBox) v).isChecked()) {
                            putAnswer(errID, errG, type, "0", null);
                            String photoName = q_id + "_" + errID;
                            removeErrorPhoto(photoName);
                        }

                    }
                });

            }
        }
    }

    private String getAnswer(int id_err) {
        JSONArray errArray = new JSONArray();
        try {
            for (int i = 0; i < errorAnswers.length(); i++) {
                if (errorAnswers.optJSONObject(i).optInt("id_question") == q_id)
                    errArray = errorAnswers.optJSONObject(i).optJSONArray("errors");
                for (int x = 0; x < errArray.length(); x++) {
                    errArray.optJSONObject(x);
                    if ((errArray.optJSONObject(x).optInt("id_error") == id_err)) {
                        String answer = errArray.optJSONObject(x).optString("answer");
                        return answer;

                    }
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
        return "";
    }

    private void putAnswer(int id_error, int id_group, String type, String answer, String manual) {
        try {
            JSONObject tmp = new JSONObject();
            tmp.put("id_error", id_error);
            if (id_group != -1)
                tmp.put("id_group", id_group);
            tmp.put("type", type);
            tmp.put("answer", answer);
            if(manual != null){
                tmp.put("manual_text", manual);
            }

            JSONObject err = new JSONObject();
            err.put("id_question", q_id);

            JSONArray errorsArray = new JSONArray();
            errorsArray.put(tmp);
            err.put("errors", errorsArray);


            boolean Qfound = false;
            boolean errFound = false;
            for (int i = 0; i < errorAnswers.length(); i++) {

                if (errorAnswers.optJSONObject(i).optInt("id_question") == q_id) {
                    Qfound = true;
                    for (int x = 0; x < errorAnswers.optJSONObject(i).optJSONArray("errors").length(); x++) {

                        if ((errorAnswers.optJSONObject(i).optJSONArray("errors").optJSONObject(x).optInt("id_error")) == id_error) {
                            errFound = true;
                            errorAnswers.optJSONObject(i).optJSONArray("errors").put(x, tmp);

                        }
                    }
                    if (!errFound) errorAnswers.optJSONObject(i).optJSONArray("errors").put(tmp);

                }
            }
            if (!Qfound)
                errorAnswers.put(err);
            Log.e("Errors Answers", errorAnswers.toString());

        } catch (Exception e) {
            Functions.err(e);
        }
    }

    private void removeErrorPhoto(String name) {
        for (int i = 0; i < photosErr.size(); i++) {
            if (photosErr.get(i).first.equals(name)) {
                photosErr.remove(i);
                Log.e("Deleted photo", photosErr.toString());
            }

        }
    }

    private void getScrollHandler() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ScrollView scrollView =
                        ((ScrollView) dialog.findViewById(R.id.ScrollView));
                if (scrollView != null) scrollView.smoothScrollTo(0, 0);
            }
        }, 100);
    }

    public void addNewErrorDialog(final Activity activity) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.add_new_error_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle(activity.getString(R.string.btn_add_error));
        final EditText edt = (EditText) dialogView.findViewById(R.id.edit1);
        dialogBuilder.setPositiveButton(activity.getString(R.string.add_error), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String newErrorText = edt.getText().toString();
                try {
                    final String type = "yesno";
                    boolean anotherErrorsFound = false;
                    for (int i = 0; i < errorCodes.length(); i++) {
                        JSONObject obj = errorCodes.optJSONObject(i);

                        if (obj.optString(actualLang).equals(activity.getString(R.string.another_errors))) {
                            newErrPos++;
                            int id_group = newErrG;
                            int id_error = newErrId + 1;
                            final int position = newErrPos;
                            anotherErrorsFound = true;

                            JSONArray errorsArray = obj.optJSONArray("errors");
                            final JSONObject newErrorObj = new JSONObject();

                            newErrorObj.put("id_group", id_group);
                            newErrorObj.put("id_error", id_error);
                            newErrorObj.put("position", position);
                            newErrorObj.put((MainActivity.language), newErrorText);
                            newErrorObj.put("type", type);

                            newErrorObj.put("photo_required", "0");
                            putAnswer(id_error, id_group, type, "1", newErrorText);

                            errorsArray.put(newErrorObj);

                        }
                    }
                    if (!anotherErrorsFound) {
                        newErrPos++;
                        int id_group = newErrG + 1;
                        int id_error = newErrId + 1;
                        int position = newErrPos;

                        JSONObject tmp = new JSONObject();

                        tmp.put("id_group", id_group);
                        tmp.put((MainActivity.language), activity.getString(R.string.another_errors));
                        tmp.put("position", 2);  //podle vzoru chyb vždy 2? čudný

                        final JSONObject newError = new JSONObject();

                        newError.put("id_group", id_group);
                        newError.put("id_error", id_error);
                        newError.put("position", position);
                        newError.put((MainActivity.language), newErrorText);
                        newError.put("type", type);

                        newError.put("photo_required", "0");
                        putAnswer(id_error, id_group, type, "1", newErrorText);

                        JSONArray newErrorsArray = new JSONArray();
                        newErrorsArray.put(newError);
                        tmp.put("errors", newErrorsArray);

                        errorCodes.put(tmp);

                        putAnswer(id_error, id_group, type, "1", newErrorText);
                    }

                    Log.e("new Error Codes", errorCodes.toString());
                    refreshErros();


                } catch (JSONException e) {
                    Functions.err(e);
                }

            }
        });
        dialogBuilder.setNegativeButton((activity.getString(R.string.cancel)), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    public Camera getCamera() {

        return camera;
    }


}