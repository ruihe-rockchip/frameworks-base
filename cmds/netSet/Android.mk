LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_MODULE := netSet
include $(BUILD_JAVA_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := netSet
LOCAL_SRC_FILES := netSet
LOCAL_MODULE_CLASS := EXECUTABLES
#LOCAL_MODULE_PATH :=/home/liaoqizhen/nfs_root
LOCAL_MODULE_TAGS := debug eng tests optional
include $(BUILD_PREBUILT)

