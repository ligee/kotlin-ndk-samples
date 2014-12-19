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

package com.sample.helper

import java.io.File
import java.io.FileInputStream

import javax.microedition.khronos.opengles.GL10

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.AudioManager
import android.media.AudioTrack
import android.opengl.GLUtils
import android.util.Log

public class NDKHelper {

    //
    // Load Bitmap
    // Java helper is useful decoding PNG, TIFF etc rather than linking libPng
    // etc separately
    //
    private fun nextPOT(i: Int): Int {
        var pot = 1
        while (pot < i)
            pot = pot shl 1
        return pot
    }

    private fun scaleBitmap(bitmapToScale: Bitmap?, newWidth: Float, newHeight: Float): Bitmap? {
        if (bitmapToScale == null)
            return null
        // get the original width and height
        val width = bitmapToScale.getWidth()
        val height = bitmapToScale.getHeight()
        // create a matrix for the manipulation
        val matrix = Matrix()

        // resize the bit map
        matrix.postScale(newWidth / width.toFloat(), newHeight / height.toFloat())

        // recreate the new Bitmap and set it back
        return Bitmap.createBitmap(bitmapToScale, 0, 0, bitmapToScale.getWidth(), bitmapToScale.getHeight(), matrix, true)
    }

    public fun loadTexture(path: String): Boolean {
        var bitmap: Bitmap?
        try {
            var str = path
            if (!path.startsWith("/")) {
                str = "/" + path
            }

            val file = File(context_!!.getExternalFilesDir(null), str)
            if (file.canRead()) {
                bitmap = BitmapFactory.decodeStream(FileInputStream(file))
            } else {
                bitmap = BitmapFactory.decodeStream(context_!!.getResources().getAssets().open(path))
            }
            // Matrix matrix = new Matrix();
            // // resize the bit map
            // matrix.postScale(-1F, 1F);
            //
            // // recreate the new Bitmap and set it back
            // bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
            // bitmap.getHeight(), matrix, true);

        } catch (e: Exception) {
            Log.w("NDKHelper", "Coundn't load a file:" + path)
            return false
        }


        if (bitmap != null) {
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0)
        }
        return true

    }

    public fun openBitmap(path: String, iScalePOT: Boolean): Bitmap {
        var bitmap: Bitmap? = null
        try {
            bitmap = BitmapFactory.decodeStream(context_!!.getResources().getAssets().open(path))
            if (iScalePOT) {
                val originalWidth = getBitmapWidth(bitmap!!)
                val originalHeight = getBitmapHeight(bitmap!!)
                val width = nextPOT(originalWidth)
                val height = nextPOT(originalHeight)
                if (originalWidth != width || originalHeight != height) {
                    // Scale it
                    bitmap = scaleBitmap(bitmap, width.toFloat(), height.toFloat())
                }
            }

        } catch (e: Exception) {
            Log.w("NDKHelper", "Coundn't load a file:" + path)
        }


        return bitmap!!
    }

    public fun getBitmapWidth(bmp: Bitmap): Int {
        return bmp.getWidth()
    }

    public fun getBitmapHeight(bmp: Bitmap): Int {
        return bmp.getHeight()
    }

    public fun getBitmapPixels(bmp: Bitmap, pixels: IntArray) {
        val w = bmp.getWidth()
        val h = bmp.getHeight()
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
    }

    public fun closeBitmap(bmp: Bitmap) {
        bmp.recycle()
    }

    public fun getNativeAudioBufferSize(): Int {
        val SDK_INT = android.os.Build.VERSION.SDK_INT
        if (SDK_INT >= 17) {
            val am = context_!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            return Integer.parseInt(framesPerBuffer)
        } else {
            return 0
        }
    }

    public fun getNativeAudioSampleRate(): Int {
        return AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM)

    }

    class object {
        private var context_: Context? = null

        public fun setContext(c: Context) {
            Log.i("NDKHelper", "setContext:" + c)
            context_ = c
        }

        public fun getNativeLibraryDirectory(appContext: Context): String {
            val ai = context_!!.getApplicationInfo()

            Log.w("NDKHelper", "ai.nativeLibraryDir:" + ai.nativeLibraryDir)

            if ((ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 || (ai.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                return ai.nativeLibraryDir
            }
            return "/system/lib/"
        }
    }

}
