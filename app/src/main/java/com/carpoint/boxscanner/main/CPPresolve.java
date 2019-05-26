package com.carpoint.boxscanner.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.carpoint.boxscanner.R;
import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class CPPresolve extends AsyncTask<Boolean, Boolean, String> {

    private final int BUFFER_SIZE = 16384;
    private final String delimiter = "--";
    private final String boundary = "SwA" + Long.toString(System.currentTimeMillis()) + "SwA";
    private Context mContext;
    private SharedPreferences prefs;
    private JSONArray ids;
    private Bitmap bitmapSign;
    private OnFinish onFinish;

    public CPPresolve(Context context, JSONArray ids, Bitmap bitmapSign, OnFinish onFinish ) {
        mContext = context;
        this.ids = ids;
        this.bitmapSign = bitmapSign;
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
            url += "api/resolveError";

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
            writeText(os, "errors", ids.toString());
            writeBitmap(os, "sign", bitmapSign, 100);
            os.write((delimiter + boundary + delimiter + "\r\n").getBytes());


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
            if (onFinish != null)
                onFinish.onResult(result);
        } catch (Exception e) {
            Functions.toast(mContext,R.string.protocol_not_found);
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


    private void writeBitmap(OutputStream os, String name, Bitmap bitmap, int quality) {
        try {
            if (bitmap == null || bitmap.getByteCount() == 0)
                return;

            os.write((delimiter + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + name + ".jpg" + "\"\r\n").getBytes());
            os.write(("Content-Type: application/octet-stream\r\n").getBytes());
            os.write(("Content-Transfer-Encoding: binary\r\n").getBytes());
            os.write("\r\n".getBytes());


            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);

            byte[] data = bos.toByteArray();
            os.write(data);
            os.write("\r\n".getBytes());
        } catch (Exception e) {
            Log.e("CP", Arrays.toString(e.getStackTrace()));
        }
    }
    public interface OnFinish {
        void onResult(String response);
    }

}
