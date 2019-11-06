package com.carpoint.boxscanner.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.carpoint.boxscanner.MainActivity;
import com.carpoint.boxscanner.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kyanogen.signatureview.SignatureView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;

public class RepairErrorDialog {
    private static final String tagErrorList = "errorList", tagChecked = "checked", tagPrErrID = "id_pr_err", tagIdProtocol = "id_protocol",
            tagNull = "null";


    private Dialog dialog;
    private LinearLayout llErrors, llError;
    private Activity mActivity;
    private String actualLang = MainActivity.language;
    private JSONArray errors, questions, errorCodes, originalErrors, errorCodesArraySolved, errorAnswersSolved;
    private JSONObject question;
    private boolean showSign;
    private Bitmap bitmapSign;
    private SignatureView signatureView;
    private LinearLayout.LayoutParams buttonParams;

    public RepairErrorDialog(final Activity activity, JSONArray errorCodes, JSONArray errorAnswers, JSONArray errorCodesArraySolved, JSONArray errorAnswersSolved, JSONArray questions) {
        try {

            mActivity = activity;
            String temp = errorAnswers.toString();
            this.originalErrors = new JSONArray(temp);
            this.errors = errorAnswers;
            this.errorAnswersSolved = errorAnswersSolved;
            this.errorCodesArraySolved = errorCodesArraySolved;
            this.questions = questions;
            this.errorCodes = errorCodes;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public RepairErrorDialog(final Activity activity, JSONArray errorCodes, JSONArray errorAnswers, JSONArray errorCodesArraySolved, JSONArray errorAnswersSolved, JSONObject question) {
        try {

            mActivity = activity;
            String temp = errorAnswers.toString();
            this.originalErrors = new JSONArray(temp);
            this.errors = errorAnswers;
            this.errorAnswersSolved = errorAnswersSolved;
            this.errorCodesArraySolved = errorCodesArraySolved;
            this.question = question;
            this.errorCodes = errorCodes;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void showDialog() {
        try {

            dialog = new Dialog(mActivity);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.dialog_repair);

            Button btnSave = dialog.findViewById(R.id.btn_save);
            Button btnBack = dialog.findViewById(R.id.btn_back);

            llErrors = dialog.findViewById(R.id.errors);
            signatureView = (SignatureView) dialog.findViewById(R.id.signature_view);

            WindowManager.LayoutParams lWindowParams = new WindowManager.LayoutParams();
            lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            lWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lWindowParams);


            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        String temp = errors.toString();
                        originalErrors = new JSONArray(temp);
                        ((FormFilling) mActivity).refresh();
                        dialog.dismiss();
                    } catch (Exception e) {
                        Functions.err(e);
                    }
                }
            });

            //Zdrznnutí virtual manual

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInf) {

                    refreshErrors();

                    ScrollView scrollView =
                            ((ScrollView) dialog.findViewById(R.id.ScrollView));
                    if (scrollView != null) scrollView.smoothScrollTo(0, 0);
                }
            });
            dialog.show();
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    private void refreshErrors() {
        llErrors.removeAllViews();
        if (questions != null) {
            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.optJSONObject(i);
                int q_id = question.optInt(FormFilling.tagIdQuestion);
                for (int z = 0; z < errors.length(); z++) {
                    JSONObject error = errors.optJSONObject(z);
                    if (error.optInt(FormFilling.tagIdQuestion) == q_id) {
                        showError(error.optJSONArray(FormFilling.tagErrors));
                    }
                }
            }
        } else {
            int q_id = question.optInt(FormFilling.tagIdQuestion);
            for (int z = 0; z < errors.length(); z++) {
                JSONObject error = errors.optJSONObject(z);
                if (error.optInt(FormFilling.tagIdQuestion) == q_id) {
                    showError(error.optJSONArray(FormFilling.tagErrors));
                }
            }
        }
    }

    private JSONObject checkGroup(final JSONArray errors) {
        JSONObject obj = new JSONObject();
        JSONArray arr = new JSONArray();
        JSONArray others = new JSONArray();
        try {
            for (int i = 0; i < errors.length(); i++) {
                final JSONObject error = errors.optJSONObject(i);
                if (error.optInt(FormFilling.tagVirtual) == 1 && error.optInt("resolved_by", 0) <= 0 && !obj.has("virtual_text")  && error.optInt("resolved_by", 0) != -2) {
                    obj.put("virtual_text", error.optString(FormFilling.tagManual));
                    arr.put(error.optInt(FormFilling.tagIdError));
                }
                if (error.optInt(FormFilling.tagVirtual) == 0 && !error.optString(FormFilling.tagManual, "").equals("null") && !error.optString(FormFilling.tagType).equals(FormFilling.tagPhoto) && error.optInt(FormFilling.tagIdError, -1) < 0 && error.optInt("resolved_by", 0) <= 0  && error.optInt("resolved_by", 0) != -2/*&& !obj.has("manual_text")*/) {
                    if (obj.has("manual_text"))
                        obj.put("manual_text", obj.optString("manual_text", "") + " - " + error.optString(FormFilling.tagManual));
                    else obj.put("manual_text", error.optString(FormFilling.tagManual));
                    arr.put(error.optInt(FormFilling.tagIdError));
                }
                if (error.optString(FormFilling.tagType).equals(FormFilling.tagPhoto) && error.optInt("resolved_by", 0) <= 0  && error.optInt("resolved_by", 0) != -2) {
                    if (obj.has("photo_text"))
                        obj.put("photo_text", obj.optString("photo_text", "") + " - " + error.optString(FormFilling.tagAnswer));
                    else obj.put("photo_text", error.optString(FormFilling.tagAnswer));
                    arr.put(error.optInt(FormFilling.tagIdError));
                }
                if (error.optInt(FormFilling.tagVirtual) == 0 && error.optInt(FormFilling.tagIdError, -1) > 0 && error.optInt("resolved_by", 0) <= 0 && error.optInt("resolved_by", 0) != -2/*&& !obj.has("manual_text")*/) {
                    others.put(error);
                }
            }
            obj.put("errors_to_solve", arr);
            obj.put("single_errors", others);
        } catch (Exception e) {
            Functions.err(e);
        }
        return obj;
    }

    private void SolveUnsolve (JSONObject group, JSONArray errors, int id_q ,boolean solve){
        try{
            ErrorDialog dialogVirtErr = new ErrorDialog(mActivity, id_q, errorAnswersSolved, errorCodesArraySolved);

            JSONArray erorsToSolve = group.optJSONArray("errors_to_solve");
            for (int i = 0; i<erorsToSolve.length();i++){
                int errorToSolve = erorsToSolve.optInt(i);
                dialogVirtErr.id_pr_err = errorToSolve;
                for (int x = 0; x<errors.length();x++ ){
                    JSONObject error = errors.optJSONObject(x);
                    if (error.optInt(FormFilling.tagIdError)== errorToSolve){
                        if (solve){
                            error.put("resolved_by", -2);
                            dialogVirtErr.addError(error.optString(FormFilling.tagType).equals(FormFilling.tagPhoto) ? error.optString(FormFilling.tagAnswer) : error.optString(FormFilling.tagManual), error.optString(FormFilling.tagType).equals(FormFilling.tagPhoto),
                                    error.optInt(FormFilling.tagIdError), error.optInt(FormFilling.tagIdError) == -2, "-2"
                                    ,error.optString("resolved_time", ""), error.optString("id_photo_sign", ""));
                            Log.e("errorAnswersSolved", errorAnswersSolved.toString());
                        }else
                        error.remove("resolved_by");
                        dialogVirtErr.removeError(error.optInt(FormFilling.tagIdError),error.optInt(FormFilling.tagIdQuestion));
                        break;
                    }
                }
            }
        }catch (Exception e){
            Functions.err(e);
        }
    }

    private void showError(final JSONArray errors) {
        final int id_q = errors.optJSONObject(0).optInt(FormFilling.tagIdQuestion);
        final JSONObject group = checkGroup(errors);
        LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error, llErrors, false);
        if (group.optJSONArray("errors_to_solve").length()>0){
            ((TextView) ll.findViewById(R.id.text)).setText(String.format("%s%s%s", group.optString("virtual_text") + (group.has("manual_text")&& group.has("virtual_text")? ", ":" "), group.optString("manual_text")+ (group.has("photo_text")&& group.has("manual_text")? ", ":""), group.optString("photo_text")));
            final CheckBox chk =  ll.findViewById(R.id.checkbox);

            chk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        /*ErrorDialog dialogVirtErr = new ErrorDialog(mActivity, singleError.optInt(FormFilling.tagIdQuestion), errorAnswersSolved, errorCodesArraySolved);
                        dialogVirtErr.id_pr_err = singleError.optInt(FormFilling.tagIdError);*/

                        if (chk.isChecked()){
                            SolveUnsolve(group,errors,id_q,true);
                        }else {
                            SolveUnsolve(group,errors,id_q,false);
                            /*singleError.remove("resolved_by");
                            dialogVirtErr.removeError(singleError.optInt(FormFilling.tagIdError),singleError.optInt(FormFilling.tagIdQuestion));*/
                        }
                        Log.e("errrrrrrrrr", errors.toString());

                    } catch (Exception e) {
                        Functions.err(e);
                    }
                }
            });


            llErrors.addView(ll);
        }
        JSONArray single_errors= group.optJSONArray("single_errors");
        if (single_errors.length()>0){
            for (int i = 0; i<single_errors.length(); i++){
                LinearLayout ll2 = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error, llErrors, false);
                final  JSONObject singleError = single_errors.optJSONObject(i);
                final CheckBox chk =  ll2.findViewById(R.id.checkbox);

                chk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            ErrorDialog dialogVirtErr = new ErrorDialog(mActivity, singleError.optInt(FormFilling.tagIdQuestion), errorAnswersSolved, errorCodesArraySolved);
                            dialogVirtErr.id_pr_err = singleError.optInt(FormFilling.tagIdError);

                            if (chk.isChecked()){
                                if (!singleError.has(FormFilling.tagIdGroup)) singleError.put(FormFilling.tagIdGroup, -1);
                                singleError.put("resolved_by", -2);
                                if (singleError.optInt(FormFilling.tagIdGroup) == -1) {
                                    dialogVirtErr.addError(singleError.optString(FormFilling.tagType).equals(FormFilling.tagPhoto) ? singleError.optString(FormFilling.tagAnswer) : singleError.optString(FormFilling.tagManual), singleError.optString(FormFilling.tagType).equals(FormFilling.tagPhoto),
                                            singleError.optInt(FormFilling.tagIdError), singleError.optInt(FormFilling.tagIdError) == -2, singleError.optString("resolved_by", "")
                                            ,singleError.optString("resolved_time", ""), singleError.optString("id_photo_sign", ""));
                                } else {
                                    dialogVirtErr.putAnswer(singleError.optInt(FormFilling.tagIdError), singleError.optInt(FormFilling.tagIdGroup),
                                            singleError.optString(FormFilling.tagType), singleError.optString(FormFilling.tagAnswer), singleError.optString(FormFilling.tagManual), singleError.optInt(FormFilling.tagVirtual), singleError.optString("resolved_by", "")
                                            ,singleError.optString("resolved_time", ""), singleError.optString("id_photo_sign", ""));
                                }
                            }else {
                                singleError.remove("resolved_by");
                                dialogVirtErr.removeError(singleError.optInt(FormFilling.tagIdError),singleError.optInt(FormFilling.tagIdQuestion));
                            }
                            Log.e("errrrrrrrrr", errors.toString());

                        } catch (Exception e) {
                            Functions.err(e);
                        }
                    }
                });


                for (int x = 0; x<errorCodes.length();x++){
                    JSONArray errorTepl = errorCodes.optJSONObject(x).optJSONArray(FormFilling.tagErrors);
                    for (int z = 0; z<errorTepl.length(); z++){
                        JSONObject errorCode = errorTepl.optJSONObject(z);
                        if (errorCode.optInt(FormFilling.tagIdError)==singleError.optInt(FormFilling.tagIdError)){
                            ((TextView)ll2.findViewById(R.id.text)).setText(errorCode.optString(actualLang));
                            llErrors.addView(ll2);
                            break;
                        }
                    }
                }
            }
        }

    }
}
