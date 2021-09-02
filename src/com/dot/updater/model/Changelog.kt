package com.dot.updater.model

import java.io.Serializable

class Changelog : Serializable {
    var hasSystem = false
    var hasSecurity = false
    var hasSettings = false
    var hasMisc = false
    var hasDevice = false

    var systemTitle: String = ""
    var systemSummary: String = ""
    var securityTitle: String = ""
    var securitySummary: String = ""
    var settingsTitle: String = ""
    var settingsSummary: String = ""
    var miscTitle: String = ""
    var miscSummary: String = ""
    var deviceChangelog: String? = null
}