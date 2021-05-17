package com.dot.updater

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.PowerManager
import android.os.SystemProperties
import android.preference.PreferenceManager
import android.text.SpannableString
import android.text.format.Formatter
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import com.dot.updater.changelog.ChangelogAdapter
import com.dot.updater.controller.UpdaterController
import com.dot.updater.controller.UpdaterService
import com.dot.updater.misc.*
import com.dot.updater.model.ChangelogItem
import com.dot.updater.model.UpdateInfo
import com.dot.updater.model.UpdateStatus
import com.dot.updater.ui.ChangelogSheet
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.IOException
import java.sql.Timestamp
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

typealias onActionChanged = ((action: UpdateView.Action?) -> Unit)?

class UpdateView : LinearLayout {

    private var mAlphaDisabledValue = 0f
    private var mDownloadId: String? = null
    private lateinit var mSelectedDownload: String
    private var mUpdaterController: UpdaterController? = null
    private var mActivity: UpdatesListActivity? = null
    private var infoDialog: AlertDialog? = null

    private var actionListener: onActionChanged = null

    private lateinit var actionCheck: RelativeLayout
    private lateinit var actionCheckButton: RelativeLayout
    private lateinit var actionStart: RelativeLayout
    private lateinit var actionStartButton: RelativeLayout
    private lateinit var actionProgress: RelativeLayout
    private lateinit var actionProgressBar: ProgressBar
    private lateinit var actionProgressText: TextView
    private lateinit var actionProgressStats: TextView
    private lateinit var actionProgressPause: ImageButton
    private lateinit var actionOptions: LinearLayout
    private lateinit var actionCancel: RelativeLayout
    private lateinit var actionResume: RelativeLayout
    private lateinit var actionInstall: RelativeLayout
    private lateinit var actionInstallButton: RelativeLayout
    private lateinit var actionReboot: RelativeLayout
    private lateinit var actionRebootButton: RelativeLayout
    private var chipHeader: View
    private var chipDateCurrent: Chip
    private var chipDateTarget: Chip
    private var update: UpdateInfo? = null
    private var noUpdates: View
    private var moreChangelog: Button
    private var summaryChangelog: TextView
    private var updateContainer: View

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        inflate(context, R.layout.update_layout, this)
        noUpdates = findViewById(R.id.no_updates)
        chipHeader = findViewById(R.id.updateChipHeader)
        chipDateCurrent = findViewById(R.id.chipDateCurrent)
        chipDateTarget = findViewById(R.id.chipDateTarget)
        moreChangelog = findViewById(R.id.fullChangelog)
        summaryChangelog = findViewById(android.R.id.summary)
        updateContainer = findViewById(R.id.update_container)
    }

    fun noUpdates() {
        noUpdates.visibility = VISIBLE
    }

    fun unleashTheBunny(resID: Int) {
        findViewById<TextView>(R.id.easterBunny).setText(resID)
        Handler().postDelayed({ findViewById<TextView>(R.id.easterBunny).setText(R.string.maybe_later) }, 2000)
    }

    fun lateInit() {
        if (mDownloadId == null) {
            actionCheck.visibility = VISIBLE
            noUpdates.visibility = VISIBLE
            return
        }
        update = mUpdaterController!!.getUpdate(mDownloadId)
        if (update == null) {
            // The update was deleted
            actionOptions.visibility = GONE
            actionStart.visibility = VISIBLE
            return
        }
        noUpdates.visibility = GONE
        val activeLayout: Boolean = when (update!!.persistentStatus) {
            UpdateStatus.Persistent.UNKNOWN -> update!!.status == UpdateStatus.STARTING
            UpdateStatus.Persistent.VERIFIED -> update!!.status == UpdateStatus.INSTALLING
            UpdateStatus.Persistent.INCOMPLETE -> true
            else -> throw RuntimeException("Unknown update status")
        }
        chipHeader.visibility = VISIBLE
        updateContainer.visibility = VISIBLE
        val currentDate = createDate(SystemProperties.getLong(Constants.PROP_BUILD_DATE, 0))
        val currentVersion = SystemProperties.get(Constants.PROP_DOT_VERSION)
        val buildDate = createDate(update!!.timestamp)
        val buildVersion = update!!.version
        chipDateCurrent.text = String.format("%s | %s", currentDate, currentVersion)
        chipDateTarget.text = String.format("%s | %s", buildDate, buildVersion)
        if (activeLayout) {
            handleActiveStatus(update!!)
        } else {
            handleNotActiveStatus(update!!)
        }
        parseChangelog()
    }

    private fun createDate(timestamp: Long): CharSequence? {
        return SimpleDateFormat("d MMM yy", Locale.getDefault()).format(Timestamp(timestamp).time * 1000L)
    }

    fun setupControlViews(actionCheck: RelativeLayout, actionStart: RelativeLayout, actionProgress: RelativeLayout, actionOptions: LinearLayout, actionInstall: RelativeLayout, actionReboot: RelativeLayout) {
        this.actionCheck = actionCheck
        this.actionCheckButton = actionCheck.findViewById(R.id.actionCheckButton)
        this.actionStart = actionStart
        this.actionStartButton = actionStart.findViewById(R.id.actionStartButton)
        this.actionProgress = actionProgress
        this.actionProgressBar = actionProgress.findViewById(R.id.updateProgressBar)
        this.actionProgressText = actionProgress.findViewById(R.id.updateProgressText)
        this.actionProgressStats = actionProgress.findViewById(R.id.updateStats)
        this.actionProgressPause = actionProgress.findViewById(R.id.updatePause)
        this.actionOptions = actionOptions
        this.actionCancel = actionOptions.findViewById(R.id.actionCancel)
        this.actionResume = actionOptions.findViewById(R.id.actionResume)
        this.actionInstall = actionInstall
        this.actionInstallButton = actionInstall.findViewById(R.id.actionInstallButton)
        this.actionReboot = actionReboot
        this.actionRebootButton = actionReboot.findViewById(R.id.actionRebootButton)
    }

    private fun initListeners() {
        actionProgressPause.setOnClickListener {
            if (!actionProgressBar.isIndeterminate) {
                mUpdaterController!!.pauseDownload(mDownloadId)
                hideEverythingBut(actionOptions)
                actionOptions.visibility = VISIBLE
            }
        }
        actionStartButton.setOnClickListener {
            mDownloadId?.let { it1 ->
                startDownloadWithWarning(it1)
                hideEverythingBut(actionProgress)
                actionProgress.visibility = VISIBLE
            }
        }
        actionResume.setOnClickListener {
            val update = mUpdaterController!!.getUpdate(mDownloadId)
            val canInstall = Utils.canInstall(update) ||
                    update.file.length() == update.fileSize
            if (canInstall) {
                mUpdaterController!!.resumeDownload(mDownloadId)
                hideEverythingBut(actionProgress)
                actionProgress.visibility = VISIBLE
            } else {
                mActivity!!.showSnackbar(R.string.snack_update_not_installable,
                        Snackbar.LENGTH_LONG)
            }
        }
        actionInstallButton.setOnClickListener {
            val update = mUpdaterController!!.getUpdate(mDownloadId)
            val canInstall = Utils.canInstall(update)
            if (canInstall) {
                getInstallDialog(mDownloadId!!)!!.show()
            } else {
                mActivity!!.showSnackbar(R.string.snack_update_not_installable,
                        Snackbar.LENGTH_LONG)
            }
        }
        actionCancel.setOnClickListener {
            getDeleteDialog(mDownloadId!!).show()
            //cancelInstallationDialog.show()
        }
        actionRebootButton.setOnClickListener {
            val pm = mActivity!!.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.reboot(null)
        }
    }

    private fun hideEverythingBut(view: View) {
        if (view.id != actionCheck.id)
            actionCheck.visibility = View.GONE
        if (view.id != actionInstall.id)
            actionInstall.visibility = View.GONE
        if (view.id != actionOptions.id)
            actionOptions.visibility = View.GONE
        if (view.id != actionProgress.id)
            actionProgress.visibility = View.GONE
        if (view.id != actionReboot.id)
            actionReboot.visibility = View.GONE
    }

    private fun parseChangelog() {
        val list: ArrayList<ChangelogItem> = ArrayList()
        val changelog = update!!.changelog
        if (changelog != null) {
            if (changelog.hasSystem) {
                val item = ChangelogItem()
                item.iconRes = R.drawable.ic_system
                item.title = context.getString(R.string.system)
                item.subtitle = changelog.systemTitle
                item.summary = changelog.systemSummary
                list.add(item)
            }
            if (changelog.hasSecurity) {
                val item = ChangelogItem()
                item.iconRes = R.drawable.ic_security
                item.title = context.getString(R.string.security)
                item.subtitle = changelog.securityTitle
                item.summary = changelog.securitySummary
                list.add(item)
            }
            if (changelog.hasSettings) {
                val item = ChangelogItem()
                item.iconRes = R.drawable.ic_settings
                item.title = context.getString(R.string.settings)
                item.subtitle = changelog.settingsTitle
                item.summary = changelog.settingsSummary
                list.add(item)
            }
            if (changelog.hasMisc) {
                val item = ChangelogItem()
                item.iconRes = R.drawable.ic_misc
                item.title = context.getString(R.string.misc)
                item.subtitle = changelog.miscTitle
                item.summary = changelog.miscSummary
                list.add(item)
            }
            val adapter = ChangelogAdapter(list)
            var summaryChangelogText = ""
            for (item in list) {
                summaryChangelogText += "${item.subtitle}. "
            }
            summaryChangelog.text = summaryChangelogText
            moreChangelog.isEnabled = true
            moreChangelog.setOnClickListener {
                ChangelogSheet().setupChangelogSheet(adapter).show(mActivity!!.supportFragmentManager, "changelog")
            }
        } else {
            summaryChangelog.text = context.getString(R.string.changelog_error)
            moreChangelog.isEnabled = false
            Log.w("Changelog", "is Null")
        }
    }

    fun setDownloadId(downloadId: String?) {
        mDownloadId = downloadId
        lateInit()
        invalidate()
    }

    fun setUpdaterController(updaterController: UpdaterController?) {
        mUpdaterController = updaterController
        initListeners()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        infoDialog?.dismiss()
    }

    fun setActivity(activity: UpdatesActivity?) {
        mActivity = activity
        val tv = TypedValue()
        mActivity!!.theme.resolveAttribute(android.R.attr.disabledAlpha, tv, true)
        mAlphaDisabledValue = tv.float
    }

    private fun handleActiveStatus(update: UpdateInfo) {
        var canDelete = false
        val downloadId = update.downloadId
        when {
            mUpdaterController!!.isDownloading(downloadId) -> {
                actionStart.visibility = GONE
                actionProgress.visibility = VISIBLE
                canDelete = true
                val downloaded = Formatter.formatShortFileSize(mActivity,
                        update.file.length())
                val total = Formatter.formatShortFileSize(mActivity, update.fileSize)
                val percentage = NumberFormat.getPercentInstance().format((
                        update.progress / 100f).toDouble())
                val eta = update.eta
                if (eta > 0) {
                    val etaString: CharSequence = StringGenerator.formatETA(mActivity, eta * 1000)
                    actionProgressStats.text = mActivity!!.getString(
                            R.string.list_download_progress_eta_new, downloaded, total, etaString)
                } else {
                    actionProgressStats.text = mActivity!!.getString(
                            R.string.list_download_progress_new, downloaded, total)
                }
                setButtonAction(actionProgressPause, Action.PAUSE, true)
                actionProgressBar.isIndeterminate = update.status == UpdateStatus.STARTING
                actionProgressBar.progress = update.progress
                actionProgressText.text = "$percentage"
            }
            mUpdaterController!!.isInstallingUpdate(downloadId) -> {
                actionProgress.visibility = VISIBLE
                setButtonAction(actionCancel, Action.CANCEL_INSTALLATION, true)
                val notAB = !mUpdaterController!!.isInstallingABUpdate
                actionProgressStats.setText(if (notAB) R.string.dialog_prepare_zip_message else if (update.finalizing) R.string.finalizing_package else R.string.preparing_ota_first_boot)
                actionProgressBar.isIndeterminate = false
                actionProgressBar.progress = update.installProgress
                actionProgressPause.isEnabled = false
            }
            mUpdaterController!!.isVerifyingUpdate(downloadId) -> {
                actionProgress.visibility = VISIBLE
                setButtonAction(actionInstallButton, Action.INSTALL, false)
                actionProgressStats.setText(R.string.list_verifying_update)
                actionProgressBar.isIndeterminate = true
                actionProgressPause.isEnabled = false
            }
            else -> {
                actionProgress.visibility = GONE
                canDelete = true
                setButtonAction(actionResume, Action.RESUME, !isBusy)
                val downloaded = StringGenerator.bytesToMegabytes(mActivity,
                        update.file.length())
                val total = Formatter.formatShortFileSize(mActivity, update.fileSize)
                val percentage = NumberFormat.getPercentInstance().format((
                        update.progress / 100f).toDouble())
                actionProgressStats.text = mActivity!!.getString(R.string.list_download_progress_new,
                        downloaded, total)
                actionProgressText.text = "$percentage"
                actionProgressBar.isIndeterminate = false
                actionProgressBar.progress = update.progress
            }
        }
        onLongClickListener = getLongClickListener(update, canDelete, this)
        actionStart.visibility = GONE
    }

    private fun handleNotActiveStatus(update: UpdateInfo) {
        val downloadId = update.downloadId
        if (mUpdaterController!!.isWaitingForReboot(downloadId)) {
            onLongClickListener = getLongClickListener(update, false, this)
            setButtonAction(actionRebootButton, Action.REBOOT, true)
        } else if (update.persistentStatus == UpdateStatus.Persistent.VERIFIED) {
            onLongClickListener = getLongClickListener(update, true, this)
            setButtonAction(actionInstallButton, if (Utils.canInstall(update)) Action.INSTALL else Action.DELETE, !isBusy)
        } else if (!Utils.canInstall(update)) {
            onLongClickListener = getLongClickListener(update, false, this)
            //setButtonAction(Action.INFO, downloadId, !isBusy)
        } else {
            onLongClickListener = getLongClickListener(update, false, this)
            setButtonAction(actionStartButton, Action.DOWNLOAD, !isBusy)
        }
        val fileSize = Formatter.formatShortFileSize(mActivity, update.fileSize)
        //mBuildSize.text = fileSize
        actionProgress.visibility = GONE
        actionStart.visibility = VISIBLE
    }

    private fun startDownloadWithWarning(downloadId: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(mActivity)
        val warn = preferences.getBoolean(Constants.PREF_MOBILE_DATA_WARNING, true)
        if (Utils.isOnWifiOrEthernet(mActivity) || !warn) {
            mUpdaterController!!.startDownload(downloadId)
            return
        }
        val checkboxView = LayoutInflater.from(mActivity).inflate(R.layout.checkbox_view, null)
        val checkbox = checkboxView.findViewById<View>(R.id.checkbox) as CheckBox
        checkbox.setText(R.string.checkbox_mobile_data_warning)
        AlertDialog.Builder(mActivity!!)
                .setTitle(R.string.update_on_mobile_data_title)
                .setMessage(R.string.update_on_mobile_data_message)
                .setView(checkboxView)
                .setPositiveButton(R.string.action_download
                ) { dialog: DialogInterface?, which: Int ->
                    if (checkbox.isChecked) {
                        preferences.edit()
                                .putBoolean(Constants.PREF_MOBILE_DATA_WARNING, false)
                                .apply()
                        mActivity!!.supportInvalidateOptionsMenu()
                    }
                    mUpdaterController!!.startDownload(downloadId)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun setButtonAction(target: View, action: Action, enabled: Boolean) {
        actionListener?.invoke(action)
        target.isEnabled = enabled
        when (action) {
            Action.DOWNLOAD -> {
                actionStart.visibility = VISIBLE
            }
            Action.RESUME -> {
                actionOptions.visibility = VISIBLE
            }
            Action.INSTALL -> {
                actionInstall.visibility = VISIBLE
            }
            Action.INFO -> {
                //button!!.setText(R.string.action_info)
                //button.isEnabled = enabled
                //clickListener = if (enabled) OnClickListener { view: View? -> showInfoDialog() } else null
            }
            Action.DELETE -> {

            }
            Action.CANCEL_INSTALLATION -> {
                actionOptions.visibility = VISIBLE
                //button!!.setText(R.string.action_cancel)
                //button.isEnabled = enabled
                //clickListener = if (enabled) OnClickListener { view: View? -> cancelInstallationDialog.show() } else null
            }
            Action.REBOOT -> {

            }
            Action.PAUSE -> {
                actionCheck.visibility = GONE
                actionStart.visibility = GONE
            }
        }
        // button!!.alpha = if (enabled) 1f else mAlphaDisabledValue

    }

    private val isBusy: Boolean
        get() = (mUpdaterController!!.hasActiveDownloads() || mUpdaterController!!.isVerifyingUpdate
                || mUpdaterController!!.isInstallingUpdate)

    private fun getDeleteDialog(downloadId: String): AlertDialog.Builder {
        return AlertDialog.Builder(mActivity!!)
                .setTitle(R.string.confirm_delete_dialog_title)
                .setMessage(R.string.confirm_delete_dialog_message)
                .setPositiveButton(android.R.string.ok
                ) { dialog: DialogInterface?, which: Int ->
                    mUpdaterController!!.pauseDownload(downloadId)
                    mUpdaterController!!.deleteUpdate(downloadId)
                    actionOptions.visibility = GONE
                    actionStart.visibility = VISIBLE
                }
                .setNegativeButton(android.R.string.cancel, null)
    }

    private fun getLongClickListener(update: UpdateInfo,
                                     canDelete: Boolean, anchor: View?): OnLongClickListener {
        return OnLongClickListener { view: View? ->
            startActionMode(update, canDelete, anchor)
            true
        }
    }

    private fun getInstallDialog(downloadId: String): AlertDialog.Builder? {
        if (!isBatteryLevelOk) {
            val resources = mActivity!!.resources
            val message = resources.getString(R.string.dialog_battery_low_message_pct,
                    resources.getInteger(R.integer.battery_ok_percentage_discharging),
                    resources.getInteger(R.integer.battery_ok_percentage_charging))
            return AlertDialog.Builder(mActivity!!)
                    .setTitle(R.string.dialog_battery_low_title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
        }
        val update = mUpdaterController!!.getUpdate(downloadId)
        val resId: Int
        resId = try {
            if (Utils.isABUpdate(update.file)) {
                R.string.apply_update_dialog_message_ab
            } else {
                R.string.apply_update_dialog_message
            }
        } catch (e: IOException) {
            Log.e(TAG, "Could not determine the type of the update")
            return null
        }
        val buildDate = StringGenerator.getDateLocalizedUTC(mActivity,
                DateFormat.MEDIUM, update.timestamp)
        val buildInfoText = mActivity!!.getString(R.string.list_build_version_date,
                BuildInfoUtils.getBuildVersion(), buildDate)
        return AlertDialog.Builder(mActivity!!)
                .setTitle(R.string.apply_update_dialog_title)
                .setMessage(mActivity!!.getString(resId, buildInfoText,
                        mActivity!!.getString(android.R.string.ok)))
                .setPositiveButton(android.R.string.ok
                ) { _: DialogInterface?, _: Int -> Utils.triggerUpdate(mActivity, downloadId) }
                .setNegativeButton(android.R.string.cancel, null)
    }

    private val cancelInstallationDialog: AlertDialog.Builder
        get() = AlertDialog.Builder(mActivity!!)
                .setMessage(R.string.cancel_installation_dialog_message)
                .setPositiveButton(android.R.string.ok
                ) { dialog: DialogInterface?, which: Int ->
                    val intent = Intent(mActivity, UpdaterService::class.java)
                    intent.action = UpdaterService.ACTION_INSTALL_STOP
                    mActivity!!.startService(intent)
                }
                .setNegativeButton(android.R.string.cancel, null)

    @SuppressLint("RestrictedApi")
    private fun startActionMode(update: UpdateInfo, canDelete: Boolean, anchor: View?) {
        mSelectedDownload = update.downloadId
        val wrapper = ContextThemeWrapper(mActivity,
                R.style.AppTheme_PopupMenuOverlapAnchor)
        val popupMenu = PopupMenu(wrapper, anchor!!, Gravity.NO_GRAVITY,
                R.attr.actionOverflowMenuStyle, 0)
        popupMenu.inflate(R.menu.menu_action_mode)
        val menu = popupMenu.menu as MenuBuilder
        menu.findItem(R.id.menu_delete_action).isVisible = canDelete
        menu.findItem(R.id.menu_copy_url).isVisible = update.availableOnline
        menu.findItem(R.id.menu_export_update).isVisible = update.persistentStatus == UpdateStatus.Persistent.VERIFIED
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_delete_action -> {
                    getDeleteDialog(update.downloadId).show()
                    return@setOnMenuItemClickListener true
                }
                R.id.menu_copy_url -> {
                    Utils.addToClipboard(mActivity,
                            mActivity!!.getString(R.string.label_download_url),
                            update.downloadUrl,
                            mActivity!!.getString(R.string.toast_download_url_copied))
                    return@setOnMenuItemClickListener true
                }
                R.id.menu_export_update -> {
                    // TODO: start exporting once the permission has been granted
                    val hasPermission = PermissionsUtils.checkAndRequestStoragePermission(
                            mActivity, 0)
                    if (hasPermission) {
                        exportUpdate(update)
                    }
                    return@setOnMenuItemClickListener true
                }
            }
            false
        }
        val helper = MenuPopupHelper(wrapper, menu, anchor)
        helper.show()
    }

    private fun exportUpdate(update: UpdateInfo) {
        var dest = File(Utils.getExportPath(mActivity), update.name)
        if (dest.exists()) {
            dest = Utils.appendSequentialNumber(dest)
        }
        val intent = Intent(mActivity, ExportUpdateService::class.java)
        intent.action = ExportUpdateService.ACTION_START_EXPORTING
        intent.putExtra(ExportUpdateService.EXTRA_SOURCE_FILE, update.file)
        intent.putExtra(ExportUpdateService.EXTRA_DEST_FILE, dest)
        mActivity!!.startService(intent)
    }

    private fun showInfoDialog() {
        val messageString = String.format(StringGenerator.getCurrentLocale(mActivity),
                mActivity!!.getString(R.string.blocked_update_dialog_message),
                Utils.getUpgradeBlockedURL(mActivity))
        val message = SpannableString(messageString)
        Linkify.addLinks(message, Linkify.WEB_URLS)
        if (infoDialog != null) {
            infoDialog!!.dismiss()
        }
        infoDialog = AlertDialog.Builder(mActivity!!)
                .setTitle(R.string.blocked_update_dialog_title)
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(message)
                .show()
        val textView = infoDialog?.findViewById<View>(android.R.id.message) as TextView?
        textView!!.movementMethod = LinkMovementMethod.getInstance()
    }

    private val isBatteryLevelOk: Boolean
        get() {
            val intent = mActivity!!.registerReceiver(null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (!intent!!.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)) {
                return true
            }
            val percent = (100f * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) /
                    intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)).roundToInt()
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val required = if (plugged and BATTERY_PLUGGED_ANY != 0) mActivity!!.resources.getInteger(R.integer.battery_ok_percentage_charging) else mActivity!!.resources.getInteger(R.integer.battery_ok_percentage_discharging)
            return percent >= required
        }

    enum class Action {
        DOWNLOAD, PAUSE, RESUME, INSTALL, INFO, DELETE, CANCEL_INSTALLATION, REBOOT
    }

    companion object {
        private const val TAG = "UpdateView"
        private const val BATTERY_PLUGGED_ANY = (BatteryManager.BATTERY_PLUGGED_AC
                or BatteryManager.BATTERY_PLUGGED_USB
                or BatteryManager.BATTERY_PLUGGED_WIRELESS)
    }
}