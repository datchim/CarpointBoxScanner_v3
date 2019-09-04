package com.carpoint.boxscanner.main;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.Tag;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONObject;

public class FileMan {
	private final int BUFFER_SIZE = 16384, MAX_ERR_SIZE = 1048576;
	private File database;

	public FileMan(Context con, String folder) {
		try {
			database = new File(Environment.getExternalStorageDirectory() + "/PalletControl/"+folder);
			if (!database.exists())
				database.mkdirs();
		} catch (Exception e) {
			Functions.err(e);
		}
	}

	public boolean saveDoc(String name, JSONArray json) {
		File f = new File(database, name);
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(f),
					BUFFER_SIZE);
			out.write(json.toString());
			out.close();
		} catch (Exception e) {
			Functions.err(e);
		}
		return f.exists();
	}


	public boolean saveDoc(String name, JSONObject json) {
		if (name.isEmpty())
			return false;
		File f = new File(database, name);
		try {
			Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();

			JsonParser parser = new JsonParser();
			JsonObject jsonPretyPrint = parser.parse(json.toString()).getAsJsonObject();


			BufferedWriter out = new BufferedWriter(new FileWriter(new File(
					database, name)), BUFFER_SIZE);

			out.write(gsonBuilder.toJson(jsonPretyPrint));
			out.close();
			Log.d("Questions","uloženy.");
		} catch (Exception e) {
			Functions.err(e);
		}
		return f.exists();
	}

	public boolean saveBitmap(String name, Bitmap bitmap) {
		File f = new File(database, name);
		try {
			FileOutputStream out = new FileOutputStream(f);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out);

			out.close();
		} catch (Exception e) {
			Functions.err(e);
		}
		return f.exists();
	}

	public boolean fileExists(String name) {
		File f = new File(database, name);
		return f.exists();
	}

	public Bitmap getBitmap(String name) {
		try {
			return BitmapFactory.decodeFile(database+"/"+name);
		} catch (Exception e) {
			Functions.err(e);
		}
		return null;
	}

	public void renameFile(String name, String renameTo) {
		try {
			File f = new File(database, name);
			if (f.exists())
				f.renameTo(new File(database, renameTo));
		} catch (Exception e) {
			Functions.err(e);
		}
	}

	public String getDoc(String name) {
		try {
			File f = new File(database, name);
			if (!f.exists() || f.length() == 0)
				return "";

			String text = "";
			int read;
			char[] buffer = new char[BUFFER_SIZE];

			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(f)));
			while (true) {
				read = br.read(buffer, 0, BUFFER_SIZE);
				if (read == -1)
					break;
				text += new String(buffer, 0, read);
				if (read < BUFFER_SIZE)
					break;
			}
			br.close();

			return text;
		} catch (Exception e) {
			Functions.err(e);
			return "";
		}
	}

	public ArrayList<String> getAllDocs() {
		try {
			return new ArrayList<String>(Arrays.asList(database.list()));
		} catch (Exception e) {
			Functions.err(e);
			return new ArrayList<String>();
		}
	}

}
