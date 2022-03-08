package com.dot.systemupdates.ui

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemProperties
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.dot.systemupdates.R
import com.dot.systemupdates.UpdatesCheckReceiver
import com.dot.systemupdates.controller.UpdaterService
import com.dot.systemupdates.misc.Constants
import com.dot.systemupdates.misc.Utils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

@SuppressLint("UseSwitchCompatOrMaterialCode")
class PreferenceSheet : BottomSheetDialogFragment() {

    private var prefs: SharedPreferences? = null

    private var mUpdaterService: UpdaterService? = null
    
    private lateinit var preferencesAbPerfMode: Switch
    private lateinit var preferencesAutoDeleteUpdates: Switch
    private lateinit var preferencesMobileDataWarning: Switch
    private lateinit var preferencesUpdateRecovery: Switch
    private lateinit var preferencesAutoUpdatesCheckInterval: Spinner

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.preferences_dialog, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            preferencesAbPerfMode = findViewById(R.id.preferences_ab_perf_mode)
            preferencesAutoDeleteUpdates = findViewById(R.id.preferences_auto_delete_updates)
            preferencesMobileDataWarning = findViewById(R.id.preferences_mobile_data_warning)
            preferencesUpdateRecovery = findViewById(R.id.preferences_update_recovery)
            preferencesAutoUpdatesCheckInterval = findViewById(R.id.preferences_auto_updates_check_interval)
        }
        if (!Utils.isABDevice())
            preferencesAbPerfMode.visibility = View.GONE

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        preferencesAutoUpdatesCheckInterval.setSelection(Utils.getUpdateCheckSetting(requireContext()))
        preferencesAutoDeleteUpdates.isChecked =
            prefs!!.getBoolean(Constants.PREF_AUTO_DELETE_UPDATES, false)
        preferencesMobileDataWarning.isChecked =
            prefs!!.getBoolean(Constants.PREF_MOBILE_DATA_WARNING, true)
        preferencesAbPerfMode.isChecked =
            prefs!!.getBoolean(Constants.PREF_AB_PERF_MODE, false)

        if (resources.getBoolean(R.bool.config_hideRecoveryUpdate)) {
            // Hide the update feature if explicitly requested.
            // Might be the case of A-only devices using prebuilt vendor images.
            preferencesUpdateRecovery.visibility = View.GONE
        } else if (Utils.isRecoveryUpdateExecPresent()) {
            preferencesUpdateRecovery.isChecked =
                SystemProperties.getBoolean(Constants.UPDATE_RECOVERY_PROPERTY, false)
        } else {
            // There is no recovery updater script in the device, so the feature is considered
            // forcefully enabled, just to avoid users to be confused and complain that
            // recovery gets overwritten. That's the case of A/B and recovery-in-boot devices.
            preferencesUpdateRecovery.isChecked = true
            preferencesUpdateRecovery.setOnTouchListener(object : View.OnTouchListener {
                private var forcedUpdateToast: Toast? = null
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    if (forcedUpdateToast != null) {
                        forcedUpdateToast!!.cancel()
                    }
                    forcedUpdateToast = Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_forced_update_recovery), Toast.LENGTH_SHORT
                    )
                    forcedUpdateToast?.show()
                    return true
                }
            })
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        prefs!!.edit()
            .putInt(
                Constants.PREF_AUTO_UPDATES_CHECK_INTERVAL,
                preferencesAutoUpdatesCheckInterval.selectedItemPosition
            )
            .putBoolean(Constants.PREF_AUTO_DELETE_UPDATES, preferencesAutoDeleteUpdates.isChecked)
            .putBoolean(Constants.PREF_MOBILE_DATA_WARNING, preferencesMobileDataWarning.isChecked)
            .putBoolean(Constants.PREF_AB_PERF_MODE, preferencesAbPerfMode.isChecked)
            .apply()

        if (Utils.isUpdateCheckEnabled(requireContext())) {
            UpdatesCheckReceiver.scheduleRepeatingUpdatesCheck(requireContext())
        } else {
            UpdatesCheckReceiver.cancelRepeatingUpdatesCheck(requireContext())
            UpdatesCheckReceiver.cancelUpdatesCheck(requireContext())
        }

        if (Utils.isABDevice()) {
            val enableABPerfMode: Boolean = preferencesAbPerfMode.isChecked
            mUpdaterService?.updaterController?.setPerformanceMode(enableABPerfMode)
        }
        if (Utils.isRecoveryUpdateExecPresent()) {
            val enableRecoveryUpdate: Boolean = preferencesUpdateRecovery.isChecked
            SystemProperties.set(
                Constants.UPDATE_RECOVERY_PROPERTY,
                enableRecoveryUpdate.toString()
            )
        }
        super.onDismiss(dialog)
    }

    fun setupPreferenceSheet(updaterService: UpdaterService): PreferenceSheet {
        this.mUpdaterService = updaterService
        return this
    }

}
