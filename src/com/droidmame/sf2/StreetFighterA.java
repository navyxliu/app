/*
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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Toast;
import android.widget.FrameLayout;

import com.droidmame.helpers.DialogHelper;
import com.droidmame.helpers.MainHelper;
import com.droidmame.helpers.MenuHelper;
import com.droidmame.helpers.PrefsHelper;
import com.droidmame.input.ControlCustomizer;
import com.droidmame.input.InputHandler;
import com.droidmame.input.InputHandlerFactory;
import com.droidmame.views.FilterView;
import com.droidmame.views.IEmuView;
import com.droidmame.views.InputView;
import com.dreamgame.sf2ce.R;
import com.dreamgame.sf2ce.BuildConfig;
import com.umeng.analytics.MobclickAgent;
import com.airpush.android.Airpush;

public class StreetFighterA extends Activity {
    public static String TAG = "emul";
    public static boolean isDebug = BuildConfig.DEBUG;
    protected View emuView              = null;
    protected InputView inputView       = null;
    protected FilterView filterView     = null;
    protected MainHelper mainHelper     = null;
    protected MenuHelper menuHelper     = null;
    protected PrefsHelper prefsHelper   = null;
    protected DialogHelper dialogHelper = null;
    protected InputHandler inputHandler = null;
    protected FileExplorer fileExplore  = null;
    private Airpush airpush; 
    public FileExplorer getFileExplore() {
        return fileExplore;
    }

    public MenuHelper getMenuHelper() {
        return menuHelper;
    }

    public PrefsHelper getPrefsHelper() {
        return prefsHelper;
    }
    
    public MainHelper getMainHelper() {
        return mainHelper;
    }
    
    public DialogHelper getDialogHelper() {
        return dialogHelper;
    }
    
    public View getEmuView() {
        return emuView;
    }

    public InputView getInputView() {
        return inputView;
    }

    public FilterView getFilterView() {
        return filterView;
    }

    public InputHandler getInputHandler() {
        return inputHandler;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
       // getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        
        setContentView(R.layout.main);
        prefsHelper  = new PrefsHelper(this);
        dialogHelper = new DialogHelper(this);
        mainHelper   = new MainHelper(this);
        fileExplore  = new FileExplorer(this);
        menuHelper   = new MenuHelper(this);
                
        //inputHandler = new InputHandler(this);
        inputHandler = InputHandlerFactory.createInputHandler(this);
        FrameLayout fl = (FrameLayout)this.findViewById(R.id.EmulatorFrame);

        if (prefsHelper.getVideoRenderMode() == PrefsHelper.PREF_RENDER_GL) {
            this.getLayoutInflater().inflate(R.layout.emuview_gl, fl);
            emuView = this.findViewById(R.id.EmulatorViewGL);
        }
        else if (prefsHelper.getVideoRenderMode() == PrefsHelper.PREF_RENDER_HW) {
            this.getLayoutInflater().inflate(R.layout.emuview_hw, fl);
            emuView = this.findViewById(R.id.EmulatorViewHW);     
            /*
                getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            */
        }
        else {
            this.getLayoutInflater().inflate(R.layout.emuview_sw, fl);
            emuView = this.findViewById(R.id.EmulatorViewSW);
        }
               
        inputView = (InputView) this.findViewById(R.id.InputView);
        ((IEmuView)emuView).setMAME4all(this);
        inputView.setMAME4all(this);
        Emulator.setMAME4all(this);        
         
        View frame = this.findViewById(R.id.EmulatorFrame);
        frame.setOnTouchListener(inputHandler);        
        
        if ((prefsHelper.getPortraitOverlayFilterType() != PrefsHelper.PREF_FILTER_NONE 
                && mainHelper.getscrOrientation() == Configuration.ORIENTATION_PORTRAIT)
            || (prefsHelper.getLandscapeOverlayFilterType() != PrefsHelper.PREF_FILTER_NONE 
                && mainHelper.getscrOrientation() == Configuration.ORIENTATION_LANDSCAPE)) {
            int type;
            
            if (mainHelper.getscrOrientation() == Configuration.ORIENTATION_PORTRAIT)
                type = prefsHelper.getPortraitOverlayFilterType();
            else
                type = prefsHelper.getLandscapeOverlayFilterType();
           
            int dwb_id = -1;
            
            switch(type){
            case 2: case 3: dwb_id = R.drawable.scanline_1;break;
            case 4: case 5: dwb_id = R.drawable.scanline_2;break;
            case 6: case 7: dwb_id = R.drawable.crt_1;break;
            case 8: case 9: dwb_id = R.drawable.crt_2;break;
            }
            
            if (dwb_id != -1) {
                getLayoutInflater().inflate(R.layout.filterview, fl);
                filterView = (FilterView)this.findViewById(R.id.FilterView);
                Bitmap bmp = BitmapFactory.decodeResource(getResources(),dwb_id);
                BitmapDrawable bitmapDrawable = new BitmapDrawable(bmp);
                bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                //bitmapDrawable.setAlpha((int)((type> 3 ? 0.16f : 0.35f) *255));
                int alpha = 0;

                if (type == 2)   alpha = 130;
                else if(type==3) alpha = 180;
                else if(type==4) alpha = 100;
                else if(type==5) alpha = 150;
                else if(type==6) alpha = 50;
                else if(type==7) alpha = 130;
                else if(type==8) alpha = 50;
                else if(type==9) alpha = 120;

                bitmapDrawable.setAlpha(alpha);
                filterView.setBackgroundDrawable(bitmapDrawable);
                filterView.setMAME4all(this);
            }
        }
                
        emuView.setOnKeyListener(inputHandler);
        emuView.setOnTouchListener(inputHandler);
                     
        inputView.setOnTouchListener(inputHandler);
        inputView.setOnKeyListener(inputHandler);
        
        mainHelper.updateMAME4all();
        toastMsg("loading, please wait...");

        if (!Emulator.isEmulating()) {
            //xliu: skip diag for ROMsDir
            if (prefsHelper.getROMsDIR() == null) {         
                if (mainHelper.ensureDefaultROMsDir()) {
                    getPrefsHelper().setROMsDIR(mainHelper.getDefaultROMsDIR());
                }
                else {
                    showDialog(DialogHelper.DIALOG_LOAD_FILE_EXPLORER);
                }
            }

        Log.d(TAG,"onCreate airpush");
        airpush = new Airpush(this);
        airpush.startPushNotification(false);
        // start icon ad.
	//airpush.startIconAd();		


            runMAME4all();
        }
    }
    
    public void runMAME4all() {
        getMainHelper().copyFiles();
        Emulator.emulate(mainHelper.getLibDir(),prefsHelper.getROMsDIR());
    }
    
    //MENU STUFF
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (menuHelper != null) {
            if (menuHelper.createOptionsMenu(menu)) 
                return true;
        }  
            
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menuHelper != null) {
            if (menuHelper.prepareOptionsMenu(menu)) 
                return true;
        }   
        return super.onPrepareOptionsMenu(menu); 
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(menuHelper!=null) {
            if(menuHelper.optionsItemSelected(item))
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //ACTIVITY
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mainHelper != null)
            mainHelper.activityResult(requestCode, resultCode, data);
    }

    //LIVE CYCLE
    @Override
    protected void onResume() {
        super.onResume();
        if (!isDebug) {
            Log.d(TAG, "umeng is called");
            MobclickAgent.onResume(this);
        }
        
        if (prefsHelper != null)
            prefsHelper.resume();

        if (DialogHelper.savedDialog != -1)
            showDialog(DialogHelper.savedDialog);
        else if (!ControlCustomizer.isEnabled())
            Emulator.resume();

        if (inputHandler != null) 
            if (inputHandler.getTiltSensor() != null)
                inputHandler.getTiltSensor().enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isDebug) {
            MobclickAgent.onPause(this);
        } 
        if (prefsHelper != null)
           prefsHelper.pause();

        if (!ControlCustomizer.isEnabled())
           Emulator.pause();

        if (inputHandler != null) {
            if (inputHandler.getTiltSensor() != null)
                inputHandler.getTiltSensor().disable();
        }

        if (dialogHelper!=null) {
            dialogHelper.removeDialogs();
        }
                
    }

    @Override
    protected void onStart() {
        super.onStart();
        //System.out.println("OnStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    //Dialog Stuff
    @Override
    protected Dialog onCreateDialog(int id) {
        if (dialogHelper != null) {
            Dialog d = dialogHelper.createDialog(id);
            if (d != null) return d;
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (dialogHelper != null)
            dialogHelper.prepareDialog(id, dialog);
    }

    public void toastMsg(String msg)
    {
        int duration = Toast.LENGTH_LONG;

        Toast.makeText(this, msg, duration).show();
    }

}
