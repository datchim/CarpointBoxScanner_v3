package com.carpoint.boxscanner.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

import com.carpoint.boxscanner.R;
import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class CPPsendPdf extends AsyncTask<Void, Boolean, String> {

    private final int BUFFER_SIZE = 16384;
    private final String delimiter = "--";
    private final String boundary = "SwA" + Long.toString(System.currentTimeMillis()) + "SwA";
    private Context mContext;
    private SharedPreferences prefs;
    private OnFinish onFinish;
    private JSONObject head;
    private JSONArray answers, errorAnswers;
    private Bitmap sign;
    private ArrayList<Pair<Integer, Bitmap>> photos = new ArrayList<>();
    private ArrayList<Pair<String, Bitmap>> photosErr = new ArrayList<>();

    public CPPsendPdf(Context context, JSONObject head, JSONArray answers, JSONArray errorAnswers, OnFinish onFinish, ArrayList<Pair<Integer, Bitmap>> photos, ArrayList<Pair<String, Bitmap>> photosError, Bitmap sign) {
        mContext = context;
        this.photos = photos;
        this.photosErr = photosError;
        this.head = head;
        this.answers = answers;
        this.sign = sign;
        this.errorAnswers= errorAnswers;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        this.onFinish = onFinish;
    }

    @Override
    protected String doInBackground(Void... bitmaps) {
        try {
            String url = prefs.getString("url_preference", "");

            head.put("username",
                    prefs.getString("username_preference", ""));
            head.put("password",
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
            url += "api/sendProtocol";

            HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();

            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setConnectTimeout(10000);
            con.setReadTimeout(6000000);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.connect();

            OutputStream os = con.getOutputStream();
            //Write login, form and error answers
            writeText(os, "head", head.toString());
            writeText(os, "answers", answers.toString());
            writeText(os, "errors", errorAnswers.toString());

            for (int i = 0; i < photos.size(); i++) {
                String idphoto = photos.get(i).first.toString();
                Bitmap bitmap = photos.get(i).second;
                writeBitmap(os, "photo_" + idphoto, bitmap, 75);
            }
            for (int i = 0; i < photosErr.size(); i++) {
                String idphoto = photosErr.get(i).first;
                Bitmap bitmap = photosErr.get(i).second;
                writeBitmap(os, "err_photo_" + idphoto, bitmap, 75);
            }

            writeBitmap(os, "sign", sign, 100);

            os.write((delimiter + boundary + delimiter + "\r\n").getBytes());

            //Get response
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

    private Bitmap setBitmapLand(Bitmap bitmap) {
        if (bitmap.getWidth() < bitmap.getHeight()) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return bitmap;
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        try {
            boolean save = false;
            if (result != null) {
                JSONObject obj = new JSONObject(result);
                String tmp = obj.optString("status","");
                if (onFinish != null)
                    onFinish.onResult(tmp);
                if (tmp == null || !tmp.equals("OK"))
                    save = true;
            } else {
                if (onFinish != null)
                    onFinish.onResult(null);
                save = true;
            }

            if (save) {
                FileMan f = new FileMan(mContext, "to_send_" + new Date().getTime());
                f.saveDoc("head.json", head);
                f.saveDoc("answers.json", answers);
                f.saveDoc("error answers.json", errorAnswers);
                f.saveBitmap("sign.jpg", sign);
                for (int i = 0; i < photos.size(); i++) {
                    f.saveBitmap("photo_" + photos.get(i).first + ".jpg", photos.get(i).second);
                }
                for (int i = 0; i < photosErr.size(); i++) {
                    f.saveBitmap("err_photo_" + photosErr.get(i).first + ".jpg", photosErr.get(i).second);
                }
            }
        } catch (Exception e) {
            Functions.err(e);
            if (onFinish != null)
                onFinish.onResult(null);
        }
    }

    public interface OnFinish {
        void onResult(String response);
    }

}
