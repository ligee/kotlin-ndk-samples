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

package com.example.nativemedia

import android.app.Activity
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import java.io.IOException
import kotlin.platform.platformStatic
import android.widget.Adapter

public class NativeMedia : Activity() {

    var mSourceString: String? = null
    var mSinkString: String? = null

    // member variables for Java media player
    var mMediaPlayer: MediaPlayer? = null
    var mMediaPlayerIsPrepared = false
    var mSurfaceView1: SurfaceView? = null
    var mSurfaceHolder1: SurfaceHolder? = null

    // member variables for native media player
    var mIsPlayingStreaming = false
    var mSurfaceView2: SurfaceView? = null
    var mSurfaceHolder2: SurfaceHolder? = null

    var mSelectedVideoSink: VideoSink? = null
    var mJavaMediaPlayerVideoSink: VideoSink? = null
    var mNativeMediaPlayerVideoSink: VideoSink? = null

    var mSurfaceHolder1VideoSink: SurfaceHolderVideoSink? = null
    var mSurfaceHolder2VideoSink: SurfaceHolderVideoSink? = null
    var mGLView1VideoSink: GLViewVideoSink? = null
    var mGLView2VideoSink: GLViewVideoSink? = null

    /** Called when the activity is first created. */
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.main)

        mGLView1 = findViewById(R.id.glsurfaceview1) as MyGLSurfaceView
        mGLView2 = findViewById(R.id.glsurfaceview2) as MyGLSurfaceView

        // initialize native media system
        createEngine()

        // set up the Surface 1 video sink
        mSurfaceView1 = findViewById(R.id.surfaceview1) as SurfaceView
        mSurfaceHolder1 = mSurfaceView1!!.getHolder()

        mSurfaceHolder1!!.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.v(TAG, "surfaceChanged format=" + format + ", width=" + width + ", height=" + height)
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.v(TAG, "surfaceCreated")
                setSurface(holder.getSurface())
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.v(TAG, "surfaceDestroyed")
            }

        })

        // set up the Surface 2 video sink
        mSurfaceView2 = findViewById(R.id.surfaceview2) as SurfaceView
        mSurfaceHolder2 = mSurfaceView2!!.getHolder()

        mSurfaceHolder2!!.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.v(TAG, "surfaceChanged format=" + format + ", width=" + width + ", height=" + height)
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.v(TAG, "surfaceCreated")
                setSurface(holder.getSurface())
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.v(TAG, "surfaceDestroyed")
            }

        })

        // create Java media player
        mMediaPlayer = MediaPlayer()

        // set up Java media player listeners
        mMediaPlayer!!.setOnPreparedListener(object : MediaPlayer.OnPreparedListener {

            override fun onPrepared(mediaPlayer: MediaPlayer) {
                val width = mediaPlayer.getVideoWidth()
                val height = mediaPlayer.getVideoHeight()
                Log.v(TAG, "onPrepared width=" + width + ", height=" + height)
                if (width != 0 && height != 0 && mJavaMediaPlayerVideoSink != null) {
                    mJavaMediaPlayerVideoSink!!.setFixedSize(width, height)
                }
                mMediaPlayerIsPrepared = true
                mediaPlayer.start()
            }

        })

        mMediaPlayer!!.setOnVideoSizeChangedListener(object : MediaPlayer.OnVideoSizeChangedListener {

            override fun onVideoSizeChanged(mediaPlayer: MediaPlayer, width: Int, height: Int) {
                Log.v(TAG, "onVideoSizeChanged width=" + width + ", height=" + height)
                if (width != 0 && height != 0 && mJavaMediaPlayerVideoSink != null) {
                    mJavaMediaPlayerVideoSink!!.setFixedSize(width, height)
                }
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

        // initialize video sink spinner
        val sinkSpinner = findViewById(R.id.sink_spinner) as Spinner
        val sinkAdapter = ArrayAdapter.createFromResource(this, R.array.sink_array, android.R.layout.simple_spinner_item)
        sinkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sinkSpinner.setAdapter(sinkAdapter)
        sinkSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                mSinkString = parent.getItemAtPosition(pos).toString()
                Log.v(TAG, "onItemSelected " + mSinkString)
                if ("Surface 1" == mSinkString) {
                    if (mSurfaceHolder1VideoSink == null) {
                        mSurfaceHolder1VideoSink = SurfaceHolderVideoSink(mSurfaceHolder1!!)
                    }
                    mSelectedVideoSink = mSurfaceHolder1VideoSink
                } else if ("Surface 2" == mSinkString) {
                    if (mSurfaceHolder2VideoSink == null) {
                        mSurfaceHolder2VideoSink = SurfaceHolderVideoSink(mSurfaceHolder2!!)
                    }
                    mSelectedVideoSink = mSurfaceHolder2VideoSink
                } else if ("SurfaceTexture 1" == mSinkString) {
                    if (mGLView1VideoSink == null) {
                        mGLView1VideoSink = GLViewVideoSink(mGLView1!!)
                    }
                    mSelectedVideoSink = mGLView1VideoSink
                } else if ("SurfaceTexture 2" == mSinkString) {
                    if (mGLView2VideoSink == null) {
                        mGLView2VideoSink = GLViewVideoSink(mGLView2!!)
                    }
                    mSelectedVideoSink = mGLView2VideoSink
                }
            }

            override fun onNothingSelected(parent: AdapterView<out Adapter>?) {
                Log.v(TAG, "onNothingSelected")
                mSinkString = null
                mSelectedVideoSink = null
            }

        })

        // initialize button click handlers

        // Java MediaPlayer start/pause

        (findViewById(R.id.start_java) as Button).setOnClickListener(object : View.OnClickListener {

            override fun onClick(view: View) {
                if (mJavaMediaPlayerVideoSink == null) {
                    if (mSelectedVideoSink == null) {
                        return
                    }
                    mSelectedVideoSink!!.useAsSinkForJava(mMediaPlayer!!)
                    mJavaMediaPlayerVideoSink = mSelectedVideoSink
                }
                if (!mMediaPlayerIsPrepared) {
                    if (mSourceString != null) {
                        try {
                            mMediaPlayer!!.setDataSource(mSourceString)
                        } catch (e: IOException) {
                            Log.e(TAG, "IOException " + e)
                        }

                        mMediaPlayer!!.prepareAsync()
                    }
                } else if (mMediaPlayer!!.isPlaying()) {
                    mMediaPlayer!!.pause()
                } else {
                    mMediaPlayer!!.start()
                }
            }

        })

        // native MediaPlayer start/pause

        (findViewById(R.id.start_native) as Button).setOnClickListener(object : View.OnClickListener {

            var created = false
            override fun onClick(view: View) {
                if (!created) {
                    if (mNativeMediaPlayerVideoSink == null) {
                        if (mSelectedVideoSink == null) {
                            return
                        }
                        mSelectedVideoSink!!.useAsSinkForNative()
                        mNativeMediaPlayerVideoSink = mSelectedVideoSink
                    }
                    if (mSourceString != null) {
                        created = createStreamingMediaPlayer(mSourceString!!)
                    }
                }
                if (created) {
                    mIsPlayingStreaming = !mIsPlayingStreaming
                    setPlayingStreamingMediaPlayer(mIsPlayingStreaming)
                }
            }

        })

        // finish

        (findViewById(R.id.finish) as Button).setOnClickListener(object : View.OnClickListener {

            override fun onClick(view: View) {
                finish()
            }

        })

        // Java MediaPlayer rewind

        (findViewById(R.id.rewind_java) as Button).setOnClickListener(object : View.OnClickListener {

            override fun onClick(view: View) {
                if (mMediaPlayerIsPrepared) {
                    mMediaPlayer!!.seekTo(0)
                }
            }

        })

        // native MediaPlayer rewind

        (findViewById(R.id.rewind_native) as Button).setOnClickListener(object : View.OnClickListener {

            override fun onClick(view: View) {
                if (mNativeMediaPlayerVideoSink != null) {
                    rewindStreamingMediaPlayer()
                }
            }

        })

    }

    /** Called when the activity is about to be paused. */
    override fun onPause() {
        mIsPlayingStreaming = false
        setPlayingStreamingMediaPlayer(false)
        mGLView1!!.onPause()
        mGLView2!!.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mGLView1!!.onResume()
        mGLView2!!.onResume()
    }

    /** Called when the activity is about to be destroyed. */
    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private var mGLView1: MyGLSurfaceView? = null
    private var mGLView2: MyGLSurfaceView? = null

    // VideoSink abstracts out the difference between Surface and SurfaceTexture
    // aka SurfaceHolder and GLSurfaceView
    abstract class VideoSink {

        abstract fun setFixedSize(width: Int, height: Int)
        abstract fun useAsSinkForJava(mediaPlayer: MediaPlayer)
        abstract fun useAsSinkForNative()

    }

    class SurfaceHolderVideoSink(private val mSurfaceHolder: SurfaceHolder) : VideoSink() {

        override fun setFixedSize(width: Int, height: Int) {
            mSurfaceHolder.setFixedSize(width, height)
        }

        override fun useAsSinkForJava(mediaPlayer: MediaPlayer) {
            // Use the newer MediaPlayer.setSurface(Surface) since API level 14
            // instead of MediaPlayer.setDisplay(mSurfaceHolder) since API level 1,
            // because setSurface also works with a Surface derived from a SurfaceTexture.
            val s = mSurfaceHolder.getSurface()
            mediaPlayer.setSurface(s)
            s.release()
        }

        override fun useAsSinkForNative() {
            val s = mSurfaceHolder.getSurface()
            setSurface(s)
            s.release()
        }

    }

    class GLViewVideoSink(private val mMyGLSurfaceView: MyGLSurfaceView) : VideoSink() {

        override fun setFixedSize(width: Int, height: Int) {
        }

        override fun useAsSinkForJava(mediaPlayer: MediaPlayer) {
            val st = mMyGLSurfaceView.getSurfaceTexture()
            val s = Surface(st)
            mediaPlayer.setSurface(s)
            s.release()
        }

        override fun useAsSinkForNative() {
            val st = mMyGLSurfaceView.getSurfaceTexture()
            val s = Surface(st)
            setSurface(s)
            s.release()
        }

    }

    class object {
        val TAG = "NativeMedia"

        /** Load jni .so on initialization */
        {
            System.loadLibrary("native-media-jni")
        }

        /** Native methods, implemented in jni folder */
        public native platformStatic fun createEngine(): Unit

        public native platformStatic fun createStreamingMediaPlayer(filename: String): Boolean
        public native platformStatic fun setPlayingStreamingMediaPlayer(isPlaying: Boolean): Unit
        public native platformStatic fun shutdown(): Unit
        public native platformStatic fun setSurface(surface: Surface): Unit
        public native platformStatic fun rewindStreamingMediaPlayer(): Unit
    }

}
