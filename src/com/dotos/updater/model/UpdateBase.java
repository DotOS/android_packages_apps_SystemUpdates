/*
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (C) 2018 The DotOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dotos.updater.model;

public class UpdateBase implements UpdateBaseInfo {

    private String mName;
    private String mDownloadUrl;
    private String mDownloadId;
    private long mTimestamp;
    private String mType;
    private String mVersion;
    private long mFileSize;
    private String c_System;
    private String c_Settings;
    private String c_Misc;
    private String c_Device;
    private String c_SecPatch;
    private String maintainerName, maintainerXDA, maintainerOther, maintainerTelegram, maintainerDevice, maintainerImageUrl;

    public UpdateBase() {
    }

    public UpdateBase(UpdateBaseInfo update) {
        mName = update.getName();
        mDownloadUrl = update.getDownloadUrl();
        mDownloadId = update.getDownloadId();
        mTimestamp = update.getTimestamp();
        mType = update.getType();
        mVersion = update.getVersion();
        mFileSize = update.getFileSize();
        c_System = update.getSystemChangelog();
        c_Settings = update.getSettingsChangelog();
        c_Misc = update.getMiscChangelog();
        c_Device = update.getDeviceChangelog();
        c_SecPatch = update.getSecurityPatchChangelog();
        maintainerName = update.getMaintainerName();
        maintainerOther = update.getMaintainerOther();
        maintainerXDA = update.getMaintainerXDA();
        maintainerTelegram = update.getMaintainerTelegram();
        maintainerDevice = update.getMaintainerDevice();
        maintainerImageUrl = update.getMaintainerImageUrl();
    }

    @Override
    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    @Override
    public String getDownloadId() {
        return mDownloadId;
    }

    public void setDownloadId(String downloadId) {
        mDownloadId = downloadId;
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    @Override
    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

    @Override
    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    @Override
    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        mDownloadUrl = downloadUrl;
    }

    @Override
    public long getFileSize() {
        return mFileSize;
    }

    @Override
    public String getSystemChangelog() {
        return c_System;
    }

    public void setC_System(String c_System) {
        this.c_System = c_System;
    }

    public void setC_Settings(String c_Settings) {
        this.c_Settings = c_Settings;
    }

    public void setC_Device(String c_Device) {
        this.c_Device = c_Device;
    }

    public void setC_Misc(String c_Misc) {
        this.c_Misc = c_Misc;
    }

    public void setC_SecPatch(String c_SecPatch) {
        this.c_SecPatch = c_SecPatch;
    }

    @Override
    public String getSettingsChangelog() {
        return c_Settings;
    }

    @Override
    public String getDeviceChangelog() {
        return c_Device;
    }

    @Override
    public String getMiscChangelog() {
        return c_Misc;
    }

    @Override
    public String getSecurityPatchChangelog() {
        return c_SecPatch;
    }

    @Override
    public String getMaintainerName() {
        return maintainerName;
    }

    @Override
    public String getMaintainerXDA() {
        return maintainerXDA;
    }

    @Override
    public String getMaintainerOther() {
        return maintainerOther;
    }

    @Override
    public String getMaintainerTelegram() {
        return maintainerTelegram;
    }

    @Override
    public String getMaintainerDevice() {
        return maintainerDevice;
    }

    @Override
    public String getMaintainerImageUrl() {
        return maintainerImageUrl;
    }

    public void setMaintainerImageUrl(String maintainerImageUrl) {
        this.maintainerImageUrl = maintainerImageUrl;
    }

    public void setMaintainerDevice(String maintainerDevice) {
        this.maintainerDevice = maintainerDevice;
    }

    public void setMaintainerTelegram(String maintainerTelegram) {
        this.maintainerTelegram = maintainerTelegram;
    }

    public void setMaintainerOther(String maintainerOther) {
        this.maintainerOther = maintainerOther;
    }

    public void setMaintainerName(String maintainerName) {
        this.maintainerName = maintainerName;
    }

    public void setMaintainerXDA(String maintainerXDA) {
        this.maintainerXDA = maintainerXDA;
    }

    public void setFileSize(long fileSize) {
        mFileSize = fileSize;
    }
}
