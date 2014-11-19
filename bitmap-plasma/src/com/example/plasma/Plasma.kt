/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.plasma

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.view.View
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.Display
import android.view.WindowManager

public class Plasma : Activity() {
    /** Called when the activity is first created. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val display = getWindowManager().getDefaultDisplay()
        setContentView(PlasmaView(this, display.getWidth(), display.getHeight()))
    }

    class object {

        /* load our native library */
        {
            System.loadLibrary("plasma")
        }
    }
}

class PlasmaView(context: Context, width: Int, height: Int) : View(context) {
    private var mBitmap: Bitmap? = null
    private var mStartTime: Long = 0

    /* implementend by libplasma.so */
    private native fun renderPlasma(bitmap: Bitmap, time_ms: Long): Unit = null!!

    {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        mStartTime = System.currentTimeMillis()
    }

    override fun onDraw(canvas: Canvas) {
        //canvas.drawColor(0xFFCCCCCC);
        renderPlasma(mBitmap!!, System.currentTimeMillis() - mStartTime)
        canvas.drawBitmap(mBitmap, 0.toFloat(), 0.toFloat(), null)
        // force a redraw, with a different time-based pattern.
        invalidate()
    }
}
