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

import android.app.NativeActivity
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager.LayoutParams
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.content.Context
import android.view.ViewGroup

public class MoreTeapotsNativeActivity : NativeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Hide toolbar
        val SDK_INT = android.os.Build.VERSION.SDK_INT
        Log.i("OnCreate", "OnCreate!!!")
        if (SDK_INT >= 19) {
            setImmersiveSticky()
            val decorView = getWindow().getDecorView()
            decorView.setOnSystemUiVisibilityChangeListener(object : View.OnSystemUiVisibilityChangeListener {
                override fun onSystemUiVisibilityChange(visibility: Int) {
                    setImmersiveSticky()
                }
            })
        }

    }

    override fun onResume() {
        super.onResume()

        //Hide toolbar
        val SDK_INT = android.os.Build.VERSION.SDK_INT
        if (SDK_INT >= 11 && SDK_INT < 14) {
            getWindow().getDecorView().setSystemUiVisibility(View.STATUS_BAR_HIDDEN)
        } else if (SDK_INT >= 14 && SDK_INT < 19) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LOW_PROFILE)
        } else if (SDK_INT >= 19) {
            setImmersiveSticky()
        }
    }

    override fun onPause() {
        super.onPause()
        if (_popupWindow != null) {

            _popupWindow!!.dismiss()
            _popupWindow = null
        }
    }
    // Our popup window, you will call it from your C/C++ code later

    fun setImmersiveSticky() {
        val decorView = getWindow().getDecorView()
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    var _activity: MoreTeapotsNativeActivity? = null
    var _popupWindow: PopupWindow? = null
    var _label: TextView? = null

    public fun showUI() {
        if (_popupWindow != null)
            return

        _activity = this

        this.runOnUiThread(object : Runnable {
            override fun run() {
                val layoutInflater = getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val popupView = layoutInflater.inflate(R.layout.widgets, null)
                _popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

                val mainLayout = LinearLayout(_activity)
                val params = MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 0)
                _activity!!.setContentView(mainLayout, params)

                // Show our UI over NativeActivity window
                _popupWindow!!.showAtLocation(mainLayout, Gravity.TOP or Gravity.LEFT, 10, 10)
                _popupWindow!!.update()

                _label = popupView.findViewById(R.id.textViewFPS) as TextView

            }
        })
    }

    public fun updateFPS(fFPS: Float) {
        if (_label == null)
            return

        _activity = this
        this.runOnUiThread(object : Runnable {
            override fun run() {
                _label!!.setText(java.lang.String.format("%2.2f FPS", fFPS))
            }
        })
    }
}
