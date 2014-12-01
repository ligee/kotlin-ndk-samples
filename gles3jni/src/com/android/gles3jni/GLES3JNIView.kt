/*
 * Copyright 2013 The Android Open Source Project
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

package com.android.gles3jni

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent

import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10

class GLES3JNIView(context: Context) : GLSurfaceView(context) {

    {
        // Pick an EGLConfig with RGB8 color, 16-bit depth, no stencil,
        // supporting OpenGL ES 2.0 or later backwards-compatible versions.
        setEGLConfigChooser(8, 8, 8, 0, 16, 0)
        setEGLContextClientVersion(2)
        setRenderer(Renderer())
    }

    private class Renderer : GLSurfaceView.Renderer {
        override fun onDrawFrame(gl: GL10) {
            GLES3JNILib.step()
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            GLES3JNILib.resize(width, height)
        }

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            GLES3JNILib.init()
        }
    }

    class object {
        private val TAG = "GLES3JNI"
        private val DEBUG = true
    }
}
