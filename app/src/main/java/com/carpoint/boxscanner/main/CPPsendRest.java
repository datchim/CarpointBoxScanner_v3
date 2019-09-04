package com.carpoint.boxscanner.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import com.carpoint.boxscanner.R;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class CPPsendRest extends AsyncTask<Void, Boolean, Boolean> {

    private final int BUFFER_SIZE = 16384;
    private final String delimiter = "--";
    private final String boundary = "SwA" + Long.toString(System.currentTimeMillis()) + "SwA";
    private Context mContext;
    private SharedPreferences prefs;
    private AlertDialog mWaitUploadDialog;
    private FileMan f;
    private ArrayList<String> to_send;

    public CPPsendRest(Activity context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        try {
            f = new FileMan(mContext, "");
            ArrayList<String> tmp = f.getAllDocs();
            to_send = new ArrayList<String>();

            for (int i = 0; i < tmp.size(); i++) {
                if (tmp.get(i).startsWith("to_send_")) {
                    to_send.add(tmp.get(i));
                }
            }

            if (to_send.size() == 0)
                return;

            mWaitUploadDialog = new AlertDialog.Builder(context)
                    .setView(context.getLayoutInflater().inflate(R.layout.wait_upload_rest, null))
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    if (mWaitUploadDialog != null)
                                        mWaitUploadDialog.dismiss();
                                }
                            }).create();
            mWaitUploadDialog.setCancelable(true);
            mWaitUploadDialog.show();

            execute();
        } catch (Exception e) {
            Functions.toast(mContext, R.string.msg_error_hhtp);
            Functions.err(e);
        }
    }

    @Override
    protected Boolean doInBackground(Void... bitmaps) {
        try {
            for (int i = 0; i < to_send.size(); i++) {
                f = new FileMan(mContext, to_send.get(i));

                String head = f.getDoc("head.json");
                String answers = f.getDoc("answers.json");
                String errorAnswers = f.getDoc("error answers.json");
                Bitmap sign = f.getBitmap("sign.jpg");

                ArrayList<String> tmp = f.getAllDocs();
                ArrayList<Pair<String, Bitmap>> photos = new ArrayList<>();
                ArrayList<Pair<String, Bitmap>> photosErr = new ArrayList<>();

                for (int j = 0; j < tmp.size(); j++) {
                    if (tmp.get(j).startsWith("photo")) {
                        photos.add(new Pair<String, Bitmap>(tmp.get(j).replaceAll(".jpg", ""), f.getBitmap(tmp.get(j))));
                    }
                    if (tmp.get(j).startsWith("err_photo")) {
                        photosErr.add(new Pair<String, Bitmap>(tmp.get(j).replaceAll(".jpg", ""), f.getBitmap(tmp.get(j))));
                    }
                }

                if (head.length() > 0 && answers.length()>0 && errorAnswers.length()>0) {
                    if (!sendOne(to_send.get(i), new JSONObject(head), answers, errorAnswers, photos, photosErr,sign))
                        return false;
                }

            }
            return true;
        } catch (Exception e) {
            Functions.toast(mContext, R.string.msg_error_hhtp);
            Functions.err(e);
        }
        return false;
    }

    private boolean sendOne(String dirName, JSONObject head, String answers, String errorAnswers, ArrayList<Pair<String, Bitmap>> photos,ArrayList<Pair<String, Bitmap>> photosErr, Bitmap sign) {
        try {
            String url = prefs.getString("url_preference", "");

            head.put("username",
                    prefs.getString("username_preference", ""));
            head.put("password",
                    prefs.getString("passwort_preference", ""));

            if (url.length() == 0) {
                Functions.toast(mContext, R.string.settup);
                return false;
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
            //Write login and form
            writeText(os, "head", head.toString());
            writeText(os, "answers", answers);
            writeText(os, "errors", errorAnswers);

            for (int i = 0; i < photos.size(); i++) {
                String idphoto = photos.get(i).first;
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
                deleteRecursive(new File(Environment.getExternalStorageDirectory() + "/PalletControl/" + dirName));
                return true;
            } else if (con.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Functions.toast(mContext, R.string.msg_error_unauthorized);
            } else {
                Functions.toast(mContext, R.string.msg_error_hhtp);
            }
        } catch (Exception e) {
            Log.e("CP", Arrays.toString(e.getStackTrace()));
        }
        return false;
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


    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        try {
            if (mWaitUploadDialog != null)
                mWaitUploadDialog.dismiss();
            if (result) {
                Functions.toast(mContext,
                        R.string.pdfCreated);
            }
        } catch (Exception e) {
            Functions.toast(mContext, R.string.msg_error_hhtp);
        }
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        if (fileOrDirectory.exists())
            fileOrDirectory.delete();
    }
}
