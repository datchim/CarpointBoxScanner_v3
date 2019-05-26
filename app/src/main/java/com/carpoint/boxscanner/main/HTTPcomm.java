package com.carpoint.boxscanner.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

import com.carpoint.boxscanner.MainActivity;
import com.carpoint.boxscanner.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class HTTPcomm extends AsyncTask<Object, Boolean, Object> {

    private final int BUFFER_SIZE = 16384;
    private final String delimiter = "--";
    private final String boundary = "SwA" + Long.toString(System.currentTimeMillis()) + "SwA";
    private Context mContext;
    private SharedPreferences prefs;
    private OnFinish onFinish;
    private OnFinishBitmap onFinishBitmap;
    private String  endUrl;


    //getErroListProtocol; getQuestions
    public HTTPcomm(Context context, OnFinish onFinish, boolean isQuestions ) {
        mContext = context;
        this.execute();
        this.onFinish = onFinish;
        if (isQuestions){
            endUrl ="getQuestions";
        }else
            endUrl ="getProtoErrorList";

    }

    //getPhoto
    public HTTPcomm(Context context,int id_protocol, String photoName, OnFinishBitmap onFinish ) {
        mContext = context;
        this.execute(id_protocol,photoName);
        this.onFinishBitmap = onFinish;
        this.endUrl = "getPhoto";

    }
    //getProtocol
    public HTTPcomm(Context context, JSONObject product, OnFinish onFinish ) {
        mContext = context;
        this.execute(product);
        this.onFinish = onFinish;
        this.endUrl = "getProtocol";
    }
    //resolve
    public HTTPcomm(Context context, JSONArray ids, Bitmap bitmapSign,OnFinish onFinish ) {
        mContext = context;
        this.execute(ids,bitmapSign);
        this.onFinish = onFinish;
        this.endUrl = "resolve";
    }
    //sendProtocol
    public HTTPcomm(Context context, JSONObject head, JSONArray answers, JSONArray errorAnswers, OnFinish onFinish, ArrayList<Pair<Integer, Bitmap>> photos, ArrayList<Pair<String, Bitmap>> photosError, Bitmap sign) {
        mContext = context;
        this.execute(head,answers,errorAnswers,photos,photosError,sign);
        this.onFinish = onFinish;
        this.endUrl = "sendProtocol";
    }

    @Override
    protected String doInBackground(Object... params) {
        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            String url = prefs.getString("url_preference", "");

            JSONObject customer = new JSONObject();
            customer.put("username",
                    prefs.getString("username_preference", ""));
            customer.put("password",
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
            url += "api/" + endUrl;

            HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.connect();

            OutputStream os = con.getOutputStream();
            //Write login, form and error answers



            boolean isPhoto = false;
            switch (endUrl){
                case "getProtoErrorList":
                    writeText(os, "login", customer.toString());
                    os.write((delimiter + boundary + delimiter + "\r\n").getBytes());
                    break;

                case "getQuestions":
                    writeText(os, "login", customer.toString());
                    os.write((delimiter + boundary + delimiter + "\r\n").getBytes());
                    if (customer != null) {
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                                con.getOutputStream());
                        outputStreamWriter.write(customer.toString());
                        outputStreamWriter.flush();
                    }
                break;

                case "getPhoto":
                    JSONObject photo = new JSONObject();
                    photo.accumulate("id_protocol",params[0]);
                    photo.accumulate("filename",params[1]);

                    writeText(os, "login", customer.toString());
                    writeText(os, "photo", photo.toString());
                    os.write((delimiter + boundary + delimiter + "\r\n").getBytes());
                break;

                case "getProtocol":
                    writeText(os, "login", customer.toString());
                    writeText(os, "ids", params[0].toString());
                    os.write((delimiter + boundary + delimiter + "\r\n").getBytes());
                break;

                case "resolve":
                    writeText(os, "login", customer.toString());
                    writeText(os, "errors", params[0].toString());
                    writeBitmap(os, "sign", (Bitmap)params[1], 100);
                    os.write((delimiter + boundary + delimiter + "\r\n").getBytes());
                break;

                case "sendProtocol":
                    JSONObject head  = (JSONObject)params[0];
                    head.put("username",
                            prefs.getString("username_preference", ""));
                    head.put("password",
                            prefs.getString("passwort_preference", ""));
                    writeText(os, "head", head.toString());
                    writeText(os, "answers",params[1].toString());
                    writeText(os, "errors", params[2].toString());
                    ArrayList<Pair<Integer, Bitmap>> photos= (ArrayList<Pair<Integer, Bitmap>> ) params[3];
                    for (int i = 0; i < photos.size(); i++) {
                        String idphoto = photos.get(i).first.toString();
                        Bitmap bitmap = photos.get(i).second;
                        writeBitmap(os, "photo_" + idphoto, bitmap, 75);
                    }
                    ArrayList<Pair<String, Bitmap>> photosErr = (ArrayList<Pair<String, Bitmap>> ) params[4];
                    for (int i = 0; i < photosErr.size(); i++) {
                        String idphoto = photosErr.get(i).first;
                        Bitmap bitmap = photosErr.get(i).second;
                        writeBitmap(os, "err_photo_" + idphoto, bitmap, 75);
                    }
                    Bitmap sign = (Bitmap) params[5];
                    writeBitmap(os, "sign",sign, 100);
                 break;
            }




            // Get data



            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                if (!isPhoto) {
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
                }else{
                    Bitmap b = BitmapFactory.decodeStream(con.getInputStream());
                   // return b;
                }
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
    protected void onPostExecute(Object result) {
        super.onPostExecute(result);
            String resultString = "";
            Bitmap resultBitmap ;
            if (result instanceof  String)
            resultString = (String) result;
            else
                resultBitmap = (Bitmap) result;
//            Log.e("result",result);
            switch (endUrl){

               case "getProtoErrorList":
                   try {
                       if (onFinish != null)
                           onFinish.onResult(resultString);
                   } catch (Exception e) {
                       Functions.toast(mContext,R.string.protocol_not_found);
                   }
               break;

               case "getQuestions":
                   try {
                       if (result != null) {
                           try {
                               JSONObject o = new JSONObject(resultString);
                               Functions.toast(mContext, R.string.questionsDownloaded);
                               FileMan file = new FileMan(mContext, "");
                               file.saveDoc("questions.json", o);
                           } catch (JSONException e) {
                               Functions.toast(mContext, R.string.msg_error_hhtp);
                           }
                       }

                       if (onFinish != null)
                           onFinish.onResult(resultString);
                   } catch (Exception e) {
                       Functions.toast(mContext, R.string.msg_error_hhtp);
                   }
                break;

                case "getProtocol":
                    try {
                        if (onFinish != null)
                            onFinish.onResult(resultString);
                    } catch (Exception e) {
                        Functions.toast(mContext,R.string.protocol_not_found);
                    }
                 break;

                case "resolve":
                    try {
                        if (onFinish != null)
                            onFinish.onResult(resultString);
                    } catch (Exception e) {
                        Functions.toast(mContext,R.string.protocol_not_found);
                    }
                break;

                case "sendProtocol":
                    try {
                        boolean save = false;
                        if (result != null) {
                            JSONObject obj = new JSONObject(resultString);
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

                        /*if (save) {
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
                        }*/
                    } catch (Exception e) {
                        Functions.err(e);
                        if (onFinish != null)
                            onFinish.onResult(null);
                    }
                break ;

            }
    }
   // bitmap
    /*@Override
    protected void onPostExecute(Bitmap result) {
        super.onPostExecute(result);
        try {
            if (onFinishBitmap != null)
                onFinishBitmap.onResult(result);
        } catch (Exception e) {
            Functions.toast(mContext,R.string.protocol_not_found);
        }
    }*/




    public interface OnFinish {
        void onResult(String response);

    }

    public interface OnFinishBitmap {
        void onResult(Bitmap response);
    }



}
