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
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.carpoint.boxscanner.MainActivity;
import com.carpoint.boxscanner.R;
import com.kyanogen.signatureview.SignatureView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ListDialog {

    
    public EditText editType, editSerial;
    public Button btnSearch;
    private JSONArray mProtocols, places;
    private Dialog dialog;
    private LinearLayout llProtocols, llHead;
    private Activity mActivity;
    private String actualLang = MainActivity.language, item, username;
    private boolean showSign, isAdmin;
    private Bitmap bitmapSign;
    private SignatureView signatureView;
    private LinearLayout.LayoutParams buttonParams;
    private Spinner autoCompletePlace;
    private JSONObject selectedPlace;

    public ListDialog() {

    }

    /////////////////////////////////////////INIT///////////////////////////////////////////////

    @SuppressLint("MissingPermission")
    public void showDialog(final Activity activity, JSONArray protocols, final String item, boolean isAdmin, JSONArray places) {
        try {
            mActivity = activity;
            mProtocols = protocols;
            this.isAdmin = isAdmin;
            this.places = places;
            dialog = new Dialog(activity);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.dilalog_errors_list);
            this.item = item;
            Button btnSave = dialog.findViewById(R.id.btn_save);
            Button btnBack = dialog.findViewById(R.id.btn_back);

            btnSave.setVisibility(item.startsWith(T.tagErrorList) ? View.VISIBLE : View.GONE);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
            username = prefs.getString(MainActivity.CARPOINT_username, "");

            llProtocols = dialog.findViewById(R.id.protocols);
            signatureView = dialog.findViewById(R.id.signature_view);

            WindowManager.LayoutParams lWindowParams = new WindowManager.LayoutParams();
            lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            lWindowParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setAttributes(lWindowParams);

            buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            buttonParams.setMargins(20, 10, 20, 10);

            llHead = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.list_error_head, llProtocols, false);
            editType = llHead.findViewById(R.id.edit_name);
            editSerial = llHead.findViewById(R.id.edit_serial);
            btnSearch = llHead.findViewById(R.id.btn_search);
            autoCompletePlace = llHead.findViewById(R.id.dropdownPlace);

            if (places != null) {
                final ArrayList<String> placeList = new ArrayList<>();

                for (int i = 0; i < places.length(); i++) {
                    placeList.add(places.optJSONObject(i).optString("name"));
                }

                ArrayAdapter<String> adapter =
                        new ArrayAdapter<String>(mActivity,
                                R.layout.item_auto, placeList);
                adapter.setDropDownViewResource(R.layout.item_autocomplete_drop);
                autoCompletePlace.setAdapter(adapter);

                autoCompletePlace.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selectedPlace = ListDialog.this.places.optJSONObject(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                LocationManager mLocationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
                Location last = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last == null)
                    last = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                if (last != null) {
                    double lat = last.getLatitude();
                    double lng = last.getLongitude();
                    double min = Integer.MAX_VALUE;
                    int index = -1;
                    for (int i = 0; i < places.length(); i++) {
                        double tmp = Math.abs(places.optJSONObject(i).optDouble("loc_lat") - lat)
                                + Math.abs(places.optJSONObject(i).optDouble("loc_lng") - lng);
                        if (tmp < min) {
                            min = tmp;
                            index = i;
                        }
                    }
                    if (index > -1) {

                        selectedPlace = ListDialog.this.places.optJSONObject(index);
                    }
                    autoCompletePlace.setSelection(index);
                } else {
                    autoCompletePlace.setSelection(0);
                    selectedPlace = ListDialog.this.places.optJSONObject(0);
                }
            } else {
                llHead.findViewById(R.id.llPlace).setVisibility(View.GONE);
            }

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
                                JSONArray errors = mProtocols.optJSONObject(i).optJSONArray(MainActivity.tagErrors);

                                if (errors != null) {
                                    for (int x = 0; x < errors.length(); x++) {
                                        if (errors.optJSONObject(x).optBoolean(T.tagChecked, false)) {
                                            ids.put(errors.optJSONObject(x).optInt(T.tagPrErrID, -1));
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
                                    ((MainActivity) mActivity).refreshErrors(item);
                                    ((MainActivity) mActivity).listDialog = null;
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

            dialog.findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signatureView.clearCanvas();
                }
            });

            //Zdrznnutí virtual manual
            if (!item.equals(MainActivity.tagUncompleteProto)) {
                for (int i = 0; i < mProtocols.length(); i++) {
                    JSONObject obj = mProtocols.optJSONObject(i);

                    if (obj.has(T.tagErrors)) {
                        JSONArray errors = obj.optJSONArray(T.tagErrors);

                        for (int j = 0; j < errors.length(); j++) {
                            if (errors.optJSONObject(j).optInt("is_virtual") == 1) {
                                findManuals(errors, errors.optJSONObject(j), errors.optJSONObject(j).optInt("id_question"));
                            }
                        }
                    }
                }

            }

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInf) {

                    refreshProtocol("", "");

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

    private void findManuals(JSONArray errors, JSONObject virtual, int id_q) {
        try {
            for (int i = 0; i < errors.length(); i++) {
                if (errors.optJSONObject(i).optInt("id_question") == id_q
                        && errors.optJSONObject(i).optInt("is_virtual") == 0
                        && errors.optJSONObject(i).optInt("id_group") == -1) {


                    errors.optJSONObject(i).put("dont_show", true);
                    if (errors.optJSONObject(i).optString(T.tagType, "").equals(T.tagPhoto)) {
                        virtual.put(T.tagType, T.tagPhoto);
                        virtual.put(T.tagAnswer
                                , errors.optJSONObject(i).optString(T.tagAnswer, ""));

                    } else {
                        virtual.put(T.tagManual, virtual.optString(T.tagManual, "")
                                + "\n" + errors.optJSONObject(i).optString(T.tagManual, ""));
                    }
                    if (!virtual.has("others")) virtual.put("others", new JSONArray());
                    virtual.optJSONArray("others").put(errors.optJSONObject(i));
                }
            }

        } catch (Exception e) {
            Functions.err(e);
        }
    }

    /////////////////////////////////////////NETWORK///////////////////////////////////////////////

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
                final JSONObject obj = mProtocols.optJSONObject(i);

                //Check place
                if (selectedPlace != null) {
                    int id_place = selectedPlace.optInt("id_place");

                    double latP = obj.optDouble("loc_lat", 0);
                    double lngP = obj.optDouble("loc_lng", 0);
                    if (latP != 0) {
                        double min = Integer.MAX_VALUE;
                        int id_min = -1;
                        for (int j = 0; j < places.length(); j++) {

                            double tmp = Math.abs(latP - places.optJSONObject(j).optDouble("loc_lat", 0))
                                    + Math.abs(lngP - places.optJSONObject(j).optDouble("loc_lng", 0));
                            if (tmp < min) {
                                min = tmp;
                                id_min = places.optJSONObject(j).optInt("id_place");
                            }
                        }
                        if (id_min != id_place) {
                            continue;
                        }
                    }
                }

                //if (!obj.optString(MainActivity.tagUsername).equals(username) && !showAll) continue;
                if ((obj.optString(T.tagName).contains(nameProtocol)) && (serialProtocol.length() == 0 ||
                        (obj.optString(T.tagSerial).equals(serialProtocol)))) {

                    if (item.equals(MainActivity.tagUncompleteProto)) {

                        LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_list, llProtocols, false);
                        ((TextView) ll.findViewById(R.id.text)).setText(String.format("%s/%s", obj.optString(T.tagName), obj.optString(T.tagSerial)));
                        ImageButton bt = ll.findViewById(R.id.btn_list);
                      //  bt.setLayoutParams(buttonParams);

                        bt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(mActivity.getApplicationContext(), FormFilling.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra(T.tagType, obj.optString(T.tagName));
                                intent.putExtra(T.tagSerial, obj.optString(T.tagSerial));
                                intent.putExtra(T.tagIdPlan, obj.optInt(T.tagIdPlan));
                                mActivity.startActivity(intent);
                                dialog.dismiss();
                            }
                        });

                        if (!obj.optBoolean("has_unsolved_errors")) ll.setBackgroundResource(R.color.lightgreen);
                        llProtocols.addView(ll);
                    } else {
                        LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_head, llProtocols, false);

                        ((TextView) ll.findViewById(R.id.text)).setText(String.format("%s/%s", obj.optString(T.tagName), obj.optString(T.tagSerial)));
                        llProtocols.addView(ll);
                        if (obj.has(T.tagErrors)) {
                            JSONArray errors = obj.optJSONArray(T.tagErrors);

                            for (int x = 0; x < errors.length(); x++) {
                                displayError(errors.optJSONObject(x), obj.optInt(T.tagIdProtocol, -1));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            Functions.err(e);
        }
    }

    private void displayError(final JSONObject group, final int id_protocol) {

        if (group.optBoolean("dont_show")) return;
        LinearLayout ll = null;

        if (group.optString(T.tagType, "").equals(T.tagPhoto)) {
            ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error_photo, llProtocols, false);
            String tmp = group.optString(actualLang, "");
            if (tmp.length() > 0 && !tmp.equals(T.tagNull)) {
                ((TextView) ll.findViewById(R.id.text)).setText(group.optString(actualLang, ""));
            } else {
                tmp = group.optString(T.tagManual, "");
                if (tmp.length() > 0 && !tmp.equals(T.tagNull)) {
                    ((TextView) ll.findViewById(R.id.text)).setText(tmp);
                } else {
                    ((TextView) ll.findViewById(R.id.text)).setText(R.string.photoError);
                }
            }
            final Button show = ll.findViewById(R.id.btn_make_photo);
            final ImageView image = ll.findViewById(R.id.camera_image);
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

                    new HTTPcomm(mActivity, id_protocol, group.optString(T.tagAnswer, ""), new HTTPcomm.OnFinishBitmap() {
                        @Override
                        public void onResult(Bitmap response) {
                            mWaitDownloadDialog.dismiss();
                            if (response != null) {
                                show.setVisibility(View.GONE);
                                image.setVisibility(View.VISIBLE);
                                image.setImageBitmap(response);
                            }
                        }
                    });
                }
            });
        } else {
            ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.item_error, llProtocols, false);
            String tmp = group.optString(T.tagManual, "");
            if (tmp.length() > 0 && !tmp.equals(T.tagNull)) {
                ((TextView) ll.findViewById(R.id.text)).setText(tmp);
            } else {
                ((TextView) ll.findViewById(R.id.text)).setText(group.optString(actualLang, ""));
            }

        }
        final CheckBox chk = ll.findViewById(R.id.checkbox);

        chk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    group.put(T.tagChecked, chk.isChecked());
                    if (group.has("others")) {
                        JSONArray others = group.optJSONArray("others");
                        for (int i = 0; i < others.length(); i++) {
                            others.optJSONObject(i).put(T.tagChecked, chk.isChecked());
                        }
                    }
                } catch (Exception e) {
                    Functions.err(e);
                }
            }
        });
        llProtocols.addView(ll);
    }


}