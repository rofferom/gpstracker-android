package fr.rtwo.gpstracker;

import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.PreferenceFragmentCompat;

public class FragmentAbout extends PreferenceFragmentCompat {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        EditTextPreference pref;

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about);

        pref = (EditTextPreference) findPreference("version");
        pref.setSummary(BuildConfig.GitHash);

        pref = (EditTextPreference) findPreference("build_date");
        pref.setSummary(BuildConfig.BuildDate);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
    }
}
