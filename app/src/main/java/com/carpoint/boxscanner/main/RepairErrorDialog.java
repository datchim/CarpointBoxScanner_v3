package com.carpoint.boxscanner.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.carpoint.boxscanner.MainActivity;
import com.carpoint.boxscanner.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class RepairErrorDialog {

    private Dialog dialog;
    private LinearLayout llErrors;
    private Activity mActivity;
    private String actualLang = MainActivity.language;
    private JSONArray errors, questions, errorCodes, errorCodesArraySolved, errorAnswersSolved;
    private JSONObject question;


    public RepairErrorDialog(final Activity activity, JSONArray errorCodes, JSONArray errorAnswers, JSONArray errorCodesArraySolved, JSONArray errorAnswersSolved, JSONArray questions) {
        mActivity = activity;
            this.errors = errorAnswers;
            this.errorAnswersSolved = errorAnswersSolved;
            this.errorCodesArraySolved = errorCodesArraySolved;
            this.questions = questions;
            this.errorCodes = errorCodes;
    }

    public RepairErrorDialog(final Activity activity, JSONArray errorCodes, JSONArray errorAnswers, JSONArray errorCodesArraySolved, JSONArray errorAnswersSolved, JSONObject question) {
            mActivity = activity;
            this.errors = errorAnswers;
            this.errorAnswersSolved = errorAnswersSolved;
            this.errorCodesArraySolved = errorCodesArraySolved;
            this.question = question;
            this.errorCodes = errorCodes;
    }


    public void showDialog() {
        try {
            dialog = new Dialog(mActivity);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.dialog_repair);

            Button btnSave = dialog.findViewById(R.id.btn_save);

            llErrors = dialog.findViewById(R.id.errors);

            WindowManager.LayoutParams lWindowParams = new WindowManager.LayoutParams();
            lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            lWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lWindowParams);


            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
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
                            dialog.findViewById(R.id.ScrollView);
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
                int q_id = question.optInt(T.tagIdQuestion);
                for (int z = 0; z < errors.length(); z++) {
                    JSONObject error = errors.optJSONObject(z);
                    if (error.optInt(T.tagIdQuestion) == q_id) {
                        showError(error.optJSONArray(T.tagErrors));
                    }
                }
            }
        } else {
            int q_id = question.optInt(T.tagIdQuestion);
            for (int z = 0; z < errors.length(); z++) {
                JSONObject error = errors.optJSONObject(z);
                if (error.optInt(T.tagIdQuestion) == q_id) {
                    showError(error.optJSONArray(T.tagErrors));
                }
            }
        }
    }

    private JSONObject getGroupedErrors(final JSONArray errors) {
        JSONObject obj = new JSONObject();
        JSONArray arr = new JSONArray();
        JSONArray others = new JSONArray();
        boolean hasManual = false;
        int countNonManual = 0;
        try {
            for (int i = 0; i < errors.length(); i++) {
                final JSONObject error = errors.optJSONObject(i);
                if (error.optInt(T.tagResolvedBy, 0) <= 0 && error.optInt(T.tagResolvedBy, 0) != -2) {
                    if (error.optInt(T.tagVirtual) == 1 && !obj.has(T.tagVirtualText)) {
                        obj.put(T.tagVirtualText, error.optString(T.tagManual));
                        arr.put(error.optInt(T.tagIdError));
                        hasManual = true;
                    }
                    if (error.optInt(T.tagVirtual) == 0 && !error.optString(T.tagManual, "").equals("null") && !error.optString(T.tagType).equals(T.tagPhoto) && error.optInt(T.tagIdError, -1) < 0) {
                        if (obj.has(T.tagManualText))
                            obj.put(T.tagManualText, obj.optString(T.tagManualText, "") + " - " + error.optString(T.tagManual));
                        else obj.put(T.tagManualText, error.optString(T.tagManual));
                        arr.put(error.optInt(T.tagIdError));
                        hasManual = true;
                    }
                    if (error.optString(T.tagType).equals(T.tagPhoto)) {
                        if (obj.has(T.tagPhotoText))
                            obj.put(T.tagPhotoText, obj.optString(T.tagPhotoText, "") + " - " + error.optString(T.tagAnswer));
                        else obj.put(T.tagPhotoText, error.optString(T.tagAnswer));
                        arr.put(error.optInt(T.tagIdError));
                        hasManual = true;
                    }
                    if (error.optInt(T.tagVirtual) == 0 && error.optInt(T.tagIdError, -1) > 0) {
                        others.put(error);
                        countNonManual++;
                    }
                }
            }
            obj.put("errors_to_solve", arr);
            obj.put("single_errors", others);
            obj.put("non_manual_count", countNonManual);
            obj.put("has_manual", hasManual);
        } catch (Exception e) {
            Functions.err(e);
        }
        return obj;
    }

    private void SolveUnsolve(JSONObject group, JSONArray errors, int id_q, boolean solve) {
        try {

            JSONArray erorsToSolve = group.optJSONArray("errors_to_solve");
            for (int i = 0; i < erorsToSolve.length(); i++) {
                int errorToSolve = erorsToSolve.optInt(i);

                for (int x = 0; x < errors.length(); x++) {
                    JSONObject error = errors.optJSONObject(x);
                    if (error.optInt(T.tagIdError) == errorToSolve) {

                        final ErrorDialog dialogVirtErr = new ErrorDialog(mActivity, id_q, errorAnswersSolved, errorCodesArraySolved);
                        dialogVirtErr.id_pr_err = errorToSolve;

                        if (solve) {
                            error.put(T.tagResolvedBy, -2);
                            dialogVirtErr.addError(error.optString(T.tagType).equals(T.tagPhoto) ? error.optString(T.tagAnswer) : error.optString(T.tagManual), error.optString(T.tagType).equals(T.tagPhoto),
                                    error.optInt(T.tagIdError), error.optInt(T.tagIdError) == -2, error.optString(T.tagResolvedBy)
                                    , error.optString(T.tagResolvedTime, ""), error.optString(T.tagIdPhotoSign, ""));
                        } else {
                            error.remove(T.tagResolvedBy);
                            dialogVirtErr.removeError(error.optInt(T.tagIdError), error.optInt(T.tagIdQuestion));
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    private void showError(final JSONArray errors) {
        final int id_q = errors.optJSONObject(0).optInt(T.tagIdQuestion);
        final JSONObject group = getGroupedErrors(errors);
        LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error, llErrors, false);
        if (group.optJSONArray("errors_to_solve").length() > 0) {
            ((TextView) ll.findViewById(R.id.text)).setText(String.format("%s%s%s", group.optString(T.tagVirtualText) + (group.has(T.tagManualText) && group.has(T.tagVirtualText) ? ", " : " "), group.optString(T.tagManualText) + (group.has(T.tagPhotoText) && group.has(T.tagManualText) ? ", " : ""), group.optString(T.tagPhotoText)));
            final CheckBox chk = ll.findViewById(R.id.checkbox);

            chk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (chk.isChecked()) {
                            SolveUnsolve(group, errors, id_q, true);
                        } else {
                            SolveUnsolve(group, errors, id_q, false);
                        }
                    } catch (Exception e) {
                        Functions.err(e);
                    }
                }
            });
            llErrors.addView(ll);
        }
        JSONArray single_errors = group.optJSONArray("single_errors");
        if (single_errors.length() > 0) {
            for (int i = 0; i < single_errors.length(); i++) {
                LinearLayout ll2 = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error, llErrors, false);
                final JSONObject singleError = single_errors.optJSONObject(i);
                final CheckBox chk = ll2.findViewById(R.id.checkbox);

                chk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            ErrorDialog dialogVirtErr = new ErrorDialog(mActivity, singleError.optInt(T.tagIdQuestion), errorAnswersSolved, errorCodesArraySolved);
                            dialogVirtErr.id_pr_err = singleError.optInt(T.tagIdError);

                            if (chk.isChecked()) {
                                singleError.put(T.tagResolvedBy, -2);
                                dialogVirtErr.putAnswer(singleError.optInt(T.tagIdError), singleError.optInt(T.tagIdGroup),
                                        singleError.optString(T.tagType), singleError.optString(T.tagAnswer), singleError.optString(T.tagManual), singleError.optInt(T.tagVirtual), singleError.optString(T.tagResolvedBy, "")
                                        , singleError.optString(T.tagResolvedTime, ""), singleError.optString(T.tagIdPhotoSign, ""));
                            } else {
                                singleError.remove(T.tagResolvedBy);
                                dialogVirtErr.removeError(singleError.optInt(T.tagIdError), singleError.optInt(T.tagIdQuestion));
                            }
                        } catch (Exception e) {
                            Functions.err(e);
                        }
                    }
                });

                for (int x = 0; x < errorCodes.length(); x++) {
                    JSONArray errorTepl = errorCodes.optJSONObject(x).optJSONArray(T.tagErrors);
                    for (int z = 0; z < errorTepl.length(); z++) {
                        JSONObject errorCode = errorTepl.optJSONObject(z);
                        if (errorCode.optInt(T.tagIdError) == singleError.optInt(T.tagIdError)) {
                            ((TextView) ll2.findViewById(R.id.text)).setText(errorCode.optString(actualLang));
                            llErrors.addView(ll2);
                            break;
                        }
                    }
                }
            }
        }
    }
}
