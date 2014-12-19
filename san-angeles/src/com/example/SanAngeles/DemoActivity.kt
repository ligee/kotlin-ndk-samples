/*
 * Copyright (C) 2009 The Android Open Source Project
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
 *
 * This is a small port of the "San Angeles Observation" demo
 * program for OpenGL ES 1.x. For more details, see:
 *
 *    http://jet.ro/visuals/san-angeles-observation/
 *
 * This program demonstrates how to use a GLSurfaceView from Java
 * along with native OpenGL calls to perform frame rendering.
 *
 * Touching the screen will start/stop the animation.
 *
 * Note that the demo runs much faster on the emulator than on
 * real devices, this is mainly due to the following facts:
 *
 * - the demo sends bazillions of polygons to OpenGL without
 *   even trying to do culling. Most of them are clearly out
 *   of view.
 *
 * - on a real device, the GPU bus is the real bottleneck
 *   that prevent the demo from getting acceptable performance.
 *
 * - the software OpenGL engine used in the emulator uses
 *   the system bus instead, and its code rocks :-)
 *
 * Fixing the program to send less polygons to the GPU is left
 * as an exercise to the reader. As always, patches welcomed :-)
 */
package com.example.SanAngeles

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import kotlin.platform.platformStatic

public class DemoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGLView = DemoGLSurfaceView(this)
        setContentView(mGLView)
    }

    override fun onPause() {
        super.onPause()
        mGLView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        mGLView!!.onResume()
    }

    private var mGLView: GLSurfaceView? = null

    class object {

        {
            System.loadLibrary("sanangeles")
        }
    }
}

class DemoGLSurfaceView(context: Context) : GLSurfaceView(context) {

    val mRenderer: DemoRenderer

    {
        mRenderer = DemoRenderer()
        setRenderer(mRenderer)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            nativeTogglePauseResume()
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        nativePause()
    }

    override fun onResume() {
        super.onResume()
        nativeResume()
    }

    class object {

        native platformStatic fun nativePause(): Unit
        native platformStatic fun nativeResume(): Unit
        native platformStatic fun nativeTogglePauseResume(): Unit
    }
}

class DemoRenderer : GLSurfaceView.Renderer {
    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        nativeInit()
    }

    override fun onSurfaceChanged(gl: GL10, w: Int, h: Int) {
        //gl.glViewport(0, 0, w, h);
        nativeResize(w, h)
    }

    override fun onDrawFrame(gl: GL10) {
        nativeRender()
    }

    class object {

        native platformStatic fun nativeInit(): Unit
        native platformStatic fun nativeResize(w: Int, h: Int): Unit
        native platformStatic fun nativeRender(): Unit
        native platformStatic fun nativeDone(): Unit
    }
}
