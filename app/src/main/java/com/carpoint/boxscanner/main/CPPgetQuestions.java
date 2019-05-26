package com.carpoint.boxscanner.main;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.carpoint.boxscanner.R;

public class CPPgetQuestions extends AsyncTask<Boolean, Boolean, String> {

    private final int BUFFER_SIZE = 16384;
    private final String delimiter = "--";
    private final String boundary = "SwA" + Long.toString(System.currentTimeMillis()) + "SwA";
    private Context mContext;
    private SharedPreferences prefs;
    private OnFinish onFinish;


    public CPPgetQuestions(Context context, OnFinish onFinish) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        this.onFinish = onFinish;
    }

    @Override
    protected String doInBackground(Boolean... booleans) {
        try {
            String url = prefs.getString("url_preference", "");

            JSONObject customer = new JSONObject();
            customer.accumulate("username",
                    prefs.getString("username_preference", ""));
            customer.accumulate("password",
                    prefs.getString("passwort_preference", ""));

            if (url.length() == 0) {
                Functions.toast(mContext, R.string.settup);
                return null;
            }

            // Check url
            if (!url.startsWith("http"))
                url = "https://" + url;
            if (!url.endsWith("/"))
                url += "/";
            url += "api/getQuestions";

            HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.connect();

            OutputStream os = con.getOutputStream();
            //Write login and form
            writeText(os, "login", customer.toString());

            os.write((delimiter + boundary + delimiter + "\r\n").getBytes());

            // adding data
            if (customer != null) {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                        con.getOutputStream());
                outputStreamWriter.write(customer.toString());
                outputStreamWriter.flush();
            }

            // Get data
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String receivestring = "";
                int read, index = 0;
                char[] buffer = new char[BUFFER_SIZE];

                InputStreamReader in = new InputStreamReader(
                        con.getInputStream(), "UTF-8");

                while ((read = in.read()) != -1) {
                    buffer[index] = (char) read;
                    index++;

                    if (index == BUFFER_SIZE) {
                        receivestring += new String(buffer);
                        index = 0;
                    }
                }
                if (index > 0) {
                    receivestring += new String(buffer, 0, index);
                }

                in.close();
                Log.e("rr", receivestring);
                if (receivestring.length() == 0)
                    return null;
                return receivestring;
            } else if (con.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Functions.toast(mContext, R.string.msg_error_unauthorized);
            } else {
                Functions.toast(mContext, R.string.msg_error_hhtp);
            }
        } catch (Exception e) {
            Functions.toast(mContext, R.string.msg_error_hhtp);
            Functions.err(e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        try {
            if (result != null) {
                try {
                    JSONObject o = new JSONObject(result);
                    Functions.toast(mContext, R.string.questionsDownloaded);
                    FileMan file = new FileMan(mContext, "");
                    file.saveDoc("questions.json", o);
                } catch (JSONException e) {
                    Functions.toast(mContext, R.string.msg_error_hhtp);
                }
            }

            if (onFinish != null)
                onFinish.onResult(result);
        } catch (Exception e) {
            Functions.toast(mContext, R.string.msg_error_hhtp);
        }
    }


    private void writeText(OutputStream os, String name, String text) {
        try {
            os.write((delimiter + boundary + "\r\n").getBytes());
            os.write("Content-Type: text/plain\r\n".getBytes());
            os.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes());
            os.write(("\r\n" + text + "\r\n").getBytes());
        } catch (Exception e) {
            Log.e("CP", Arrays.toString(e.getStackTrace()));
        }
    }

    public interface OnFinish {
        void onResult(String response);
    }

}
