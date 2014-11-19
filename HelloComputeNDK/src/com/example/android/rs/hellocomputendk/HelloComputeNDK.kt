/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.rs.hellocomputendk

import android.app.Activity
import android.os.Bundle
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.widget.ImageView

public class HelloComputeNDK : Activity() {
    private var mBitmapIn: Bitmap? = null
    private var mBitmapOut: Bitmap? = null

    native fun nativeMono(cacheDir: String, X: Int, Y: Int, `in`: Bitmap, out: Bitmap): Unit = null!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        mBitmapIn = loadBitmap(R.drawable.data)
        mBitmapOut = Bitmap.createBitmap(mBitmapIn!!.getWidth(), mBitmapIn!!.getHeight(), mBitmapIn!!.getConfig())

        val `in` = findViewById(R.id.displayin) as ImageView
        `in`.setImageBitmap(mBitmapIn)

        val out = findViewById(R.id.displayout) as ImageView
        out.setImageBitmap(mBitmapOut)

        nativeMono(this.getCacheDir().toString(), mBitmapIn!!.getWidth(), mBitmapIn!!.getHeight(), mBitmapIn!!, mBitmapOut!!)

    }

    private fun loadBitmap(resource: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        return BitmapFactory.decodeResource(getResources(), resource, options)
    }

    class object {

        {
            System.loadLibrary("RSSupport")
            System.loadLibrary("hellocomputendk")
        }
    }
}
