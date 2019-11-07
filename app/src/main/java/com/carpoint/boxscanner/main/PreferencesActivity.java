package com.carpoint.boxscanner.main;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.carpoint.boxscanner.R;

public class PreferencesActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    //carpoint keys
    public static final String CARPOINT_url = "url_preference";
    private EditTextPreference editText_url;


    private static SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        editText_url = (EditTextPreference) getPreferenceScreen().findPreference(CARPOINT_url);

        editText_url.setSummary(sharedPreferences.getString(CARPOINT_url, ""));
    }

    /**
     * Interface definition for a callback to be invoked when a shared
     * preference is changed. Sets summary text for the app's preferences. Summary text values show the
     * current settings for the values.
     *
     * @param sharedPreferences
     *            the Android.content.SharedPreferences that received the change
     * @param key
     *            the key of the preference that was changed, added, or removed
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        if(key.equals(CARPOINT_url)) {
            editText_url.setSummary(sharedPreferences.getString(key, ""));
        }

    }

    /**
     * Sets up initial preference summary text
     * values and registers the OnSharedPreferenceChangeListener.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Called when Activity is about to lose focus. Unregisters the
     * OnSharedPreferenceChangeListener.
     */
    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}