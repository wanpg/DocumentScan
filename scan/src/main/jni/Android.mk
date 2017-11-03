LOCAL_PATH := $(call my-dir)

#Prebuilt libraries
include $(CLEAR_VARS)
LOCAL_MODULE := opencv_java3

ARCH_PATH = $(TARGET_ARCH_ABI)
ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    ARCH_PATH = armeabi
endif

#ifeq ($(TARGET_ARCH_ABI), arm64-v8a)
#    ARCH_PATH = arm64
#endif

LOCAL_SRC_FILES := ../../../../native/libs/$(ARCH_PATH)/libopencv_java3.so

include $(PREBUILT_SHARED_LIBRARY)

# Main Jni Library
include $(CLEAR_VARS)

OpenCV_INSTALL_MODULES := on
OpenCV_CAMERA_MODULES := off

OPENCV_LIB_TYPE := SHARED

ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
include ../../../../native/jni/OpenCV.mk
else
include $(OPENCV_MK_PATH)
endif

LOCAL_MODULE := EgeioOpenCV

LOCAL_SRC_FILES := OpenCVHelperJNI.cpp

LOCAL_LDLIBS +=  -lm -llog

include $(BUILD_SHARED_LIBRARY)