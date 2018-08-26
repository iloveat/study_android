####################################################################################################
# An Android.mk file must begin with the definition of the LOCAL_PATH variable
# It is used to locate source files in the development tree. In this example,
# the macro function 'my-dir', provided by the build system, is used to return the path of the
# current directory (i.e. the directory containing the Android.mk file itself).
####################################################################################################
LOCAL_PATH := $(call my-dir)


############################################################################### openssl module begin
MY_SSL_HOME := /home/brycezou/Android/Turing_third/openssl-1_1_0/4android
include $(CLEAR_VARS)
LOCAL_MODULE    := libssl
LOCAL_SRC_FILES += $(MY_SSL_HOME)/lib/libssl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := libcrypto
LOCAL_SRC_FILES += $(MY_SSL_HOME)/lib/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)
################################################################################# openssl module end


############################################################################### libcurl module begin
MY_CURL_HOME := /home/brycezou/Android/Turing_third/curl-7_53_1/4android
include $(CLEAR_VARS)
LOCAL_MODULE    := libcurl
LOCAL_SRC_FILES += $(MY_CURL_HOME)/lib/libcurl.a
include $(PREBUILT_STATIC_LIBRARY)
################################################################################# libcurl module end


####################################################################################################
include $(CLEAR_VARS)
LOCAL_MODULE := iconv
LOCAL_CFLAGS := \
	-Wno-multichar \
	-DANDROID \
	-DLIBDIR="c" \
	-DBUILDING_LIBICONV \
	-DIN_LIBRARY
LOCAL_SRC_FILES := \
	libiconv-1.14/libcharset/lib/localcharset.c \
	libiconv-1.14/lib/iconv.c \
	libiconv-1.14/lib/relocatable.c
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/libiconv-1.14/include \
	$(LOCAL_PATH)/libiconv-1.14/libcharset \
	$(LOCAL_PATH)/libiconv-1.14/lib \
	$(LOCAL_PATH)/libiconv-1.14/libcharset/include \
	$(LOCAL_PATH)/libiconv-1.14/srclib
TARGET_ARCH_ABI := armeabi-v7a
LOCAL_ARM_NEON := true
LOCAL_EXPORT_C_INCLUDES := \
	$(LOCAL_PATH)/libiconv-1.14/include
include $(BUILD_STATIC_LIBRARY)
####################################################################################################


####################################################################################################
# The CLEAR_VARS variable is provided by the build system and points to a special GNU Makefile that
# will clear many LOCAL_XXX variables for you (e.g. LOCAL_MODULE, LOCAL_SRC_FILES,
# LOCAL_STATIC_LIBRARIES, etc...), with the exception of LOCAL_PATH. This is needed because all
# build control files are parsed in a single GNU Make execution context where all variables are global.
####################################################################################################
include $(CLEAR_VARS)


####################################################################################################
# The LOCAL_MODULE variable must be defined to identify each module you describe in your Android.mk.
# The name must be *unique* and not contain any spaces. Note that the build system will
# automatically add proper prefix and suffix to the corresponding generated file. In other words,
# a shared library module named 'foo' will generate 'libfoo.so'.
# BUT if you name your module 'libfoo', the build system will not add another 'lib' prefix and will
# generate libfoo.so as well.
####################################################################################################
LOCAL_MODULE := libturingtts


####################################################################################################
# It is possible to specify additional include paths with LOCAL_CFLAGS += -I<path>,
# however, it is better to use LOCAL_C_INCLUDES for this,
# since the paths will then also be used during native debugging with ndk-gdb.
####################################################################################################
LOCAL_C_INCLUDES := $(LOCAL_PATH)/libiconv-1.14/include
LOCAL_STATIC_LIBRARIES := iconv


# libcurl
LOCAL_C_INCLUDES += $(MY_CURL_HOME)/include
LOCAL_STATIC_LIBRARIES += libcurl
# openssl
LOCAL_C_INCLUDES += $(MY_SSL_HOME)/include
LOCAL_STATIC_LIBRARIES += libssl
LOCAL_STATIC_LIBRARIES += libcrypto


####################################################################################################
# The LOCAL_SRC_FILES variables must contain a list of C and/or C++ source files that will be built
# and assembled into a module. Note that you should not list header and included files here,
# because the build system will compute dependencies automatically for you; just list the source
# files that will be passed directly to a compiler, and you should be good
####################################################################################################
LOCAL_SRC_FILES += src/emADPCM.c
LOCAL_SRC_FILES += src/emCommon.c
LOCAL_SRC_FILES += src/emDebug.c
LOCAL_SRC_FILES += src/emDecode.c
LOCAL_SRC_FILES += src/emG7231.c
LOCAL_SRC_FILES += src/emG7231_COD_CNG.c
LOCAL_SRC_FILES += src/emG7231_CODER.c
LOCAL_SRC_FILES += src/emG7231_DEC_CNG.c
LOCAL_SRC_FILES += src/emG7231_DECOD.c
LOCAL_SRC_FILES += src/emG7231_EXC_LBC.c
LOCAL_SRC_FILES += src/emG7231_EXC_LBC_Cache.c
LOCAL_SRC_FILES += src/emG7231_LPC.c
LOCAL_SRC_FILES += src/emG7231_LPC_Cache.c
LOCAL_SRC_FILES += src/emG7231_LSP.c
LOCAL_SRC_FILES += src/emG7231_LSP_Cache.c
LOCAL_SRC_FILES += src/emG7231_TAB_LBC.c
LOCAL_SRC_FILES += src/emG7231_TAME.c
LOCAL_SRC_FILES += src/emG7231_TYPEDEF.c
LOCAL_SRC_FILES += src/emG7231_UTIL_CNG.c
LOCAL_SRC_FILES += src/emG7231_UTIL_LBC.c
LOCAL_SRC_FILES += src/emG7231_VAD.c
LOCAL_SRC_FILES += src/emHeap.c
LOCAL_SRC_FILES += src/emMath.c
LOCAL_SRC_FILES += src/emResample.c
LOCAL_SRC_FILES += src/emRes.c
LOCAL_SRC_FILES += src/emResPack.c
LOCAL_SRC_FILES += src/emString.c
LOCAL_SRC_FILES += src/emTTS.c
LOCAL_SRC_FILES += src/emTTS_Common.c
LOCAL_SRC_FILES += src/Front_CodeConvert.c
LOCAL_SRC_FILES += src/Front_ProcFuHao.c
LOCAL_SRC_FILES += src/Front_ProcHZRhythm_New.c
LOCAL_SRC_FILES += src/Front_ProcHZRhythm_New_PPH.c
LOCAL_SRC_FILES += src/Front_ProcHZRhythm_New_PW.c
LOCAL_SRC_FILES += src/Front_ProcHZRhythm_Old.c
LOCAL_SRC_FILES += src/Front_ProcHZToPinYin.c
LOCAL_SRC_FILES += src/Front_ProcHZWordSeg.c
LOCAL_SRC_FILES += src/Front_ProcShuZi.c
LOCAL_SRC_FILES += src/Front_ProcYingWen.c
LOCAL_SRC_FILES += src/Front_ToLab.c
LOCAL_SRC_FILES += src/Rear_Common.c
LOCAL_SRC_FILES += src/Rear_Pre.c
LOCAL_SRC_FILES += src/Rear_Synth.c
LOCAL_SRC_FILES += src/vocoder/common.c
LOCAL_SRC_FILES += src/vocoder/fft.c
LOCAL_SRC_FILES += src/vocoder/matlabfuns.c
LOCAL_SRC_FILES += src/vocoder/synthesis.c
LOCAL_SRC_FILES += src/vocoder/sptk.c
LOCAL_SRC_FILES += src/vocoder/world.c
LOCAL_SRC_FILES += main.cpp


####################################################################################################
# This is an optional variable that can be defined to indicate that your code relies on specific C++
# features.The effect of this variable is to enable the right compiler/linker flags when building
# your modules from sources. For prebuilt binaries, this also helps declare which features the
# binary relies on to ensure the final link works correctly. It is recommended to use this variable
# instead of enabling -frtti and -fexceptions directly in your LOCAL_CPPFLAGS definition.
####################################################################################################
LOCAL_CPP_FEATURES := rtti exceptions g


####################################################################################################
# The list of additional linker flags to be used when building your module. This is useful to pass
# the name of specific system libraries with the "-l" prefix. For example, the following will tell
# the linker to generate a module that links to /system/lib/libz.so at load time:LOCAL_LDLIBS:=-lz
####################################################################################################
LOCAL_LDLIBS := -lm -ldl -lz -llog -landroid -latomic


TARGET_ARCH_ABI := armeabi-v7a
LOCAL_ARM_NEON := true


####################################################################################################
# The BUILD_SHARED_LIBRARY is a variable provided by the build system that points to a GNU Makefile
# script that is in charge of collecting all the information you defined in LOCAL_XXX variables
# since the latest 'include $(CLEAR_VARS)' and determine what to build, and how to do itexactly.
# There is also BUILD_STATIC_LIBRARY to generate a static library.
####################################################################################################
include $(BUILD_SHARED_LIBRARY)


