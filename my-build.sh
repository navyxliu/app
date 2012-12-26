#!/bin/sh
$ANDROID_NDK/ndk-build APP_ABI="armeabi-v7a"
cp ~/Development/droidmame/libdroidMAME.so ./libs/armeabi-v7a/libMAME4all.so
