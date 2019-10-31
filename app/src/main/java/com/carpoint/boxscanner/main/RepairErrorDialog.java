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

import java.util.ArrayList;

public class RepairErrorDialog {
    private static final String tagErrorList = "errorList", tagChecked = "checked", tagPrErrID = "id_pr_err", tagIdProtocol = "id_protocol",
            tagNull = "null";


    private Dialog dialog;
    private LinearLayout llErrors, llError;
    private Activity mActivity;
    private String actualLang = MainActivity.language;
    private JSONArray errors, questions,errorCodes, originalErrors, errorCodesArraySolved, errorAnswersSolved;
    private boolean showSign;
    private Bitmap bitmapSign;
    private SignatureView signatureView;
    private LinearLayout.LayoutParams buttonParams;
    public RepairErrorDialog(){
        
    }


    public void showDialog(final Activity activity, JSONArray errorCodes, JSONArray errorAnswers,  JSONArray errorCodesArraySolved, JSONArray errorAnswersSolved,JSONArray questions) {
        try {
            mActivity = activity;
            String temp = errorAnswers.toString();
            this.originalErrors= new JSONArray(temp);
            this.errors= errorAnswers;
            this.errorAnswersSolved = errorAnswersSolved;
            this.errorCodesArraySolved = errorCodesArraySolved;
            this.questions = questions;
            this.errorCodes = errorCodes;
            dialog = new Dialog(activity);
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

    private void refreshErrors(){
        llErrors.removeAllViews();
        for (int i=0; i<questions.length(); i++){
            JSONObject question = questions.optJSONObject(i);
            int q_id = question.optInt(FormFilling.tagIdQuestion);
            for (int z=0; z<errors.length();z++){
                JSONObject error = errors.optJSONObject(z);
                if (error.optInt(FormFilling.tagIdQuestion) == q_id){
                    showError(error.optJSONArray(FormFilling.tagErrors));
                }
            }
        }
    }


    private void showError(final JSONArray errors){

        for (int i = 0; i < errors.length(); i++){
            final JSONObject error = errors.optJSONObject(i);
            if (error.optInt("resolved_by",0)>0 || error.optInt("resolved_by",0)==-2){
                continue;
            }
            LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error, llErrors, false);
            if(error.optString("type","").equals("photo")&& error.optString(FormFilling.tagAnswer, "").startsWith("err_photo")){
                ((TextView) ll.findViewById(R.id.text)).setText(mActivity.getString(R.string.photoError)+" "+error.optInt("position",1));
            }else if ((error.optString(FormFilling.tagType).equals(FormFilling.tagYesNo))){
                for (int x = 0; x<errorCodes.length();x++){
                    JSONArray errorTepl = errorCodes.optJSONObject(x).optJSONArray(FormFilling.tagErrors);
                    for (int z = 0; z<errorTepl.length(); z++){
                        JSONObject errorCode = errorTepl.optJSONObject(z);
                        if (errorCode.optInt(FormFilling.tagIdError)==error.optInt(FormFilling.tagIdError)){
                            ((TextView)ll.findViewById(R.id.text)).setText(errorCode.optString(actualLang));
                            break;
                        }
                    }
                }
            }else if ((error.optInt(FormFilling.tagVirtual)==1)){
                ((TextView)ll.findViewById(R.id.text)).setText(error.optString(FormFilling.tagManual));
            }
            final CheckBox chk = (CheckBox) ll.findViewById(R.id.checkbox);

            chk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        ErrorDialog dialogVirtErr = new ErrorDialog(mActivity, error.optInt(FormFilling.tagIdQuestion), errorAnswersSolved, errorCodesArraySolved);
                        dialogVirtErr.id_pr_err = error.optInt(FormFilling.tagIdError);

                        if (chk.isChecked()){
                            if (!error.has(FormFilling.tagIdGroup)) error.put(FormFilling.tagIdGroup, -1);
                            error.put("resolved_by", -2);
                            if (error.optInt(FormFilling.tagIdGroup) == -1) {
                                dialogVirtErr.addError(error.optString(FormFilling.tagType).equals(FormFilling.tagPhoto) ? error.optString(FormFilling.tagAnswer) : error.optString(FormFilling.tagManual), error.optString(FormFilling.tagType).equals(FormFilling.tagPhoto),
                                        error.optInt(FormFilling.tagIdError), error.optInt(FormFilling.tagIdError) == -2, error.optString("resolved_by", "")
                                        ,error.optString("resolved_time", ""), error.optString("id_photo_sign", ""));
                            } else {
                                dialogVirtErr.putAnswer(error.optInt(FormFilling.tagIdError), error.optInt(FormFilling.tagIdGroup),
                                        error.optString(FormFilling.tagType), error.optString(FormFilling.tagAnswer), error.optString(FormFilling.tagManual), error.optInt(FormFilling.tagVirtual), error.optString("resolved_by", "")
                                        ,error.optString("resolved_time", ""), error.optString("id_photo_sign", ""));
                            }
                        }else {
                            error.remove("resolved_by");
                            dialogVirtErr.removeError(error.optInt(FormFilling.tagIdError),error.optInt(FormFilling.tagIdQuestion));
                        }
                        Log.e("errrrrrrrrr", errors.toString());

                    } catch (Exception e) {
                        Functions.err(e);
                    }
                }
            });
            llErrors.addView(ll);
        }
    }
}
