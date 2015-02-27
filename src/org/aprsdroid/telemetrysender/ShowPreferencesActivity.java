package org.aprsdroid.telemetrysender;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import java.util.List;

public class ShowPreferencesActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add a button to the header list.
/*        if (hasHeaders()) {
            Button button = new Button(this);
            button.setText("Some action");
            setListFooter(button);
        }*/
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return Prefs1Fragment.class.getName().equals(fragmentName)
//            || Prefs1FragmentInner.class.getName().equals(fragmentName)
            || Prefs2Fragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows the preferences for the first header.
     */
    public static class Prefs1Fragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Make sure default values are applied.  In a real app, you would
            // want this in a shared function that is used to retrieve the
            // SharedPreferences wherever they are needed.
//            PreferenceManager.setDefaultValues(getActivity(), R.xml.advanced_preferences, false);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences_frag1);
        }
    }

    /**
     * This fragment contains a second-level set of preference that you
     * can get to by tapping an item in the first preferences fragment.
     */
/*    public static class Prefs1FragmentInner extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Can retrieve arguments from preference XML.
            Log.i("args", "Arguments: " + getArguments());

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences_frag1inner);
        }
    }*/

    /**
     * This fragment shows the preferences for the second header.
     */
    public static class Prefs2Fragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Can retrieve arguments from headers XML.
            Log.i("args", "Arguments: " + getArguments());

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences_frag2);

            // dynamically add sensors to preferences
            PreferenceCategory targetCategory = (PreferenceCategory)findPreference("pref_sensor_settings");
            SensorsObject s = SensorsObject.getSingletonObject();
            Sensor sensors[] = s.getSensors();
            for (Sensor sensor: sensors) {
                CheckBoxPreference checkBoxPreference = new CheckBoxPreference(this.getActivity());
                checkBoxPreference.setKey("pref_sensor_sensor_type_" + sensor.getType());
                checkBoxPreference.setTitle("Sensor " + sensor.getType());
                checkBoxPreference.setSummary(sensor.getName());
                checkBoxPreference.setChecked(false);
                targetCategory.addPreference(checkBoxPreference);
            }
        }
    }
}
