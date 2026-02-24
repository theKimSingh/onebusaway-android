package org.onebusaway.android.ui;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.onebusaway.android.BuildConfig;
import org.onebusaway.android.R;
import org.onebusaway.android.app.Application;
import org.onebusaway.android.io.ObaAnalytics;
import org.onebusaway.android.io.PlausibleAnalytics;
import org.onebusaway.android.io.elements.ObaRegion;
import org.onebusaway.android.provider.ObaContract;
import org.onebusaway.android.region.ObaRegionsTask;
import org.onebusaway.android.travelbehavior.io.coroutines.FirebaseDataPusher;
import org.onebusaway.android.util.BackupUtils;
import org.onebusaway.android.util.BuildFlavorUtils;
import org.onebusaway.android.util.ShowcaseViewUtils;
import org.onebusaway.android.util.UIUtils;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceFragmentCompat;

import static org.onebusaway.android.util.UIUtils.setAppTheme;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        UIUtils.setupActionBar(this);
//         Display the fragment as the main content
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new MySettingsFragment())
                    .commit();
        }
    }

    public static class MySettingsFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener,
            SharedPreferences.OnSharedPreferenceChangeListener, ObaRegionsTask.Callback {
        private static final String TAG = "PreferencesActivity";

        public static final String SHOW_CHECK_REGION_DIALOG = ".checkRegionDialog";

        public static final int REQUEST_CODE_RESTORE_BACKUP = 1234;
        public static final int REQUEST_CODE_SAVE_BACKUP = 1199;

        Preference mPreference;

        Preference mLeftHandMode;

        Preference mCustomApiUrlPref;

        Preference mCustomOtpApiUrlPref;

        Preference mAnalyticsPref;

        CheckBoxPreference mTravelBehaviorPref;

        Preference mHideAlertsPref;

        Preference mTutorialPref;

        Preference mDonatePref;

        Preference mPoweredByObaPref;

        Preference mAboutPref;

        Preference mSaveBackup;

        Preference mRestoreBackup;

        Preference pushFirebaseData;

        Preference resetDonationTimestamps;

        boolean mAutoSelectInitialValue;

        boolean mOtpCustomAPIUrlChanged = false;
        //Save initial value so we can compare to current value in onDestroy()

        ListPreference preferredUnits;
        ListPreference preferredTempUnits;

        ListPreference mThemePref;
        ListPreference mapMode;

        private FirebaseAnalytics mFirebaseAnalytics;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Points to your XML file in res/xml/preferences.xml
            setPreferencesFromResource(R.xml.preferences, rootKey);
//            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
            setTheme();

//            setProgressBarIndeterminate(true);

            mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());

            mPreference = findPreference(getString(R.string.preference_key_region));
            mPreference.setOnPreferenceClickListener(this);

            mLeftHandMode = findPreference(getString(R.string.preference_key_left_hand_mode));
            mLeftHandMode.setOnPreferenceChangeListener(this);

            mSaveBackup = findPreference(getString(R.string.preference_key_save_backup));
            mSaveBackup.setOnPreferenceClickListener(this);

            mRestoreBackup = findPreference(getString(R.string.preference_key_restore_backup));
            mRestoreBackup.setOnPreferenceClickListener(this);

            mCustomApiUrlPref = findPreference(getString(R.string.preference_key_oba_api_url));
            mCustomApiUrlPref.setOnPreferenceChangeListener(this);

            mCustomOtpApiUrlPref = findPreference(getString(R.string.preference_key_otp_api_url));
            mCustomOtpApiUrlPref.setOnPreferenceChangeListener(this);

            mAnalyticsPref = findPreference(getString(R.string.preferences_key_analytics));
            mAnalyticsPref.setOnPreferenceChangeListener(this);

//        mTravelBehaviorPref = (CheckBoxPreference) findPreference(getString(R.string.preferences_key_travel_behavior));
//        mTravelBehaviorPref.setOnPreferenceChangeListener(this);

//        if (!TravelBehaviorUtils.isTravelBehaviorActiveInRegion() ||
//                (!TravelBehaviorUtils.allowEnrollMoreParticipantsInStudy() &&
//                        !TravelBehaviorUtils.isUserParticipatingInStudy())) {
//            PreferenceCategory aboutCategory = (PreferenceCategory)
//                    findPreference(getString(R.string.preferences_category_about));
//            aboutCategory.removePreference(mTravelBehaviorPref);
//        } else {
//            mTravelBehaviorPref.setChecked(TravelBehaviorUtils.isUserParticipatingInStudy());
//        }

            pushFirebaseData = findPreference(getString(R.string.preference_key_push_firebase_data));
            pushFirebaseData.setOnPreferenceClickListener(this);

            resetDonationTimestamps = findPreference(getString(R.string.preference_key_reset_donation_timestamps));
            resetDonationTimestamps.setOnPreferenceClickListener(this);

            mHideAlertsPref = findPreference(getString(R.string.preference_key_hide_alerts));
            mHideAlertsPref.setOnPreferenceChangeListener(this);

            mTutorialPref = findPreference(getString(R.string.preference_key_tutorial));
            mTutorialPref.setOnPreferenceClickListener(this);

            mDonatePref = findPreference(getString(R.string.preferences_key_donate));
            mDonatePref.setOnPreferenceClickListener(this);

            mPoweredByObaPref = findPreference(getString(R.string.preferences_key_powered_by_oba));
            mPoweredByObaPref.setOnPreferenceClickListener(this);

            mAboutPref = findPreference(getString(R.string.preferences_key_about));
            mAboutPref.setOnPreferenceClickListener(this);

            mapMode = (ListPreference) findPreference(getString(R.string.preference_key_map_mode));
            mapMode.setOnPreferenceChangeListener(this);

            SharedPreferences settings = Application.getPrefs();
            mAutoSelectInitialValue = settings
                    .getBoolean(getString(R.string.preference_key_auto_select_region), true);

            preferredUnits = (ListPreference) findPreference(
                    getString(R.string.preference_key_preferred_units));

            preferredTempUnits = (ListPreference) findPreference(
                    getString(R.string.preference_key_preferred_temperature_units));

            mThemePref = (ListPreference) findPreference(
                    getString(R.string.preference_key_app_theme));
            mThemePref.setOnPreferenceChangeListener(this);

            settings.registerOnSharedPreferenceChangeListener(this);

            PreferenceScreen preferenceScreen = getPreferenceScreen();
            // Hide any preferences that shouldn't be shown if the region is hard-coded via build flavor
            if (BuildConfig.USE_FIXED_REGION) {
                PreferenceCategory regionCategory = (PreferenceCategory)
                        findPreference(getString(R.string.preferences_category_location));
                regionCategory.removeAll();
                preferenceScreen.removePreference(regionCategory);
                PreferenceCategory advancedCategory = (PreferenceCategory)
                        findPreference(getString(R.string.preferences_category_advanced));
                Preference experimentalRegion = findPreference(
                        getString(R.string.preference_key_experimental_regions));
                advancedCategory.removePreference(experimentalRegion);
            }

            // If the Android version is Oreo (8.0) hide "Notification" preference
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Preference pref = findPreference(getString(R.string.preference_key_notifications));
                if (pref != null) {
                    PreferenceGroup parent = pref.getParent();
                    if (parent != null) {
                        parent.removePreference(pref);
                    }
                }
            }

            // If the Android version is lower than Nougat (7.0) or equal to and above Pie (9.0) hide "Share trip logs" preference
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.N) || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)) {
                getPreferenceScreen().removePreference(findPreference(getString(R.string.preferences_key_user_debugging_logs_category)));
            }

            // If its the OBA brand flavor, then show the "Donate" preference and hide "Powered by OBA"
            PreferenceCategory aboutCategory = (PreferenceCategory)
                    findPreference(getString(R.string.preferences_category_about));
            if (BuildFlavorUtils.isOBABuildFlavor()) {
                aboutCategory.removePreference(mPoweredByObaPref);
            } else {
                // Its not the OBA brand flavor, then hide the "Donate" preference and show "Powered by OBA"
                aboutCategory.removePreference(mDonatePref);
            }

            boolean showCheckRegionDialog = getActivity().getIntent().getBooleanExtra(SHOW_CHECK_REGION_DIALOG, false);
            if (showCheckRegionDialog) {
                showCheckRegionDialog();
            }

            onAddCustomRegion();

            // Update preference summaries that contain the app name placeholder
            updateBrandedPreferenceSummaries();
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.getKey().equals("preference_key_notification_settings")) {
                openNotificationSettings();
                return true;
            }
            return super.onPreferenceTreeClick(preference);
        }

        /**
         * Enable Notification Selecting (for backwards compatibility)
         */
        private void openNotificationSettings() {
            Intent intent = new Intent();
            String packageName = requireContext().getPackageName();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26+
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName);
            } else {
                // API 21-25 (Bulletproof version)
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", packageName);
                intent.putExtra("app_uid", requireContext().getApplicationInfo().uid);
                // Added for API 24/25 transition support
                intent.putExtra("android.provider.extra.APP_PACKAGE", packageName);
            }

            try {
                startActivity(intent);
            } catch (Exception e) {
                // Safety fallback to general settings if the specific app page is unavailable
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            changePreferenceSummary(getString(R.string.preference_key_region));
            changePreferenceSummary(getString(R.string.preference_key_preferred_units));
            changePreferenceSummary(getString(R.string.preference_key_preferred_temperature_units));
            changePreferenceSummary(getString(R.string.preference_key_app_theme));
            changePreferenceSummary(getString(R.string.preference_key_otp_api_url));
            changePreferenceSummary(getString(R.string.preference_key_map_mode));


            // Remove preferences for notifications if no trip planning
            ObaRegion obaRegion = Application.get().getCurrentRegion();
            if (obaRegion != null && TextUtils.isEmpty(obaRegion.getOtpBaseUrl())) {
                PreferenceCategory notifications = (PreferenceCategory)
                        findPreference(getString(R.string.preference_key_notifications));
                Preference tripPlan = findPreference(
                        getString(R.string.preference_key_trip_plan_notifications));

                if (notifications != null && tripPlan != null) {
                    notifications.removePreference(tripPlan);
                }
            }
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            // CRITICAL: Unregister the listener to prevent the crash
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        private void showCheckRegionDialog() {
            ObaRegion obaRegion = Application.get().getCurrentRegion();
            if (obaRegion == null) {
                return;
            }

            new MaterialAlertDialogBuilder(getContext())
                    .setTitle(getString(R.string.preference_region_dialog_title))
                    .setMessage(getString(R.string.preference_region_dialog_message, obaRegion.getName()))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
        }

        /**
         * Updates preference summaries that contain the app name placeholder (%1$s).
         * This enables white-label branding by injecting the app name into static XML summaries.
         */
        private void updateBrandedPreferenceSummaries() {
            String appName = getString(R.string.app_name);

            // Update API server title
            if (mCustomApiUrlPref != null) {
                mCustomApiUrlPref.setTitle(getString(R.string.preferences_oba_api_servername_title, appName));
            }

            // Update notification preferences
            Preference soundPref = findPreference(getString(R.string.preference_key_notification_sound));
            if (soundPref != null) {
                soundPref.setSummary(getString(R.string.preferences_preferred_sound_summary, appName));
            }

            Preference vibratePref = findPreference(getString(R.string.preference_key_preference_vibrate_allowed));
            if (vibratePref != null) {
                vibratePref.setSummary(getString(R.string.preferences_preferred_vibration_summary, appName));
            }

            // Update backup preferences
            if (mRestoreBackup != null) {
                mRestoreBackup.setSummary(getString(R.string.preferences_restore_summary, appName));
            }

            // Update about/donate preferences
            if (mDonatePref != null) {
                mDonatePref.setSummary(getString(R.string.preferences_donate_summary, appName));
            }

            if (mPoweredByObaPref != null) {
                mPoweredByObaPref.setTitle(getString(R.string.preferences_powered_by_oba_title, appName));
            }

            // Update destination logs preference
            Preference destLogsPref = findPreference(getString(R.string.preferences_key_user_share_destination_logs));
            if (destLogsPref != null) {
                destLogsPref.setSummary(getString(R.string.preferences_user_share_destination_logs_summary, appName));
            }
        }

        /**
         * Changes the summary of a preference based on a given preference key
         *
         * @param preferenceKey preference key that triggers a change in summary
         */
        private void changePreferenceSummary(String preferenceKey) {
            // Change the current region summary and server API URL summary
            if (preferenceKey.equalsIgnoreCase(getString(R.string.preference_key_region))
                    || preferenceKey.equalsIgnoreCase(getString(R.string.preference_key_oba_api_url))) {
                if (Application.get().getCurrentRegion() != null) {
                    mPreference.setSummary(Application.get().getCurrentRegion().getName());
                    mCustomApiUrlPref
                            .setSummary(getString(R.string.preferences_oba_api_servername_summary, getString(R.string.app_name)));
                    String customOtpApiUrl = Application.get().getCustomOtpApiUrl();
                    if (!TextUtils.isEmpty(customOtpApiUrl)) {
                        mCustomOtpApiUrlPref.setSummary(customOtpApiUrl);
                    } else {
                        mCustomOtpApiUrlPref
                                .setSummary(getString(R.string.preferences_otp_api_servername_summary));
                    }
                } else {
                    mPreference.setSummary(getString(R.string.preferences_region_summary_custom_api));
                    mCustomApiUrlPref.setSummary(Application.get().getCustomApiUrl());
                }
            } else if (preferenceKey
                    .equalsIgnoreCase(getString(R.string.preference_key_preferred_units))) {
                preferredUnits.setSummary(preferredUnits.getValue());
            } else if (preferenceKey
                    .equalsIgnoreCase(getString(R.string.preference_key_app_theme))) {
                mThemePref.setSummary(mThemePref.getValue());
            } else if (preferenceKey
                    .equalsIgnoreCase(getString(R.string.preference_key_otp_api_url))) {
                String customOtpApiUrl = Application.get().getCustomOtpApiUrl();
                if (!TextUtils.isEmpty(customOtpApiUrl)) {
                    mCustomOtpApiUrlPref.setSummary(customOtpApiUrl);
                } else {
                    mCustomOtpApiUrlPref.setSummary(
                            getString(R.string.preferences_otp_api_servername_summary));
                }
                Application.get().setUseOldOtpApiUrlVersion(false);
            } else if (preferenceKey
                    .equalsIgnoreCase(getString(R.string.preference_key_preferred_temperature_units))) {
                preferredTempUnits.setSummary(preferredTempUnits.getValue());
            } else if (preferenceKey.equalsIgnoreCase(getString(R.string.preference_key_map_mode))) {
                mapMode.setSummary(mapMode.getValue());
            }
        }

        @Override
        public boolean onPreferenceClick(Preference pref) {
            Log.d(TAG, "preference - " + pref.getKey());
            if (pref.equals(mPreference)) {
                RegionsActivity.start(getContext());
            } else if (pref.equals(mTutorialPref)) {
                ObaAnalytics.reportUiEvent(mFirebaseAnalytics,
                        Application.get().getPlausibleInstance(),
                        PlausibleAnalytics.REPORT_PREFERENCES_EVENT_URL,
                        getString(R.string.analytics_label_button_press_tutorial),
                        null);
                ShowcaseViewUtils.resetAllTutorials(getContext());
                NavHelp.goHome(getContext(), true);
            } else if (pref.equals(mDonatePref)) {
                startActivity(Application.getDonationsManager().buildOpenDonationsPageIntent());
            } else if (pref.equals(mPoweredByObaPref)) {
                ObaAnalytics.reportUiEvent(mFirebaseAnalytics,
                        Application.get().getPlausibleInstance(),
                        PlausibleAnalytics.REPORT_PREFERENCES_EVENT_URL,
                        getString(R.string.analytics_label_button_press_powered_by_oba),
                        null);
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.powered_by_oba_url)));
                startActivity(intent);
            } else if (pref.equals(mAboutPref)) {
                ObaAnalytics.reportUiEvent(mFirebaseAnalytics,
                        Application.get().getPlausibleInstance(),
                        PlausibleAnalytics.REPORT_PREFERENCES_EVENT_URL,
                        getString(R.string.analytics_label_button_press_about),
                        null);
                AboutActivity.start(getContext());
            } else if (pref.equals(mSaveBackup)) {
                BackupUtils.createBackupFile(getActivity());
            } else if (pref.equals(mRestoreBackup)){
                BackupUtils.selectBackupFile(getActivity());
            } else if (pref.equals(pushFirebaseData)) {
                // Try to push firebase data to the server
                FirebaseDataPusher pusher = new FirebaseDataPusher();
                pusher.push(getContext());
            } else if (pref.equals(resetDonationTimestamps)) {
                Application.getDonationsManager().setDonationRequestReminderDate(null);
                Application.getDonationsManager().setDonationRequestDismissedDate(null);
            }
            return true;
        }


        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if(resultCode != RESULT_OK) return;
            Uri uri = data.getData();
            if(uri != null){
                if (requestCode == REQUEST_CODE_RESTORE_BACKUP) {
                    BackupUtils.restore(getContext(), uri);
                }else if(requestCode == REQUEST_CODE_SAVE_BACKUP){
                    BackupUtils.save(getContext(),uri);
                }
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference.equals(mCustomApiUrlPref) && newValue instanceof String) {
                String apiUrl = (String) newValue;

                if (!TextUtils.isEmpty(apiUrl)) {
                    boolean validUrl = validateUrl(apiUrl);
                    if (!validUrl) {
                        Toast.makeText(getContext(), getString(R.string.custom_api_url_error, getString(R.string.app_name)),
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    //User entered a custom API Url, so set the region info to null
                    Application.get().setCurrentRegion(null);
                    Log.d(TAG, "User entered new API URL, set region to null.");
                } else {
                    //User cleared the API URL preference value, so re-initialize regions
                    Log.d(TAG, "User entered blank API URL, re-initializing regions...");
                    NavHelp.goHome(getContext(), false);
                }
            }
            if (preference.equals(mCustomOtpApiUrlPref) && newValue instanceof String) {
                String apiUrl = (String) newValue;

                if (!TextUtils.isEmpty(apiUrl)) {
                    boolean validUrl = validateUrl(apiUrl);
                    if (!validUrl) {
                        Toast.makeText(getContext(), getString(R.string.custom_otp_api_url_error),
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                mOtpCustomAPIUrlChanged = true;
            } else if (preference.equals(mAnalyticsPref) && newValue instanceof Boolean) {
                Boolean isAnalyticsActive = (Boolean) newValue;
                //Report if the analytics turns off, just before shared preference changed
                ObaAnalytics.setSendAnonymousData(mFirebaseAnalytics, isAnalyticsActive);
            } else if (preference.equals(mTravelBehaviorPref) && newValue instanceof Boolean) {
//            Boolean activateTravelBehaviorCollection = (Boolean) newValue;
//            if (activateTravelBehaviorCollection) {
//                new TravelBehaviorManager(this, getApplicationContext()).
//                        registerTravelBehaviorParticipant(true);
//            } else {
//                showOptOutDialog();
//                return false;
//            }
            } else if (preference.equals(mLeftHandMode) && newValue instanceof Boolean) {
                Boolean isLeftHandEnabled = (Boolean) newValue;
                //Report if left handed mode is turned on, just before shared preference changed
                ObaAnalytics.setLeftHanded(mFirebaseAnalytics, isLeftHandEnabled);
            } else if (preference.equals(mHideAlertsPref) && newValue instanceof Boolean) {
                Boolean hideAlerts = (Boolean) newValue;
                if (hideAlerts) {
                    ObaContract.ServiceAlerts.hideAllAlerts();
                }
            } else if (preference.equals(mThemePref) && newValue instanceof String) {
                String theme = ((String) newValue);
                setAppTheme(theme);
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            }
            return true;
        }

        /**
         * Shows the dialog to explain user is choosing to opt out of travel behavior research study
         * Currently disabled see ticket https://github.com/OneBusAway/onebusaway-android/issues/1240
         */
//    private void showOptOutDialog() {
//        androidx.appcompat.app.MaterialAlertDialogBuilder builder = new androidx.appcompat.app.MaterialAlertDialogBuilder(this)
//                .setTitle(R.string.travel_behavior_dialog_opt_out_title)
//                .setMessage(R.string.travel_behavior_dialog_opt_out_message)
//                .setCancelable(false)
//                .setPositiveButton(R.string.ok,
//                        (dialog, which) -> {
//                            // Remove user from study
//                            new TravelBehaviorManager(this, getApplicationContext()).
//                                    stopCollectingData();
//                            TravelBehaviorManager.optOutUser();
//                            TravelBehaviorManager.optOutUserOnServer();
//                            // Change preference
//                            mTravelBehaviorPref.setChecked(false);
//                            PreferenceUtils.saveBoolean(getString(R.string.preferences_key_travel_behavior), false);
//                        }
//                )
//                .setNegativeButton(R.string.cancel,
//                        (dialog, which) -> {
//                            // No-op
//                        }
//                );
//        builder.create().show();
//    }

        @Override
        public void onStop() {
            if (isAdded() && getContext() != null) {
                SharedPreferences settings = Application.getPrefs();
                String key = getString(R.string.preference_key_auto_select_region);
                boolean currentValue = settings.getBoolean(key, true);

                if (currentValue && !mAutoSelectInitialValue) {
                    Log.d(TAG, "User re-enabled auto-select regions pref, auto-selecting via Home Activity...");
                    NavHelp.goHome(getActivity(), false);
                } else if (mOtpCustomAPIUrlChanged) {
                    NavHelp.goHome(getActivity(), false);
                }
            }
            super.onStop();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            SharedPreferences settings = Application.getPrefs();
            // Listening to changes to a custom Preference doesn't seem to work, so we can listen to changes to the shared pref value instead
            if (key.equals(getString(R.string.preference_key_experimental_regions))) {
                boolean experimentalServers = settings
                        .getBoolean(getString(R.string.preference_key_experimental_regions), false);
                Log.d(TAG, "Experimental regions shared preference changed to " + experimentalServers);

            /*
            Force a refresh of the regions list, but don't using blocking progress dialog
            inside the ObaRegionsTask AsyncTask.
            We need to use our own Action Bar progress bar here so its linked to this activity,
            which will survive orientation changes.
            */
                if (getActivity() != null) {
                    getActivity().setProgressBarIndeterminateVisibility(true);
                }
                List<ObaRegionsTask.Callback> callbacks = new ArrayList<>();
                callbacks.add(this);
                ObaRegionsTask task = new ObaRegionsTask(getContext(), callbacks, true, false);
                task.execute();

                // Wait to change the region preference description until the task callback
                //Analytics
                if (experimentalServers) {
                    ObaAnalytics.reportUiEvent(mFirebaseAnalytics,
                            Application.get().getPlausibleInstance(),
                            PlausibleAnalytics.REPORT_PREFERENCES_EVENT_URL,
                            getString(R.string.analytics_label_button_press_experimental_on),
                            null);
                } else {
                    ObaAnalytics.reportUiEvent(mFirebaseAnalytics,
                            Application.get().getPlausibleInstance(),
                            PlausibleAnalytics.REPORT_PREFERENCES_EVENT_URL,
                            getString(R.string.analytics_label_button_press_experimental_off),
                            null);
                }
            } else if (key.equals(getString(R.string.preference_key_oba_api_url))) {
                // Change the region preference description to show we're not using a region
                changePreferenceSummary(key);
            } else if (key.equals(getString(R.string.preference_key_otp_api_url))) {
                // Change the otp url preference description
                changePreferenceSummary(key);
            } else if (key.equalsIgnoreCase(getString(R.string.preference_key_preferred_units))) {
                // Change the preferred units description
                changePreferenceSummary(key);
            } else if (key.equalsIgnoreCase(getString(R.string.preference_key_app_theme))) {
                // Change the app theme preference description
                changePreferenceSummary(key);
                // Update the app theme
                setAppTheme(settings.getString(key, getString(R.string.preferences_app_theme_option_system_default)));
            } else if (key.equalsIgnoreCase(getString(R.string.preference_key_auto_select_region))) {
                //Analytics
                boolean autoSelect = settings
                        .getBoolean(getString(R.string.preference_key_auto_select_region), true);
                if (autoSelect) {
                    ObaAnalytics.reportUiEvent(mFirebaseAnalytics,
                            Application.get().getPlausibleInstance(),
                            PlausibleAnalytics.REPORT_PREFERENCES_EVENT_URL,
                            getString(R.string.analytics_label_button_press_auto),
                            null);
                } else {
                    ObaAnalytics.reportUiEvent(mFirebaseAnalytics,
                            Application.get().getPlausibleInstance(),
                            PlausibleAnalytics.REPORT_PREFERENCES_EVENT_URL,
                            getString(R.string.analytics_label_button_press_manual),
                            null);
                }
            } else if (key.equalsIgnoreCase(getString(R.string.preferences_key_analytics))) {
                Boolean isAnalyticsActive = settings.getBoolean(Application.get().
                        getString(R.string.preferences_key_analytics), Boolean.TRUE);
                //Report if the analytics turns on, just after shared preference changed
                ObaAnalytics.setSendAnonymousData(mFirebaseAnalytics, isAnalyticsActive);
            } else if (key.equalsIgnoreCase(getString(R.string.preference_key_arrival_info_style))) {
                // Change the arrival info description
                changePreferenceSummary(key);
            } else if (key.equalsIgnoreCase(getString(R.string.preference_key_show_negative_arrivals))) {
                boolean showDepartedBuses = settings.getBoolean(Application.get().
                        getString(R.string.preference_key_show_negative_arrivals), Boolean.FALSE);
                ObaAnalytics.setShowDepartedVehicles(mFirebaseAnalytics, showDepartedBuses);
            }else if (key.equalsIgnoreCase(getString(R.string.preference_key_preferred_temperature_units))) {
                // Change the preferred temp unit description
                changePreferenceSummary(key);
            } else if (key.equalsIgnoreCase(getString(R.string.preference_key_map_mode))) {
                // Change map mode description
                changePreferenceSummary(key);
            }
        }

        /**
         * Returns true if the provided apiUrl could be a valid URL, false if it could not
         *
         * @param apiUrl the URL to validate
         * @return true if the provided apiUrl could be a valid URL, false if it could not
         */
        private boolean validateUrl(String apiUrl) {
            if (!apiUrl.startsWith("http")) {
                // Assume HTTPS scheme if none is provided
                apiUrl = getString(R.string.https_prefix) + apiUrl;
            }

            URL url = null;
            try {
                // URI.parse() doesn't tell us if the scheme is missing, so use URL() instead (#126)
                url = new URL(apiUrl);
            } catch (MalformedURLException e) {
                return false;
            }

            if (url.getHost().equals("localhost")) {
                return true;
            } else {
                return Patterns.WEB_URL.matcher(apiUrl).matches();
            }
        }


        //
        // Region Task Callback
        //
        public void onRegionTaskFinished(boolean currentRegionChanged) {
            if (getActivity() != null) {
                getActivity().setProgressBarIndeterminateVisibility(false);
            }

            if (currentRegionChanged) {
                // If region was auto-selected, show user the region we're using
                if (Application.getPrefs()
                        .getBoolean(getString(R.string.preference_key_auto_select_region), true)
                        && Application.get().getCurrentRegion() != null) {
                    Toast.makeText(getContext(),
                            getString(R.string.region_region_found,
                                    Application.get().getCurrentRegion().getName()),
                            Toast.LENGTH_LONG
                    ).show();
                }

                // Update the preference summary to show the newly selected region
                changePreferenceSummary(getString(R.string.preference_key_region));

                // Since the current region was updated as a result of enabling/disabling experimental servers, go home
                NavHelp.goHome(getContext(), false);
            }
        }

        /**
         * The function will process deep links used for adding custom regions
         */
        void onAddCustomRegion() {
            Uri deepLink = getActivity().getIntent().getData();
            if(deepLink == null){
                return;
            }
            String obaCustomUrl = deepLink.getQueryParameter("oba-url");
            String otpCustomURl = deepLink.getQueryParameter("otp-url");

            // onPreferenceChange is responsible for checking changes if it's valid
            if (obaCustomUrl != null && onPreferenceChange(mCustomApiUrlPref, obaCustomUrl)) {
                Application.get().setCustomApiUrl(obaCustomUrl);
            }

            if (otpCustomURl != null && onPreferenceChange(mCustomOtpApiUrlPref, otpCustomURl)) {
                Application.get().setCustomOtpApiUrl(otpCustomURl);
            }
            Intent i = new Intent(getContext(), HomeActivity.class);
            startActivity(i);
            getActivity().finish();
        }

        /**
         * Set the theme based on the current night mode
         */
        private void setTheme() {
            Preference themePref = findPreference(getString(R.string.preference_key_app_theme));
            if (themePref != null) {
                themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String themeValue = (String) newValue;
                    int mode;
                    if (themeValue.equals("dark")) {
                        mode = AppCompatDelegate.MODE_NIGHT_YES;
                    } else if (themeValue.equals("light")) {
                        mode = AppCompatDelegate.MODE_NIGHT_NO;
                    } else {
                        mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    }
                    AppCompatDelegate.setDefaultNightMode(mode);
                    return true;
                });
            }
        }
    }
}