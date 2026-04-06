LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libtermux-bootstrap
LOCAL_SRC_FILES := termux-bootstrap-zip.S termux-bootstrap.c

# Required for Android 15+ (16KB page size)
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384

include $(BUILD_SHARED_LIBRARY)
