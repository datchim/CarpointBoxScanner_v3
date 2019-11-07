package com.carpoint.boxscanner.main;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.carpoint.boxscanner.MainActivity;
import com.carpoint.boxscanner.R;

import org.json.JSONArray;

import java.util.ArrayList;

public class UserDialog {

    private JSONArray users;
    private Dialog dialog;
    private MainActivity mActivity;
    private String  fullname;

    public static final String CARPOINT_username = "username_preference";
    public static final String CARPOINT_fullname = "fullname_preference";
    public static final String CARPOINT_password = "passwort_preference";

    private AutoCompleteTextView textVievType;

    private EditText editText_password;


    public UserDialog(final MainActivity activity, JSONArray users) {
        mActivity = activity;
        this.users = users;
        dialog = new Dialog(activity);
    }

    public void showDialog() {
        try {

            dialog.setCancelable(true);
            dialog.setContentView(R.layout.dialog_users);
            dialog.setTitle(mActivity.getResources().getString(R.string.login));

            editText_password = (EditText) dialog.findViewById(R.id.password);
            Button btnOk = (Button) dialog.findViewById(R.id.btn_ok);
            Button btnCancel = (Button) dialog.findViewById(R.id.btn_cancel);
            textVievType = (AutoCompleteTextView) dialog.findViewById (R.id.autocompletetextview);

            WindowManager.LayoutParams lWindowParams = new WindowManager.LayoutParams();
            lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            lWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lWindowParams);
            if (users!=null){
                final ArrayList<String> userList = new ArrayList<>();

                for (int i = 0; i < users.length(); i++) {
                    userList.add(users.optJSONObject(i).optString("username") + " (" + users.optJSONObject(i).optString("fullname") + ")");
                }

                ArrayAdapter<String> adapter =
                        new ArrayAdapter<String>(mActivity,
                                R.layout.item_auto, userList);
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
                        position = userList.indexOf(item);

                        fullname= users.optJSONObject(position).optString("fullname","");
                        textVievType.setText(users.optJSONObject(position).optString("username"));
                    }
                });
            }



            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences.Editor pEditor = getPrefs(mActivity).edit();
                    pEditor.putString(CARPOINT_username,textVievType.getText().toString()).apply();
                    pEditor.putString(CARPOINT_fullname,fullname).apply();
                    pEditor.putString(CARPOINT_username,textVievType.getText().toString()).apply();
                    pEditor.putString(CARPOINT_password,editText_password.getText().toString()).apply();
                    pEditor.commit();
                    mActivity.setHeadTitle();
                    mActivity.login();
                    dialog.dismiss();

                }
            });

            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });


            dialog.show();
        } catch (Exception e) {
            Functions.err(e);
        }

    }

    private  SharedPreferences getPrefs(Context context){
        return context.getSharedPreferences("com.carpoint.boxscanner_preferences", Context.MODE_PRIVATE);
    }

}