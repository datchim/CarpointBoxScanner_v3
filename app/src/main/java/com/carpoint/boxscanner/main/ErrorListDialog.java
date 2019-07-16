package com.carpoint.boxscanner.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.carpoint.boxscanner.MainActivity;
import com.carpoint.boxscanner.R;
import com.google.gson.JsonObject;
import com.kyanogen.signatureview.SignatureView;
import com.mindorks.paracamera.Camera;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ErrorListDialog {


    public EditText editType, editSerial;
    public Button btnSearch;
    private JSONArray mProtocols;
    private Dialog dialog;
    private LinearLayout llProtocols, llHead;
    private Activity mActivity;
    private String actualLang = MainActivity.language;
    private boolean showSign,showall;
    private Bitmap bitmapSign;
    private SignatureView signatureView;


    public ErrorListDialog() {

    }

    public void showDialog(final Activity activity, JSONArray protocols) {
        try {
            mActivity = activity;
            mProtocols = protocols;
            dialog = new Dialog(activity);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.dilalog_errors_list);

            Button btnSave = dialog.findViewById(R.id.btn_save);
            Button btnBack = dialog.findViewById(R.id.btn_back);
            llProtocols = dialog.findViewById(R.id.protocols);
            signatureView = (SignatureView) dialog.findViewById(R.id.signature_view);

            WindowManager.LayoutParams lWindowParams = new WindowManager.LayoutParams();
            lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            lWindowParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setAttributes(lWindowParams);

            llHead = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.list_error_head, llProtocols, false);
            editType = llHead.findViewById(R.id.edit_name);
            editSerial = llHead.findViewById(R.id.edit_serial);
            btnSearch = llHead.findViewById(R.id.btn_search);

            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (showSign) {
                            bitmapSign = signatureView.getSignatureBitmap();
                            if (bitmapSign == null) {
                                Toast.makeText(mActivity, R.string.fillAll, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            JSONArray ids = new JSONArray();
                            for (int i = 0; i < mProtocols.length(); i++) {
                                JSONArray errors = mProtocols.optJSONObject(i).optJSONArray("errors");

                                if (errors != null) {
                                    for (int x = 0; x < errors.length(); x++) {
                                        if (errors.optJSONObject(x).optBoolean("checked", false)) {
                                            ids.put(errors.optJSONObject(x).optInt("id_pr_err", -1));
                                        }
                                    }
                                }
                            }
                            final AlertDialog mWaitDownloadDialog = new AlertDialog.Builder(mActivity)
                                    .setView(mActivity.getLayoutInflater().inflate(R.layout.wait_upload, null))
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
                            new HTTPcomm(mActivity, ids, bitmapSign, new HTTPcomm.OnFinish() {
                                @Override
                                public void onResult(String response) {
                                    if (response != null) {
                                        Functions.toast(mActivity, R.string.success);
                                    }

                                    mWaitDownloadDialog.dismiss();
                                    dialog.dismiss();
                                    ((MainActivity) mActivity).refreshErrors();
                                    ((MainActivity) mActivity).errorListDialog = null;
                                }
                            });

                        } else {
                            showSign = true;
                            llProtocols.removeAllViews();
                            (dialog.findViewById(R.id.llscroll)).setVisibility(View.GONE);
                            (dialog.findViewById(R.id.llsign)).setVisibility(View.VISIBLE);
                        }

                    } catch (Exception e) {
                        Functions.err(e);
                    }
                }
            });

            ((Button) dialog.findViewById(R.id.btnClear)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signatureView.clearCanvas();
                }
            });

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInf) {

                    refreshProtocol("", "");

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


    private void refreshProtocol(String nameProtocol, String serialProtocol) {
        try {
            llProtocols.removeAllViews();

            llProtocols.addView(llHead);

            btnSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refreshProtocol(editType.getText().toString(), editSerial.getText().toString());
                }
            });

            editType.setText(nameProtocol);
            editSerial.setText(serialProtocol);

            for (int i = 0; i < mProtocols.length(); i++) {
                JSONObject obj = mProtocols.optJSONObject(i);

                if ((obj.optString("name").contains(nameProtocol)) && (obj.optString("serial").contains(serialProtocol))) {
                    LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_head, llProtocols, false);

                    ((TextView) ll.findViewById(R.id.text)).setText(String.format("%s/%s", obj.optString("name"), obj.optString("serial")));
                    llProtocols.addView(ll);

                    if (obj.has("errors")) {
                        JSONArray errors = obj.optJSONArray("errors");

                        for (int x = 0; x < errors.length(); x++) {
                            displayError(errors.optJSONObject(x), obj.optInt("id_protocol", -1));
                        }
                    }
                }

            }
            if (!showall) {
                Button bt = (Button) mActivity.getLayoutInflater().inflate(R.layout.item_button, llProtocols, false);
                bt.setText(R.string.showAll);
                bt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            showall = true;
                            final AlertDialog mWaitDownloadDialog = new AlertDialog.Builder(mActivity)
                                    .setView(mActivity.getLayoutInflater().inflate(R.layout.wait_download, null))
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

                            new HTTPcomm(mActivity, new HTTPcomm.OnFinish() {
                                @Override
                                public void onResult(String response) {
                                    if (response != null) {
                                        try {
                                            mWaitDownloadDialog.dismiss();
                                            JSONObject tmp = new JSONObject(response);
                                            mProtocols = tmp.getJSONArray("protocols");

                                            refreshProtocol("","");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }, false , true);
                        } catch (Exception e) {
                            Functions.err(e);
                        }
                    }
                });
                llProtocols.addView(bt);
            }
        } catch (Exception e) {
            Functions.err(e);
        }
    }

    private void displayError(final JSONObject group, final int id_protocol) {

        LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error, llProtocols, false);

        if (group.optString("type", "").equals("photo")) {
            String tmp = group.optString(actualLang, "");
            if (tmp.length() > 0 && !tmp.equals("null")) {
                ((TextView) ll.findViewById(R.id.text)).setText(group.optString(actualLang, ""));
            } else {
                ((TextView) ll.findViewById(R.id.text)).setText(R.string.photoError);
            }
            final Button show = (Button) ll.findViewById(R.id.btn_make_photo);
            final ImageView image = (ImageView) ll.findViewById(R.id.camera_image);
            show.setVisibility(View.VISIBLE);
            show.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog mWaitDownloadDialog = new AlertDialog.Builder(mActivity)
                            .setView(mActivity.getLayoutInflater().inflate(R.layout.wait_download, null))
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

                    new HTTPcomm(mActivity, id_protocol, group.optString("answer", ""), new HTTPcomm.OnFinishBitmap() {
                        @Override
                        public void onResult(Bitmap response) {
                            mWaitDownloadDialog.dismiss();
                            if(response != null){
                                show.setVisibility(View.GONE);
                                image.setVisibility(View.VISIBLE);
                                image.setImageBitmap(response);
                            }
                        }
                    });
                }
            });
        } else {
            String tmp = group.optString("manual_text", "");
            if (tmp.length() > 0 && !tmp.equals("null")) {
                ((TextView) ll.findViewById(R.id.text)).setText(tmp);
            } else {
                ((TextView) ll.findViewById(R.id.text)).setText(group.optString(actualLang, ""));
            }

        }
        final CheckBox chk = (CheckBox) ll.findViewById(R.id.checkbox);

        chk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    group.put("checked", chk.isChecked());
                } catch (Exception e) {
                    Functions.err(e);
                }
            }
        });

        llProtocols.addView(ll);
    }


}