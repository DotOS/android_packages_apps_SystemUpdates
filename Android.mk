LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_USE_AAPT2 := true

LOCAL_STATIC_ANDROID_LIBRARIES := \
	androidx.recyclerview_recyclerview \
	androidx.cardview_cardview \
	androidx.preference_preference \
	androidx.appcompat_appcompat \
	androidx.annotation_annotation \
	com.google.android.material_material \
	androidx.legacy_legacy-support-v4 \
	androidx.viewpager_viewpager


LOCAL_STATIC_JAVA_LIBRARIES := \
	guava \
	gson

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
	gson:libs/gson-2.8.2.jar

LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/res

LOCAL_PACKAGE_NAME := SystemUpdates
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_PRIVILEGED_MODULE := true
LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)


include $(CLEAR_VARS)
LOCAL_MODULE := UpdaterStudio
LOCAL_MODULE_CLASS := FAKE
LOCAL_MODULE_SUFFIX := -timestamp
updater_system_deps := $(call java-lib-deps,framework)
updater_system_libs_path := $(abspath $(LOCAL_PATH))/system_libs

include $(BUILD_SYSTEM)/base_rules.mk

.PHONY: copy_updater_system_deps
copy_updater_system_deps: $(updater_system_deps)
	$(hide) mkdir -p $(updater_system_libs_path)
	$(hide) rm -rf $(updater_system_libs_path)/*.jar
	$(hide) cp $(updater_system_deps) $(updater_system_libs_path)/framework.jar

$(LOCAL_BUILT_MODULE): copy_updater_system_deps
	$(hide) echo "Fake: $@"
	$(hide) mkdir -p $(dir $@)
	$(hide) touch $@