/*
 * Copyright (C) 2011 David Valdeita (Seleuco)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * In addition, as a special exception, Seleuco
 * gives permission to link the code of this program with
 * the MAME library (or with modified versions of MAME that use the
 * same license as MAME), and distribute linked combinations including
 * the two.  You must obey the GNU General Public License in all
 * respects for all of the code used other than MAME.  If you modify
 * this file, you may extend this exception to your version of the
 * file, but you are not obligated to do so.  If you do not wish to
 * do so, delete this exception statement from your version.
 */

package com.droidmame.sf2;


import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Paint.Style;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.droidmame.helpers.PrefsHelper;
import com.droidmame.views.EmulatorViewGL;

public class Emulator 
{
    final static public int FPS_SHOWED_KEY   = 1;
    final static public int EXIT_GAME_KEY    = 2;
    final static public int LAND_BUTTONS_KEY = 3;
    final static public int HIDE_LR__KEY     = 4;
    final static public int BPLUSX_KEY       = 5;
    final static public int WAYS_STICK_KEY   = 6;
    final static public int ASMCORES_KEY     = 7;
    final static public int INFOWARN_KEY     = 8;
    final static public int EXIT_PAUSE       = 9;
    final static public int IDLE_WAIT        = 10;

    private static StreetFighterA mm = null;
    
    private static boolean isEmulating = false;
    public static boolean isEmulating() { return isEmulating; }

    private static boolean paused = false;
    private static Object lock1 = new Object();
    private static Object lock2 = new Object();
    
    private static SurfaceHolder holder = null;
    private static Bitmap emuBitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.RGB_565);
    
    private static int window_width   = 320;
    private static int window_height  = 240;
    private static int emu_width      = 320;
    private static int emu_height     = 240;
    
    private static int []screenBuffPx = new int[640*480*3];
    public  static int[] getScreenBuffPx() {
        return screenBuffPx;
    }

    private static boolean frameFiltering = false;
    public static boolean isFrameFiltering() {
        return frameFiltering;
    }

    private static Paint emuPaint = null;
    private static Paint debugPaint = new Paint();
    
    private static Matrix mtx = new Matrix();
    
    public static int getWindow_width() {
        return window_width;
    }
    
    public static int getWindow_height() {
        return window_height;
    }

    private static ByteBuffer screenBuff = null;
    private static AudioTrack audioTrack    = null;
    private static boolean isThreadedSound  = false;
    private static boolean isDebug          = false;
    private static int videoRenderMode      = PrefsHelper.PREF_RENDER_THREADED;
    private static boolean inMAME           = false;
    private static boolean screenshot       = false; 
    public static int screenshot_num; 

    public static boolean isInMAME() {
        return inMAME;
    }
    
    private static int overlayFilterType  =  PrefsHelper.PREF_FILTER_NONE;

    public static int getOverlayFilterType() {
        return overlayFilterType;
    }

    public static void setOverlayFilterType(int overlayFilterType) {
        Emulator.overlayFilterType = overlayFilterType;
    }

    static long j = 0;
    static int i = 0;
    static int fps = 0;
    static long millis;
    
    private static SoundThread soundT = new SoundThread();
    private static VideoThread videoT = new VideoThread();
                    
    static
    {
        try {
            System.loadLibrary("mame4all-jni");
        }
        catch(java.lang.Error e) {
           e.printStackTrace();
        }
                        
        debugPaint.setARGB(255, 255, 255, 255);
        debugPaint.setStyle(Style.STROKE);
        debugPaint.setTextSize(16);
        //videoT.start();
    }
    
    public static int getEmulatedWidth() {
        return emu_width;
    }

    public static int getEmulatedHeight() {
        return emu_height;
    }
    
    public static boolean isThreadedSound() {
        return isThreadedSound;
    }

    public static void setThreadedSound(boolean isThreadedSound) {
        Emulator.isThreadedSound = isThreadedSound;
    }

    public static boolean isDebug() {
        return isDebug;
    }

    public static void setDebug(boolean isDebug) {
        Emulator.isDebug = isDebug;
    }
    
    public static int getVideoRenderMode() {
        return Emulator.videoRenderMode;
    }
    
    public static void setVideoRenderMode(int videoRenderMode) {
        Emulator.videoRenderMode = videoRenderMode;
    }

    public static Paint getEmuPaint() {
            return emuPaint;
    }
    
    public static Paint getDebugPaint() {
            return debugPaint;
    }
    
    public static Matrix getMatrix() {
            return mtx;
    }
    
    //synchronized
    public static SurfaceHolder getHolder() {
        return holder;
    }
    
    //synchronized 
    public static Bitmap getEmuBitmap() {
        return emuBitmap;
    }
    
    //synchronized 
    public static ByteBuffer getScreenBuffer() {
        return screenBuff;
    }
    
    public static void setHolder(SurfaceHolder value) {
        synchronized(lock1) {
            if (value != null) {
                holder = value;
                holder.setFormat(PixelFormat.OPAQUE);
                holder.setKeepScreenOn(true);
                videoT.start();
            }
            else {
                videoT.stop();
                holder=null;
            }
        }
    }
    
    public static Canvas lockCanvas() {
        if (holder!=null) {
            return holder.lockCanvas();
        }
        else 
            return null;
    }
    
    public static void unlockCanvas(Canvas c) {
        if (holder != null && c != null) {
           holder.unlockCanvasAndPost(c);
        }
    }
    
    public static void setMAME4all(StreetFighterA mm) {
        Emulator.mm = mm;
        videoT.setMAME4all(mm);
    }
    
    //VIDEO
    public static void setWindowSize(int w, int h) {
        window_width = w;
        window_height = h;
        
        if (videoRenderMode == PrefsHelper.PREF_RENDER_GL)
            return;

        mtx.setScale((float)(window_width / (float)emu_width), 
                    (float)(window_height / (float)emu_height));
    }

    public static void setFrameFiltering(boolean value) {
        frameFiltering = value;
        if (value) {
            emuPaint = new Paint();
            emuPaint.setFilterBitmap(true);
        }
        else {
            emuPaint = null;
        }
    }
    
    
    //synchronized 
    static void bitblt(ByteBuffer sScreenBuff, boolean inMAME) {
        if (paused) { //locks are expensive
            synchronized(lock2) {
                try {
                    if (paused)
                       lock2.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        synchronized(lock1) {
            screenBuff = sScreenBuff;
            Emulator.inMAME = inMAME;
               
            if (videoRenderMode == PrefsHelper.PREF_RENDER_GL) {
                ((EmulatorViewGL)mm.getEmuView()).requestRender();
            }
            else if (videoRenderMode == PrefsHelper.PREF_RENDER_THREADED) {
                videoT.update();
            }
            else if (videoRenderMode == PrefsHelper.PREF_RENDER_HW) {
                videoT.update();
            }
            else {
                if (holder == null)
                    return;

                Canvas canvas = holder.lockCanvas();
                sScreenBuff.rewind();
                emuBitmap.copyPixelsFromBuffer(sScreenBuff);
                i++;
                canvas.concat(mtx);
                canvas.drawBitmap(emuBitmap, 0, 0, emuPaint);
                //canvas.drawBitmap(emuBitmap, null, frameRect, emuPaint);
                if (isDebug) {
                    canvas.drawText("Normal fps:"+fps+ " "+inMAME, 5,  40, debugPaint);
                    if(System.currentTimeMillis() - millis >= 1000) {fps = i; i=0;millis = System.currentTimeMillis();}
                }
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }
    
    //synchronized 
    static public void changeVideo(int newWidth, int newHeight) {
        synchronized(lock1) {
            for (int i=0;i<4;i++)
                Emulator.setPadData(i, 0);
            
            emu_width = newWidth;
            emu_height = newHeight;
                
            emuBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.RGB_565);
            mtx.setScale((float)(window_width / (float)emu_width), 
                        (float)(window_height / (float)emu_height));
                
            if (videoRenderMode == PrefsHelper.PREF_RENDER_GL) {
                GLRenderer r = (GLRenderer)((EmulatorViewGL)mm.getEmuView()).getRender();

                if (r != null)
                    r.changedEmulatedSize();
            }
                                    
            mm.runOnUiThread(new Runnable() {
                public void run() {
                    mm.getMainHelper().updateMAME4all();
                }
            });
        }
    }
    
    //SOUND
    static public void initAudio(int freq, boolean stereo) {
        int sampleFreq = freq;
        int channelConfig = stereo ? AudioFormat.CHANNEL_CONFIGURATION_STEREO : AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioTrack.getMinBufferSize(sampleFreq, channelConfig, audioFormat) * 2;
        
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        sampleFreq,
                        channelConfig,
                        audioFormat,
                        bufferSize,
                        AudioTrack.MODE_STREAM);
        
        audioTrack.play();
    }
    
    public static void endAudio(){
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
    }
            
    public static void writeAudio(byte[] b, int sz) {
        //System.out.println("Envio "+sz+" "+audioTrack);
        if (audioTrack != null) {
            if (isThreadedSound && soundT != null) {
               soundT.setAudioTrack(audioTrack);
               soundT.writeSample(b, sz);
            }
            else {
               audioTrack.write(b, 0, sz);
            }  
        }   
    }
    
    //LIVE CYCLE
    public static void pause(){
        if (isEmulating) {
            //pauseEmulation(true);
            paused = true;
        }   
        
        if (audioTrack != null)
            audioTrack.pause();
        
        videoT.stop();
    }
    
    public static void resume() {
        if (audioTrack != null)
            audioTrack.play();
        
        if (isEmulating) {
            //pauseEmulation(false);
            Emulator.setValue(Emulator.EXIT_PAUSE, 1);
            synchronized(lock2) {
                paused = false;
                lock2.notify();
            }    
        }    
        
        videoT.start();
    }
    
    //EMULATOR
    public static void emulate(final String libPath,final String resPath){
        //Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        
        if (isEmulating) return;
            
        Thread t = new Thread(new Runnable() {
                public void run() {
                        isEmulating = true;
                        init(libPath,resPath);
                }
        },"emulator-Thread");
            

        //t.setPriority(Thread.MIN_PRIORITY);
        //t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    //is it necessary to set a lock? i think storing a boolean value is atomic
    public static void setScreenshot() {
        if (!screenshot) {
            screenshot = true; 
        }
    } 


    public static void doScreenshot() {
        if (screenshot) {
            //Bitmap screen_bmp = Bitmap.createScaledBitmap(emuBitmap, getWindow_width(), getWindow_height(), false);

            //per google play, we get 480*800
            Bitmap screen_bmp; 
            if (mm.getMainHelper().getscrOrientation() 
                    == Configuration.ORIENTATION_LANDSCAPE) {
                screen_bmp = Bitmap.createScaledBitmap(emuBitmap, 800, 480, false);
            }
            else {
                screen_bmp = Bitmap.createScaledBitmap(emuBitmap, 480, 800, false);
            }
            try {
                String newname= String.format("screenshot-%04d.jpg", screenshot_num++);
                File fn = new File(mm.getPrefsHelper().getROMsDIR() + File.separator + newname);
                FileOutputStream fos = new FileOutputStream(fn);
                screen_bmp.compress(Bitmap.CompressFormat.JPEG, 80, fos);

                if (mm != null)  {
                    Log.v(StreetFighterA.TAG, "saved " + newname);
                    //doesn't work, why? 
                    //mm.toastMsg("saved " +  newname);
                }
            } catch (IOException ex) {
                Log.getStackTraceString(ex);     
            } catch (Exception ex) {
                Log.getStackTraceString(ex); 
            }
            screenshot = false;
         }
    }    
    //native
    protected static native void init(String libPath,String resPath);
                    
    synchronized public static native void setPadData(int i, long data);
    
    synchronized public static native void setAnalogData(int i, float v1, float v2);
    
    public static native int getValue(int key);
    
    public static native void setValue(int key, int value);
            
}
