package top.linesoft.open2share;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.kiylx.m3preference.ui.BaseSettingsFragment;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setIconHide(false);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit();
        AppBarLayout appBarLayout = findViewById(R.id.appbar_layout);
        appBarLayout.setStatusBarForeground(
                MaterialShapeDrawable.createWithElevationOverlay(this, 16f));
        appBarLayout.setStatusBarForegroundColor(MaterialColors.getColor(appBarLayout,
                com.google.android.material.R.attr.colorSecondaryContainer));
        edge2edge();
    }

    public static class SettingsFragment extends BaseSettingsFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

        private List<Preference> getAll(PreferenceGroup preferenceGroup) {
            final List<Preference> result = new ArrayList<>();
            PreferenceGroup screen;
            if (preferenceGroup == null) {
                screen = getPreferenceManager().getPreferenceScreen();
            } else {
                screen = preferenceGroup;
            }
            final int preferenceCount = screen.getPreferenceCount();
            for (int i = 0; i < preferenceCount; i++) {
                final Preference preference = screen.getPreference(i);
                if (preference instanceof PreferenceGroup) {
                    List<Preference> tmp = getAll((PreferenceGroup) preference);
                    result.addAll(tmp);
                }else{
                    result.add(preference);
                }
            }
            return result;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (preference.getKey().equals("about")) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.about_dialogue_title)
                        .setMessage(R.string.about_dialogue_msg)
                        .setPositiveButton(R.string.ok, null)
                        .create()
                        .show();
            }
            return false;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            for (Preference preference : getAll(null)) {
                preference.setOnPreferenceChangeListener(this);
                preference.setOnPreferenceClickListener(this);
            }
        }

        /**
         * Called when a preference has been changed by the user. This is called before the state
         * of the preference is about to be updated and before the state is persisted.
         *
         * @param preference The changed preference
         * @param newValue   The new value of the preference
         * @return {@code true} to update the state of the preference with the new value
         */
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference.getKey().equals("hide_icon")) {
                PackageManager pm = requireContext().getPackageManager();
                ComponentName hideComponentName = new ComponentName(requireContext(), "top.linesoft.open2share.hide_icon");
                ComponentName unhideComponentName = new ComponentName(requireActivity(), "top.linesoft.open2share.unhide_icon");
                if ((Boolean) newValue) {

                    MaterialAlertDialogBuilder mDialogBuilder = new MaterialAlertDialogBuilder(requireActivity());
                    mDialogBuilder.setTitle(R.string.warn)
                            .setMessage(R.string.hide_tips)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                pm.setComponentEnabledSetting(hideComponentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                                pm.setComponentEnabledSetting(unhideComponentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                ((SwitchPreferenceCompat) preference).setChecked(true);
                            }).setNegativeButton(R.string.no, null).create().show();
                    return false;


                } else {
                    pm.setComponentEnabledSetting(hideComponentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    pm.setComponentEnabledSetting(unhideComponentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    return true;
                }

            } else if (preference.getKey().equals("use_file_uri")) {
                if ((Boolean) newValue) {
                    MaterialAlertDialogBuilder mDialogBuilder = new MaterialAlertDialogBuilder(requireActivity());
                    mDialogBuilder.setTitle(R.string.warn)
                            .setMessage(R.string.open_file_uri_msg)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                ((SwitchPreferenceCompat) preference).setChecked(true);
                            }).setNegativeButton(R.string.no, (dialog, which) -> {
                                ((SwitchPreferenceCompat) preference).setChecked(false);
                            }).create().show();
                    return false;
                } else {
                    return true;
                }
            }
            return true;
        }
    }
}