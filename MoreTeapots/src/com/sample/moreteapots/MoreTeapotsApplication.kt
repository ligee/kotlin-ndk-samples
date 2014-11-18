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

package com.sample.moreteapots

import javax.microedition.khronos.opengles.GL10

import com.sample.helper.NDKHelper

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.opengl.GLUtils
import android.util.Log
import android.widget.Toast

public class MoreTeapotsApplication : Application() {
    override fun onCreate() {
        context = getApplicationContext()
        NDKHelper.setContext(context!!)
        Log.w("native-activity", "onCreate")

        val pm = getApplicationContext().getPackageManager()
        val ai: ApplicationInfo?
        try {
            ai = pm.getApplicationInfo(this.getPackageName(), 0)
        } catch (e: NameNotFoundException) {
            ai = null
        }

        val applicationName = (if (ai != null) pm.getApplicationLabel(ai) else "(unknown)") as String
        Toast.makeText(this, applicationName, Toast.LENGTH_SHORT).show()
    }

    class object {
        private var context: Context? = null
    }

}
