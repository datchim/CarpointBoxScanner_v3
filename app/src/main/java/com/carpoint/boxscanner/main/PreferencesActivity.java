package com.carpoint.boxscanner.main;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.carpoint.boxscanner.R;

public class PreferencesActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    // Preference keys not carried over from ZXing project
    public static final String KEY_CONTINUOUS_PREVIEW = "preference_capture_continuous";
    public static final String KEY_PAGE_SEGMENTATION_MODE = "preference_page_segmentation_mode";
    public static final String KEY_OCR_ENGINE_MODE = "preference_ocr_engine_mode";
    public static final String KEY_CHARACTER_BLACKLIST = "preference_character_blacklist";
    public static final String KEY_CHARACTER_WHITELIST = "preference_character_whitelist";
    public static final String KEY_TOGGLE_LIGHT = "preference_toggle_light";

    // Preference keys carried over from ZXing project
    public static final String KEY_AUTO_FOCUS = "preferences_auto_focus";
    public static final String KEY_DISABLE_CONTINUOUS_FOCUS = "preferences_disable_continuous_focus";
    public static final String KEY_NOT_OUR_RESULTS_SHOWN = "preferences_not_our_results_shown";
    public static final String KEY_REVERSE_IMAGE = "preferences_reverse_image";
    public static final String KEY_PLAY_BEEP = "preferences_play_beep";
    public static final String KEY_VIBRATE = "preferences_vibrate";

    //carpoint keys
    public static final String CARPOINT_url = "url_preference";
    public static final String CARPOINT_username = "username_preference";
    public static final String CARPOINT_password = "passwort_preference";

    private EditTextPreference editText_url;
    private EditTextPreference editText_username;
    private EditTextPreference editText_password;


    private static SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);



        editText_url = (EditTextPreference) getPreferenceScreen().findPreference(CARPOINT_url);
        editText_username = (EditTextPreference) getPreferenceScreen().findPreference(CARPOINT_username);
        editText_password = (EditTextPreference) getPreferenceScreen().findPreference(CARPOINT_password);

        editText_url.setSummary(sharedPreferences.getString(CARPOINT_url, ""));
        editText_username.setSummary(sharedPreferences.getString(CARPOINT_username, ""));

        String Password=sharedPreferences.getString(CARPOINT_password, "");
        String shownpwd="";
        if(Password.length()>0) shownpwd="******";
        editText_password.setSummary(shownpwd);


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
        } else if (key.equals(CARPOINT_username)) {
            editText_username.setSummary(sharedPreferences.getString(key, ""));
        } else if (key.equals(CARPOINT_password)) {
            String Password=sharedPreferences.getString(key, "");
            String shownpwd="";
            if(Password.length()>0) shownpwd="******";
            editText_password.setSummary(shownpwd);
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