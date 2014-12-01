/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.nativecodec

import android.app.Activity
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.RadioButton
import android.widget.Spinner

import java.io.IOException
import kotlin.platform.platformStatic
import android.widget.Adapter

public class NativeCodec : Activity() {

    var mSourceString: String? = null

    var mSurfaceView1: SurfaceView? = null
    var mSurfaceHolder1: SurfaceHolder? = null

    var mSelectedVideoSink: VideoSink? = null
    var mNativeCodecPlayerVideoSink: VideoSink? = null

    var mSurfaceHolder1VideoSink: SurfaceHolderVideoSink? = null
    var mGLView1VideoSink: GLViewVideoSink? = null

    var mCreated = false
    var mIsPlaying = false

    /** Called when the activity is first created. */
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.main)

        mGLView1 = findViewById(R.id.glsurfaceview1) as MyGLSurfaceView

        // set up the Surface 1 video sink
        mSurfaceView1 = findViewById(R.id.surfaceview1) as SurfaceView
        mSurfaceHolder1 = mSurfaceView1!!.getHolder()

        mSurfaceHolder1!!.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.v(TAG, "surfaceChanged format=" + format + ", width=" + width + ", height=" + height)
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.v(TAG, "surfaceCreated")
                if (mRadio1!!.isChecked()) {
                    setSurface(holder.getSurface())
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.v(TAG, "surfaceDestroyed")
            }

        })

        // initialize content source spinner
        val sourceSpinner = findViewById(R.id.source_spinner) as Spinner
        val sourceAdapter = ArrayAdapter.createFromResource(this, R.array.source_array, android.R.layout.simple_spinner_item)
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sourceSpinner.setAdapter(sourceAdapter)
        sourceSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                mSourceString = parent.getItemAtPosition(pos).toString()
                Log.v(TAG, "onItemSelected " + mSourceString)
            }

            override fun onNothingSelected(parent: AdapterView<out Adapter>?) {
                Log.v(TAG, "onNothingSelected")
                mSourceString = null
            }

        })

        mRadio1 = findViewById(R.id.radio1) as RadioButton
        mRadio2 = findViewById(R.id.radio2) as RadioButton

        val checklistener = object : CompoundButton.OnCheckedChangeListener {

            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                Log.i("@@@@", "oncheckedchanged")
                if (buttonView == mRadio1 && isChecked) {
                    mRadio2!!.setChecked(false)
                }
                if (buttonView == mRadio2 && isChecked) {
                    mRadio1!!.setChecked(false)
                }
                if (isChecked) {
                    if (mRadio1!!.isChecked()) {
                        if (mSurfaceHolder1VideoSink == null) {
                            mSurfaceHolder1VideoSink = SurfaceHolderVideoSink(mSurfaceHolder1!!)
                        }
                        mSelectedVideoSink = mSurfaceHolder1VideoSink
                        mGLView1!!.onPause()
                        Log.i("@@@@", "glview pause")
                    } else {
                        mGLView1!!.onResume()
                        if (mGLView1VideoSink == null) {
                            mGLView1VideoSink = GLViewVideoSink(mGLView1!!)
                        }
                        mSelectedVideoSink = mGLView1VideoSink
                    }
                    switchSurface()
                }
            }
        }
        mRadio1!!.setOnCheckedChangeListener(checklistener)
        mRadio2!!.setOnCheckedChangeListener(checklistener)
        mRadio2!!.toggle()

        // the surfaces themselves are easier targets than the radio buttons
        mSurfaceView1!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                mRadio1!!.toggle()
            }
        })
        mGLView1!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                mRadio2!!.toggle()
            }
        })

        // initialize button click handlers

        // native MediaPlayer start/pause
        (findViewById(R.id.start_native) as Button).setOnClickListener(object : View.OnClickListener {

            override fun onClick(view: View) {
                if (!mCreated) {
                    if (mNativeCodecPlayerVideoSink == null) {
                        if (mSelectedVideoSink == null) {
                            return
                        }
                        mSelectedVideoSink!!.useAsSinkForNative()
                        mNativeCodecPlayerVideoSink = mSelectedVideoSink
                    }
                    if (mSourceString != null) {
                        mCreated = createStreamingMediaPlayer(mSourceString!!)
                    }
                }
                if (mCreated) {
                    mIsPlaying = !mIsPlaying
                    setPlayingStreamingMediaPlayer(mIsPlaying)
                }
            }

        })


        // native MediaPlayer rewind
        (findViewById(R.id.rewind_native) as Button).setOnClickListener(object : View.OnClickListener {

            override fun onClick(view: View) {
                if (mNativeCodecPlayerVideoSink != null) {
                    rewindStreamingMediaPlayer()
                }
            }

        })

    }

    fun switchSurface() {
        if (mCreated && mNativeCodecPlayerVideoSink != mSelectedVideoSink) {
            // shutdown and recreate on other surface
            Log.i("@@@", "shutting down player")
            shutdown()
            mCreated = false
            mSelectedVideoSink!!.useAsSinkForNative()
            mNativeCodecPlayerVideoSink = mSelectedVideoSink
            if (mSourceString != null) {
                Log.i("@@@", "recreating player")
                mCreated = createStreamingMediaPlayer(mSourceString!!)
                mIsPlaying = false
            }
        }
    }

    /** Called when the activity is about to be paused. */
    override fun onPause() {
        mIsPlaying = false
        setPlayingStreamingMediaPlayer(false)
        mGLView1!!.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (mRadio2!!.isChecked()) {
            mGLView1!!.onResume()
        }
    }

    /** Called when the activity is about to be destroyed. */
    override fun onDestroy() {
        shutdown()
        mCreated = false
        super.onDestroy()
    }

    private var mGLView1: MyGLSurfaceView? = null

    private var mRadio1: RadioButton? = null

    private var mRadio2: RadioButton? = null

    // VideoSink abstracts out the difference between Surface and SurfaceTexture
    // aka SurfaceHolder and GLSurfaceView
    abstract class VideoSink {

        abstract fun setFixedSize(width: Int, height: Int)
        abstract fun useAsSinkForNative()

    }

    class SurfaceHolderVideoSink(private val mSurfaceHolder: SurfaceHolder) : VideoSink() {

        override fun setFixedSize(width: Int, height: Int) {
            mSurfaceHolder.setFixedSize(width, height)
        }

        override fun useAsSinkForNative() {
            val s = mSurfaceHolder.getSurface()
            Log.i("@@@", "setting surface " + s)
            setSurface(s)
        }

    }

    class GLViewVideoSink(private val mMyGLSurfaceView: MyGLSurfaceView) : VideoSink() {

        override fun setFixedSize(width: Int, height: Int) {
        }

        override fun useAsSinkForNative() {
            val st = mMyGLSurfaceView.getSurfaceTexture()
            val s = Surface(st)
            setSurface(s)
            s.release()
        }

    }

    class object {
        val TAG = "NativeCodec"

        /** Native methods, implemented in jni folder */
        public native platformStatic fun createEngine(): Unit = null!!

        public native platformStatic fun createStreamingMediaPlayer(filename: String): Boolean = null!!
        public native platformStatic fun setPlayingStreamingMediaPlayer(isPlaying: Boolean): Unit = null!!
        public native platformStatic fun shutdown(): Unit = null!!
        public native platformStatic fun setSurface(surface: Surface): Unit = null!!
        public native platformStatic fun rewindStreamingMediaPlayer(): Unit = null!!

                /** Load jni .so on initialization */
        {
            System.loadLibrary("native-codec-jni")
        }
    }

}
