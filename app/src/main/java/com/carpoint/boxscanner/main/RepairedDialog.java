package com.carpoint.boxscanner.main;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.carpoint.boxscanner.MainActivity;
import com.carpoint.boxscanner.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class RepairedDialog {

    public boolean isDialog = false;
    private JSONArray errorCodes, errorAnswers;
    private Dialog dialog;
    private LinearLayout llErrors;
    private Activity mActivity;
    private String actualLang = MainActivity.language, common_id_errors, searchString;
    private int  q_id;

    public RepairedDialog(final Activity activity, JSONArray error_answers, JSONArray passedErrors) {
        mActivity = activity;
        errorAnswers = error_answers;
        dialog = new Dialog(activity);
        errorCodes = passedErrors;
        showDialog();
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
            btnAdError.setVisibility(View.GONE);
            btnAdPhoto.setVisibility(View.GONE);
            llErrors = dialog.findViewById(R.id.erros);

            dialog.findViewById(R.id.editSearch).setVisibility(View.GONE);

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


            dialog.show();
        } catch (Exception e) {
            Functions.err(e);
        }

    }

    /////////////////////////////////////////DISPLAYING///////////////////////////////////////////////

    private void refreshErros() {
        try {
            llErrors.removeAllViews();

            for (int i = 0; i < errorAnswers.length(); i++) {

                displayError(errorAnswers.optJSONObject(i));
            }


            /*for (int i = 0; i < errorCodes.length(); i++) {
                JSONObject obj = errorCodes.optJSONObject(i);

                JSONArray errors = obj.optJSONArray("errors");
                LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_head, llErrors, false);
                llErrors.addView(ll);

                ((TextView) ll.findViewById(R.id.text)).setText(obj.optString("code", "") + "-" + obj.optString(actualLang, ""));

                for (int x = 0; x < errors.length(); x++) {
                    displayError(errors.optJSONObject(x));
                }
            }*/

        } catch (Exception e) {
            Functions.err(e);
        }
    }
    private void displayError(JSONObject error) {
        LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error_photo, llErrors, false);
        ll.findViewById(R.id.checkbox).setVisibility(View.GONE);
        ll.findViewById(R.id.btn_make_photo).setVisibility(View.GONE);
        llErrors.addView(ll);

        if(error.optInt("is_virtual", 0) == 1){
            ((TextView) ll.findViewById(R.id.text)).setText(error.optString("manual_text",""));
        }else{
            int errID = error.optInt("id_error", -1);
            int errG = error.optInt("id_group", -1);
            String text = "";

            for (int i = 0; i < errorCodes.length(); i++) {
                JSONObject obj = errorCodes.optJSONObject(i);

                if(obj.optInt("id_group") == errG){
                    JSONArray errors = obj.optJSONArray("errors");
                    for (int j = 0; j < errors.length(); j++) {

                        if(errors.optJSONObject(j).optInt("id_error") == errID){
                            text=obj.optString(actualLang,"")+" - "+errors.optJSONObject(j).optString(actualLang,"");
                            break;
                        }
                    }
                }
            }
            ((TextView) ll.findViewById(R.id.text)).setText(text);
        }
    }


    private void displayError2(JSONObject group) {


        final int errID = group.optInt("id_error", -1);
        final int errG = group.optInt("id_group", -1);

        if (group.has("id_question")) {
            if (group.optInt("id_question")!=q_id)
                return;
        }

        LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error_photo, llErrors, false);
       if(group.optString("type","").equals("photo")&&group.optString(actualLang, "").startsWith("err_photo")){

            ((TextView) ll.findViewById(R.id.text)).setText(mActivity.getString(R.string.photoError)+" "+group.optInt("position",1));
        }else{
            ((TextView) ll.findViewById(R.id.text)).setText(group.optString(actualLang, ""));
        }

        if(group.optInt("resolved_by", -1) > -1){
            ll.setBackgroundResource(R.color.lightgreen);
            ((TextView) ll.findViewById(R.id.text)).append("\n"+mActivity.getString(R.string.repaired));
        }

        llErrors.addView(ll);

        ll.findViewById(R.id.checkbox).setVisibility(View.GONE);
        ll.findViewById(R.id.btn_make_photo).setVisibility(View.GONE);

        String answer = getAnswer(errID,q_id);
        /*if (answer.equals("1")||answer.contains("err_photo")) {
            chk.setChecked(true);
            btnMakePhoto.setVisibility(View.GONE);
        }else{

            if (photoRequired) {
                chk.setVisibility(View.GONE);

            } else {
                btnMakePhoto.setVisibility(View.GONE);
            }
        }*/
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



    private void getScrollHandler() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ScrollView scrollView =
                        dialog.findViewById(R.id.ScrollView);
                if (scrollView != null) scrollView.smoothScrollTo(0, 0);
            }
        }, 100);
    }

}