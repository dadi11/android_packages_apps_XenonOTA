LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

ifneq ($(ANDROID_BUILD_TOP),)
    $(shell cp -u $(ANDROID_BUILD_TOP)/out/ota_conf $(LOCAL_PATH)/app/src/main/assets/)
endif

LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := XenonOTA
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

ifeq ($(TARGET_BUILD_APPS),)
support_library_root_dir := frameworks/support
else
support_library_root_dir := prebuilts/sdk/current/support
endif

LOCAL_SRC_FILES := $(call all-java-files-under, app/src/main)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/app/src/main/res \
    $(support_library_root_dir)/design/res \
    $(support_library_root_dir)/transition/res \
    $(support_library_root_dir)/v7/appcompat/res \
    $(support_library_root_dir)/v7/cardview/res

LOCAL_ASSET_DIR := $(LOCAL_PATH)/app/src/main/assets

LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true
LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages android.support.design \
    --extra-packages android.support.transition \
    --extra-packages android.support.constraint \
    --extra-packages android.support.v7.appcompat \
    --extra-packages android.support.v7.cardview

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    android-support-design \
    android-support-v13 \
    android-support-v7-appcompat \
    android-support-v7-cardview \
    android-support-constraint-layout-solver \
    android-support-transition

LOCAL_STATIC_JAVA_AAR_LIBRARIES += \
    android-support-constraint-layout

LOCAL_PROGUARD_FLAG_FILES := \
    proguard.flags \
    ../../../frameworks/support/core-ui/proguard-rules.pro \
    ../../../frameworks/support/design/proguard-rules.pro

include $(BUILD_PACKAGE)
