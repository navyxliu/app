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

package com.droidmame.input;

import java.lang.reflect.Method;

import android.view.MotionEvent;
import android.util.Log;
import com.droidmame.sf2.StreetFighterA;

public class InputHandlerFactory {
    static public InputHandler createInputHandler(StreetFighterA mm) {
        try {
            @SuppressWarnings("unused")
            Method m = MotionEvent.class.getMethod("getPointerCount");
            Log.d(StreetFighterA.TAG, "support multitouch."); 
            return new InputHandlerExt(mm);//MultiTouch  
        } catch (NoSuchMethodException e) {
            return new InputHandler(mm);
        }
    }
}
