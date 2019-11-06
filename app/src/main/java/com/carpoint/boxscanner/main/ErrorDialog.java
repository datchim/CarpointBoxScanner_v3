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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.carpoint.boxscanner.MainActivity;
import com.carpoint.boxscanner.R;
import com.mindorks.paracamera.Camera;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ErrorDialog {

    private Camera camera;
    public int photoIdQuestion, photoIdError,id_pr_err = 0;
    public boolean isDialog = false;
    private JSONArray errorCodes, errorAnswers;
    private Dialog dialog;
    private LinearLayout llErrors;
    private Activity mActivity;
    private String actualLang = MainActivity.language, common_id_errors, searchString;
    private int  q_id;
    private ArrayList<Pair<String, Bitmap>> photosErr;

    public ErrorDialog(final Activity activity, int question_id, JSONArray error_answers,JSONArray passedErrors) {
        mActivity = activity;
        q_id = question_id;
        errorAnswers = error_answers;
        dialog = new Dialog(activity);
        errorCodes = passedErrors;
    }

    public ErrorDialog(final Activity activity, int question_id, String common_errors, JSONArray passedErrors,
                       JSONArray error_answers, ArrayList<Pair<String, Bitmap>> errorPhotos) {

        mActivity = activity;
        q_id = question_id;
        common_id_errors = common_errors;
        errorAnswers = error_answers;
        dialog = new Dialog(activity);
        errorCodes = new JSONArray();
        errorCodes = passedErrors;
        photosErr = errorPhotos;


    }

    /////////////////////////////////////////INIT///////////////////////////////////////////////

    public void showDialog() {
        try {
            isDialog = true ;
            searchString = "";
            dialog.setCancelable(true);
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
                    ((FormFilling) mActivity).refresh();
                }
            });
            btnAdError.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    addNewErrorDialog(mActivity);

                }
            });

            btnAdPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addError(mActivity.getString(R.string.photoError),true,0,false);
                    takePicture("err_photo_");
                }
            });

            ((EditText) dialog.findViewById(R.id.editSearch)).addTextChangedListener(new TextWatcher() {

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

    /////////////////////////////////////////DISPLAYING///////////////////////////////////////////////

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
                        displayError(errors.optJSONObject(x), true);
                    }
                }
            }


            for (int i = 0; i < errorCodes.length(); i++) {
                JSONObject obj = errorCodes.optJSONObject(i);

                if (searchString.length() > 0 && !searchString.toUpperCase().contains(obj.optString("code", "").toUpperCase())) {
                    continue;
                }

                JSONArray errors = obj.optJSONArray("errors");
                LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_head, llErrors, false);
                llErrors.addView(ll);

                ((TextView) ll.findViewById(R.id.text)).setText(obj.optString("code", "") + "-" + obj.optString(actualLang, ""));

                for (int x = 0; x < errors.length(); x++) {
                    displayError(errors.optJSONObject(x), false);
                }
            }

        } catch (Exception e) {
            Functions.err(e);
        }
    }

    private void displayError(JSONObject group, boolean common) {

        final boolean photoRequired = group.optString("photo_required", "").equals("1");

        boolean showCommon =false;
        final int errID = group.optInt("id_error", -1);
        final int errG = group.optInt("id_group", -1);

        if (group.has("id_question")) {
            if (group.optInt("id_question")!=q_id)
                return;
        }

        if (searchString.length() > 1 && !searchString.contains(group.optString("position", "")) && !common) {
            return;
        }

        LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error_photo, llErrors, false);
        if ((common_id_errors.contains(group.optString("id_error"))) && (common)){
            showCommon = true;
            ((TextView) ll.findViewById(R.id.text)).setText(group.optString(actualLang, ""));
        }else if(group.optString("type","").equals("photo")&&group.optString(actualLang, "").startsWith("err_photo")){

            ((TextView) ll.findViewById(R.id.text)).setText(mActivity.getString(R.string.photoError)+" "+group.optInt("position",1));
        }else{
            ((TextView) ll.findViewById(R.id.text)).setText(group.optString(actualLang, ""));
        }

        /*if(group.optInt("resolved_by", -1) > -1){
            ll.setBackgroundResource(R.color.lightgreen);
            ((TextView) ll.findViewById(R.id.text)).append("\n"+mActivity.getString(R.string.repaired));
        }*/

        if (showCommon && common) llErrors.addView(ll);
        if (!common) llErrors.addView(ll);

        final CheckBox chk = (CheckBox) ll.findViewById(R.id.checkbox);
        final Button btnMakePhoto = ll.findViewById(R.id.btn_make_photo);

        if (group.optInt("is_virtual")==1){
            chk.setVisibility(View.GONE);
        }

        btnMakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    photoIdQuestion = q_id;
                    photoIdError = errID;
                    takePicture("err_photo_");
                    chk.setChecked(true);

                    putAnswer(errID, errG, "photo", "err_photo_" + q_id + "_" + errID, null,0);
                    refreshErros();
                } catch (Exception e) {
                    Functions.err(e);
                }
            }
        });

        String answer = getAnswer(errID,q_id);
        if (isErrorResolved(errID,q_id)/* && group.optInt("is_virtual")==0*/)  {
            ll.setBackgroundResource(R.color.lightgreen);
            ((TextView) ll.findViewById(R.id.text)).append("\n"+mActivity.getString(R.string.repaired));
            btnMakePhoto.setVisibility(View.GONE);
            chk.setVisibility(View.GONE);
        }else{
            if (answer.equals("1")||answer.contains("err_photo")) {
                chk.setChecked(true);
                btnMakePhoto.setVisibility(View.GONE);
            }else{

                if (photoRequired) {
                    chk.setVisibility(View.GONE);

                } else {
                    btnMakePhoto.setVisibility(View.GONE);
                }
            }
        }


        chk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    if (((CheckBox) v).isChecked()) {
                        putAnswer(errID, errG, "yesno", "1", null,0);
                    } else {

                        removeError(errID, q_id);

                        String photoName = q_id + "_" + errID;
                        removeErrorPhoto(photoName);
                        if(photoRequired){
                            chk.setVisibility(View.GONE);
                            btnMakePhoto.setVisibility(View.VISIBLE);
                        }
                    }
                    refreshErros();
                } catch (Exception e) {
                    Functions.err(e);
                }
            }
        });
    }

    /////////////////////////////////////////INNER LOGIC///////////////////////////////////////////////

    public String getAnswer(int id_err, int q_id) {
        JSONArray errArray = new JSONArray();
        try {
            for (int i = 0; i < errorAnswers.length(); i++) {
                if (errorAnswers.optJSONObject(i).optInt("id_question") == q_id)
                    errArray = errorAnswers.optJSONObject(i).optJSONArray("errors");

                for (int x = 0; x < errArray.length(); x++) {
                    errArray.optJSONObject(x);
                    if ((errArray.optJSONObject(x).optInt("id_error") == id_err)) {
                        String answer = errArray.optJSONObject(x).optString("answer");
                        String manualText = errArray.optJSONObject(x).optString("manual_text");
                        return answer;
                    }
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
        return "";
    }
    public boolean isErrorResolved(int id_err, int q_id) {
        JSONArray errArray = new JSONArray();
        try {
            for (int i = 0; i < errorAnswers.length(); i++) {
                if (errorAnswers.optJSONObject(i).optInt("id_question") == q_id)
                    errArray = errorAnswers.optJSONObject(i).optJSONArray("errors");

                for (int x = 0; x < errArray.length(); x++) {
                    errArray.optJSONObject(x);
                    if ((errArray.optJSONObject(x).optInt("id_error") == id_err) && (errArray.optJSONObject(x).optInt("resolved_by",-1)>0 || errArray.optJSONObject(x).optInt("resolved_by",-1)==-2)) {

                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
        return false;
    }

    public void putAnswer(int id_error, int id_group, String type, String answer, String manual, int virtual) {
        putAnswer(id_error, id_group, type, answer, manual, virtual, null, null,null);
    }
    public void putAnswer(int id_error, int id_group, String type, String answer, String manual, int virtual, String resolved_by,  String resolved_time, String id_photo_sign) {
        try {
            JSONObject tmp = new JSONObject();
            tmp.put("id_error", id_error);
            if (id_pr_err!=0)
                tmp.put("id_pr_err", id_pr_err);
            if (id_group != -1)
                tmp.put("id_group", id_group);
            tmp.put("type", type);
            tmp.put("is_virtual", virtual);
            tmp.put("answer", answer);
            tmp.put("id_question",q_id);
            if (manual != null) {
                tmp.put("manual_text", manual);
            }
            if(resolved_by != null)
                tmp.put("resolved_by", resolved_by);
            tmp.put("resolved_time", resolved_time);
            tmp.put("id_photo_sign", id_photo_sign);
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

        } catch (Exception e) {
            Functions.err(e);
        }
    }

    public void addError(String text, boolean isPhoto, int downlError, boolean isVirtual) {
        addError(text,isPhoto,downlError,isVirtual,null,null,null);
    }

    public void addError(String text, boolean isPhoto, int downlError, boolean isVirtual, String resolved_by, String resolved_time, String id_photo_sign){

        try {
            Log.e ("error codes", errorCodes.toString());
            int errID = (int) Math.round(Math.random() * Integer.MIN_VALUE);
            if (downlError!=0) errID=downlError;
            if (downlError==-2) errID=-2;
            photoIdQuestion = q_id;
            photoIdError = errID;
            JSONObject group = new JSONObject();
            boolean newOne = true;

            for (int i = 0; i < errorCodes.length(); i++) {
                JSONObject obj = errorCodes.optJSONObject(i);

                if (obj.optInt("id_group")==-1) {
                    group = obj;
                    newOne = false;
                    break;
                }
            }

            if(newOne){
                group.put("id_group", -1);
                group.put((MainActivity.language), mActivity.getString(R.string.another_errors));
            }

            final JSONObject newError = new JSONObject();

            newError.put("id_group", -1);
            newError.put("id_error", errID);
            newError.put("id_question", q_id);
            if(resolved_by != null){
                newError.put("resolved_by", resolved_by);
                newError.put("resolved_time", resolved_time);
                newError.put("id_photo_sign", id_photo_sign);
            }

            if (isPhoto){
                putAnswer(errID, -1, "photo", "err_photo_" + q_id + "_" + photoIdError, null,0,resolved_by,resolved_time,id_photo_sign);
                newError.put("type", "photo");
                newError.put("photo_required", 1);
            }else{
                putAnswer(errID, -1, "yesno", "1", text,isVirtual?1:0,resolved_by, resolved_time, id_photo_sign);
                newError.put("is_virtual", isVirtual?1:0);
                newError.put("type", "yesno");
                newError.put("lang_cs", text);
                newError.put("lang_de", text);
                newError.put("lang_en", text);
                newError.put("lang_ro", text);
            }

            if(newOne){
                JSONArray newErrorsArray = new JSONArray();
                newErrorsArray.put(newError);
                newError.put("position",1);

                if (isPhoto){
                    newError.put("lang_cs", text +" "+1);
                    newError.put("lang_de", text +" "+1);
                    newError.put("lang_en", text +" "+1);
                    newError.put("lang_ro", text +" "+1);
                }
                group.put("errors", newErrorsArray);
                errorCodes.put(group);
            }else{
                for (int i = 0; i < errorCodes.length(); i++) {
                    JSONObject obj = errorCodes.optJSONObject(i);
                    JSONArray errors = obj.optJSONArray("errors");
                    for (int x =0; x<errors.length(); x++){
                        if (errors.optJSONObject(x).optString(actualLang).equals(text)) return;
                    }
                }
                JSONArray arr = group.optJSONArray("errors");
                newError.put("position",arr.length()+1);
                if (isPhoto){
                    newError.put("lang_cs", text +" "+(arr.length()+1));
                    newError.put("lang_de", text +" "+(arr.length()+1));
                    newError.put("lang_en", text +" "+(arr.length()+1));
                    newError.put("lang_ro", text +" "+(arr.length()+1));

                }
                arr.put(newError);
            }

            if (isDialog)  refreshErros();
        } catch (JSONException e) {
            Functions.err(e);
        }
    }

    public void removeError(int errID, int q_id){
        JSONArray errArray = new JSONArray();
        boolean breakit = false;
        for (int i = 0; i < errorAnswers.length(); i++) {
            if (errorAnswers.optJSONObject(i).optInt("id_question") == q_id)
                errArray = errorAnswers.optJSONObject(i).optJSONArray("errors");
            for (int x = 0; x < errArray.length(); x++) {
                errArray.optJSONObject(x);
                if ((errArray.optJSONObject(x).optInt("id_error") == errID)) {
                    errArray.remove(x);
                    breakit = true;
                    break;
                }
            }
            if(breakit)
                break;
        }
    }

    public void removeVirtualCode(int q_id){
        for (int i = 0; i < errorCodes.length(); i++) {
            JSONObject obj = errorCodes.optJSONObject(i);
            JSONArray errors = obj.optJSONArray("errors");
            for (int x =0; x<errors.length(); x++){
                if (errors.optJSONObject(x).optInt("id_error")==-2 && q_id==errors.optJSONObject(x).optInt("id_question")) {
                    errorCodes.optJSONObject(i).optJSONArray("errors").remove(x);
                }
            }
        }

    }

    private void removeErrorPhoto(String name) {
        for (int i = 0; i < photosErr.size(); i++) {
            if (photosErr.get(i).first.equals(name)) {
                photosErr.remove(i);
            }
        }
    }

    public void takePicture(String name){
        camera = new Camera.Builder()
                .resetToCorrectOrientation(true)
                .setTakePhotoRequestCode(2)
                .setDirectory("BoxScannerPics")
                .setName(name + System.currentTimeMillis())
                .setImageFormat(Camera.IMAGE_JPEG)
                .setCompression(75)
                .build(mActivity);
        try {
            camera.takePicture();
        } catch (IllegalAccessException e) {
            Functions.err(e);
        }
    }

    public Camera getCamera() {
        return camera;
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

    /////////////////////////////////////////DIALOGS///////////////////////////////////////////////

    private void addNewErrorDialog(final Activity activity) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.add_new_error_dialog, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle(activity.getString(R.string.btn_add_error));
        final EditText edt = (EditText) dialogView.findViewById(R.id.edit1);

        dialogBuilder.setPositiveButton(activity.getString(R.string.add_error), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String newErrorText = edt.getText().toString();
                addError(newErrorText,false,0,false);

            }
        });
        dialogBuilder.setNegativeButton((activity.getString(R.string.cancel)), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

}