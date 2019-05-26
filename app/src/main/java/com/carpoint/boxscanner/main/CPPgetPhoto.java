package com.carpoint.boxscanner.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.carpoint.boxscanner.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class CPPgetPhoto extends AsyncTask<Boolean, Boolean, Bitmap> {

    private final int BUFFER_SIZE = 16384;
    private final String delimiter = "--";
    private final String boundary = "SwA" + Long.toString(System.currentTimeMillis()) + "SwA";
    private Context mContext;
    private SharedPreferences prefs;
    private String photoName;
    private int id_protocol;
    private OnFinish onFinish;

    public CPPgetPhoto(Context context,int id_protocol, String photoName, OnFinish onFinish ) {
        mContext = context;
        this.photoName = photoName;
        this.id_protocol = id_protocol;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        this.onFinish = onFinish;
    }

    @Override
    protected Bitmap doInBackground(Boolean... booleans) {
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
            url += "api/getPhoto";

            HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.connect();

            OutputStream os = con.getOutputStream();

            JSONObject photo = new JSONObject();
            photo.accumulate("id_protocol",id_protocol);
            photo.accumulate("filename",photoName);

            writeText(os, "login", customer.toString());
            writeText(os, "photo", photo.toString());
            os.write((delimiter + boundary + delimiter + "\r\n").getBytes());


            // Get data
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                Bitmap b = BitmapFactory.decodeStream(con.getInputStream());
                return b;
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
    protected void onPostExecute(Bitmap result) {
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
            ;
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
        void onResult(Bitmap response);
    }

}
