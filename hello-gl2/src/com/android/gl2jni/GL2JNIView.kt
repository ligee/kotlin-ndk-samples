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
 */

package com.android.gl2jni

/*
 * Copyright (C) 2008 The Android Open Source Project
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

/**
 * A simple GLSurfaceView sub-class that demonstrate how to perform
 * OpenGL ES 2.0 rendering into a GL Surface. Note the following important
 * details:
 *
 * - The class must use a custom context factory to enable 2.0 rendering.
 *   See ContextFactory class definition below.
 *
 * - The class must use a custom EGLConfigChooser to be able to select
 *   an EGLConfig that supports 2.0. This is done by providing a config
 *   specification to eglChooseConfig() that has the attribute
 *   EGL10.ELG_RENDERABLE_TYPE containing the EGL_OPENGL_ES2_BIT flag
 *   set. See ConfigChooser class definition below.
 *
 * - The class must select the surface's format, then choose an EGLConfig
 *   that matches it exactly (with regards to red/green/blue/alpha channels
 *   bit depths). Failure to do so would result in an EGL_BAD_MATCH error.
 */

class GL2JNIView(context: Context, translucent: Boolean = false, depth: Int = 0, stencil: Int = 0) : GLSurfaceView(context) {

    {
        /* By default, GLSurfaceView() creates a RGB_565 opaque surface.
         * If we want a translucent one, we should change the surface's
         * format here, using PixelFormat.TRANSLUCENT for GL Surfaces
         * is interpreted as any 32-bit surface with alpha by SurfaceFlinger.
         */
        if (translucent) {
            this.getHolder().setFormat(PixelFormat.TRANSLUCENT)
        }

        /* Setup the context factory for 2.0 rendering.
         * See ContextFactory class definition below
         */
        setEGLContextFactory(ContextFactory())

        /* We need to choose an EGLConfig that matches the format of
         * our surface exactly. This is going to be done in our
         * custom config chooser. See ConfigChooser class definition
         * below.
         */
        setEGLConfigChooser(if (translucent)
            ConfigChooser(8, 8, 8, 8, depth, stencil)
        else
            ConfigChooser(5, 6, 5, 0, depth, stencil))

        /* Set the renderer responsible for frame rendering */
        setRenderer(Renderer())
    }

    private class ContextFactory : GLSurfaceView.EGLContextFactory {
        override fun createContext(egl: EGL10, display: EGLDisplay, eglConfig: EGLConfig): EGLContext {
            Log.w(TAG, "creating OpenGL ES 2.0 context")
            checkEglError("Before eglCreateContext", egl)
            val attrib_list = intArray(EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
            val context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list)
            checkEglError("After eglCreateContext", egl)
            return context
        }

        override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) {
            egl.eglDestroyContext(display, context)
        }

        class object {
            private val EGL_CONTEXT_CLIENT_VERSION = 12440
        }
    }

    private class ConfigChooser(// Subclasses can adjust these values:
            protected var mRedSize: Int, protected var mGreenSize: Int, protected var mBlueSize: Int, protected var mAlphaSize: Int, protected var mDepthSize: Int, protected var mStencilSize: Int) : GLSurfaceView.EGLConfigChooser {

        override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig? {

            /* Get the number of minimally matching EGL configurations
             */
            val num_config = IntArray(1)
            egl.eglChooseConfig(display, s_configAttribs2, null, 0, num_config)

            val numConfigs = num_config[0]

            if (numConfigs <= 0) {
                throw IllegalArgumentException("No configs match configSpec")
            }

            /* Allocate then read the array of minimally matching EGL configs
             */
            val configs = arrayOfNulls<EGLConfig>(numConfigs)
            egl.eglChooseConfig(display, s_configAttribs2, configs, numConfigs, num_config)

            if (DEBUG) {
                printConfigs(egl, display, configs)
            }
            /* Now return the "best" one
             */
            return chooseConfig(egl, display, configs)
        }

        public fun chooseConfig(egl: EGL10, display: EGLDisplay, configs: Array<EGLConfig?>): EGLConfig? {
            for (config in configs) {
                val d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0)
                val s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0)

                // We need at least mDepthSize and mStencilSize bits
                if (d < mDepthSize || s < mStencilSize)
                    continue

                // We want an *exact* match for red/green/blue/alpha
                val r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0)
                val g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0)
                val b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0)
                val a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0)

                if (r == mRedSize && g == mGreenSize && b == mBlueSize && a == mAlphaSize)
                    return config
            }
            return null
        }

        private fun findConfigAttrib(egl: EGL10, display: EGLDisplay, config: EGLConfig?, attribute: Int, defaultValue: Int): Int {

            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0]
            }
            return defaultValue
        }

        private fun printConfigs(egl: EGL10, display: EGLDisplay, configs: Array<EGLConfig?>) {
            val numConfigs = configs.count()
            Log.w(TAG,  java.lang.String.format("%d configurations", numConfigs))
            for (i in 0..numConfigs - 1) {
                Log.w(TAG,  java.lang.String.format("Configuration %d:\n", i))
                printConfig(egl, display, configs[i])
            }
        }

        private fun printConfig(egl: EGL10, display: EGLDisplay, config: EGLConfig?) {
            val attributes: kotlin.Array<Int> = array(EGL10.EGL_BUFFER_SIZE, EGL10.EGL_ALPHA_SIZE, EGL10.EGL_BLUE_SIZE, EGL10.EGL_GREEN_SIZE, EGL10.EGL_RED_SIZE, EGL10.EGL_DEPTH_SIZE, EGL10.EGL_STENCIL_SIZE, EGL10.EGL_CONFIG_CAVEAT, EGL10.EGL_CONFIG_ID, EGL10.EGL_LEVEL, EGL10.EGL_MAX_PBUFFER_HEIGHT, EGL10.EGL_MAX_PBUFFER_PIXELS, EGL10.EGL_MAX_PBUFFER_WIDTH, EGL10.EGL_NATIVE_RENDERABLE, EGL10.EGL_NATIVE_VISUAL_ID, EGL10.EGL_NATIVE_VISUAL_TYPE, 12336, // EGL10.EGL_PRESERVED_RESOURCES,
                    EGL10.EGL_SAMPLES, EGL10.EGL_SAMPLE_BUFFERS, EGL10.EGL_SURFACE_TYPE, EGL10.EGL_TRANSPARENT_TYPE, EGL10.EGL_TRANSPARENT_RED_VALUE, EGL10.EGL_TRANSPARENT_GREEN_VALUE, EGL10.EGL_TRANSPARENT_BLUE_VALUE, 12345, // EGL10.EGL_BIND_TO_TEXTURE_RGB,
                    12346, // EGL10.EGL_BIND_TO_TEXTURE_RGBA,
                    12347, // EGL10.EGL_MIN_SWAP_INTERVAL,
                    12348, // EGL10.EGL_MAX_SWAP_INTERVAL,
                    EGL10.EGL_LUMINANCE_SIZE, EGL10.EGL_ALPHA_MASK_SIZE, EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RENDERABLE_TYPE, 12354 // EGL10.EGL_CONFORMANT
            )
            val names = array("EGL_BUFFER_SIZE", "EGL_ALPHA_SIZE", "EGL_BLUE_SIZE", "EGL_GREEN_SIZE", "EGL_RED_SIZE", "EGL_DEPTH_SIZE", "EGL_STENCIL_SIZE", "EGL_CONFIG_CAVEAT", "EGL_CONFIG_ID", "EGL_LEVEL", "EGL_MAX_PBUFFER_HEIGHT", "EGL_MAX_PBUFFER_PIXELS", "EGL_MAX_PBUFFER_WIDTH", "EGL_NATIVE_RENDERABLE", "EGL_NATIVE_VISUAL_ID", "EGL_NATIVE_VISUAL_TYPE", "EGL_PRESERVED_RESOURCES", "EGL_SAMPLES", "EGL_SAMPLE_BUFFERS", "EGL_SURFACE_TYPE", "EGL_TRANSPARENT_TYPE", "EGL_TRANSPARENT_RED_VALUE", "EGL_TRANSPARENT_GREEN_VALUE", "EGL_TRANSPARENT_BLUE_VALUE", "EGL_BIND_TO_TEXTURE_RGB", "EGL_BIND_TO_TEXTURE_RGBA", "EGL_MIN_SWAP_INTERVAL", "EGL_MAX_SWAP_INTERVAL", "EGL_LUMINANCE_SIZE", "EGL_ALPHA_MASK_SIZE", "EGL_COLOR_BUFFER_TYPE", "EGL_RENDERABLE_TYPE", "EGL_CONFORMANT")
            val value = IntArray(1)
            var i = 0
            while (i < attributes.count()) {
                val attribute = attributes[i]
                val name = names[i]
                if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
                    Log.w(TAG,  java.lang.String.format("  %s: %d\n", name, value[0]))
                } else {
                    // Log.w(TAG, String.format("  %s: failed\n", name));
                    while (egl.eglGetError() != EGL10.EGL_SUCCESS)
                        ;
                }
            }
        }

        private val mValue = IntArray(1)

        class object {

            /* This EGL config specification is used to specify 2.0 rendering.
         * We use a minimum size of 4 bits for red/green/blue, but will
         * perform actual matching in chooseConfig() below.
         */
            private val EGL_OPENGL_ES2_BIT = 4
            private val s_configAttribs2 = intArray(EGL10.EGL_RED_SIZE, 4, EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4, EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE)
        }
    }

    class Renderer : GLSurfaceView.Renderer {
        override fun onDrawFrame(gl: GL10) {
            GL2JNILib.step()
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            GL2JNILib.init(width, height)
        }

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            // Do nothing.
        }
    }

    class object {
        internal val TAG: String = "GL2JNIView"
        internal val DEBUG: Boolean = false

        internal fun checkEglError(prompt: String, egl: EGL10) {
            val error: Int
            while (true) {
                error = egl.eglGetError()
                if (error != EGL10.EGL_SUCCESS) {
                    Log.e(TAG,  java.lang.String.format("%s: EGL error: 0x%x", prompt, error))
                }
                else break
            }
        }
    }
}
