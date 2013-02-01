#!/bin/sh
./jni/jni.sh 
javah -classpath ./bin/classes -d jni/ com.droidmame.sf2.Emulator 
$ANDROID_NDK/ndk-build APP_ABI="armeabi-v7a"
cp ~/Development/droidmame/libdroidMAME.so ./libs/armeabi-v7a/libMAME4all.so
