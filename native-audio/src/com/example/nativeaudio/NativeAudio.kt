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

package com.example.nativeaudio

import android.app.Activity
import android.content.res.AssetManager
import android.os.Bundle
//import android.util.Log;
import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Spinner
import android.widget.Toast
import android.widget.Adapter
import kotlin.platform.platformStatic

public class NativeAudio : Activity() {

    /** Called when the activity is first created. */
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.main)

        assetManager = getAssets()

        // initialize native audio system

        createEngine()
        createBufferQueueAudioPlayer()

        // initialize URI spinner
        val uriSpinner = findViewById(R.id.uri_spinner) as Spinner
        val uriAdapter = ArrayAdapter.createFromResource(this, R.array.uri_spinner_array, android.R.layout.simple_spinner_item)
        uriAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        uriSpinner.setAdapter(uriAdapter)
        uriSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                URI = parent.getItemAtPosition(pos).toString()
            }

            override fun onNothingSelected(parent: AdapterView<out Adapter>?) {
                URI = null
            }

        })

        // initialize button click handlers

        (findViewById(R.id.hello) as Button).setOnClickListener(object : OnClickListener {
            override fun onClick(view: View) {
                // ignore the return value
                selectClip(CLIP_HELLO, 5)
            }
        })

        (findViewById(R.id.android) as Button).setOnClickListener(object : OnClickListener {
            override fun onClick(view: View) {
                // ignore the return value
                selectClip(CLIP_ANDROID, 7)
            }
        })

        (findViewById(R.id.sawtooth) as Button).setOnClickListener(object : OnClickListener {
            override fun onClick(view: View) {
                // ignore the return value
                selectClip(CLIP_SAWTOOTH, 1)
            }
        })

        (findViewById(R.id.reverb) as Button).setOnClickListener(object : OnClickListener {
            var enabled = false
            override fun onClick(view: View) {
                enabled = !enabled
                if (!enableReverb(enabled)) {
                    enabled = !enabled
                }
            }
        })

        (findViewById(R.id.embedded_soundtrack) as Button).setOnClickListener(object : OnClickListener {
            var created = false
            override fun onClick(view: View) {
                if (!created) {
                    created = createAssetAudioPlayer(assetManager!!, "background.mp3")
                }
                if (created) {
                    isPlayingAsset = !isPlayingAsset
                    setPlayingAssetAudioPlayer(isPlayingAsset)
                }
            }
        })

        (findViewById(R.id.uri_soundtrack) as Button).setOnClickListener(object : OnClickListener {
            var created = false
            override fun onClick(view: View) {
                if (!created && URI != null) {
                    created = createUriAudioPlayer(URI!!)
                }
            }
        })

        (findViewById(R.id.pause_uri) as Button).setOnClickListener(object : OnClickListener {
            override fun onClick(view: View) {
                setPlayingUriAudioPlayer(false)
            }
        })

        (findViewById(R.id.play_uri) as Button).setOnClickListener(object : OnClickListener {
            override fun onClick(view: View) {
                setPlayingUriAudioPlayer(true)
            }
        })

        (findViewById(R.id.loop_uri) as Button).setOnClickListener(object : OnClickListener {
            var isLooping = false
            override fun onClick(view: View) {
                isLooping = !isLooping
                setLoopingUriAudioPlayer(isLooping)
            }
        })

        (findViewById(R.id.mute_left_uri) as Button).setOnClickListener(object : OnClickListener {
            var muted = false
            override fun onClick(view: View) {
                muted = !muted
                setChannelMuteUriAudioPlayer(0, muted)
            }
        })

        (findViewById(R.id.mute_right_uri) as Button).setOnClickListener(object : OnClickListener {
            var muted = false
            override fun onClick(view: View) {
                muted = !muted
                setChannelMuteUriAudioPlayer(1, muted)
            }
        })

        (findViewById(R.id.solo_left_uri) as Button).setOnClickListener(object : OnClickListener {
            var soloed = false
            override fun onClick(view: View) {
                soloed = !soloed
                setChannelSoloUriAudioPlayer(0, soloed)
            }
        })

        (findViewById(R.id.solo_right_uri) as Button).setOnClickListener(object : OnClickListener {
            var soloed = false
            override fun onClick(view: View) {
                soloed = !soloed
                setChannelSoloUriAudioPlayer(1, soloed)
            }
        })

        (findViewById(R.id.mute_uri) as Button).setOnClickListener(object : OnClickListener {
            var muted = false
            override fun onClick(view: View) {
                muted = !muted
                setMuteUriAudioPlayer(muted)
            }
        })

        (findViewById(R.id.enable_stereo_position_uri) as Button).setOnClickListener(object : OnClickListener {
            var enabled = false
            override fun onClick(view: View) {
                enabled = !enabled
                enableStereoPositionUriAudioPlayer(enabled)
            }
        })

        (findViewById(R.id.channels_uri) as Button).setOnClickListener(object : OnClickListener {
            override fun onClick(view: View) {
                if (numChannelsUri == 0) {
                    numChannelsUri = getNumChannelsUriAudioPlayer()
                }
                Toast.makeText(this@NativeAudio, "Channels: " + numChannelsUri, Toast.LENGTH_SHORT).show()
            }
        })

        (findViewById(R.id.volume_uri) as SeekBar).setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            var lastProgress = 100
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                assert(progress >= 0 && progress <= 100)
                lastProgress = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val attenuation = 100 - lastProgress
                val millibel = attenuation * -50
                setVolumeUriAudioPlayer(millibel)
            }
        })

        (findViewById(R.id.pan_uri) as SeekBar).setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            var lastProgress = 100
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                assert(progress >= 0 && progress <= 100)
                lastProgress = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val permille = (lastProgress - 50) * 20
                setStereoPositionUriAudioPlayer(permille)
            }
        })

        (findViewById(R.id.record) as Button).setOnClickListener(object : OnClickListener {
            var created = false
            override fun onClick(view: View) {
                if (!created) {
                    created = createAudioRecorder()
                }
                if (created) {
                    startRecording()
                }
            }
        })

        (findViewById(R.id.playback) as Button).setOnClickListener(object : OnClickListener {
            override fun onClick(view: View) {
                // ignore the return value
                selectClip(CLIP_PLAYBACK, 3)
            }
        })

    }

    /** Called when the activity is about to be destroyed. */
    override fun onPause() {
        // turn off all audio
        selectClip(CLIP_NONE, 0)
        isPlayingAsset = false
        setPlayingAssetAudioPlayer(false)
        isPlayingUri = false
        setPlayingUriAudioPlayer(false)
        super.onPause()
    }

    /** Called when the activity is about to be destroyed. */
    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    class object {

        //static final String TAG = "NativeAudio";

        val CLIP_NONE = 0
        val CLIP_HELLO = 1
        val CLIP_ANDROID = 2
        val CLIP_SAWTOOTH = 3
        val CLIP_PLAYBACK = 4

        var URI: String? = null
        var assetManager: AssetManager? = null

        var isPlayingAsset = false
        var isPlayingUri = false

        var numChannelsUri = 0

        /** Native methods, implemented in jni folder */
        public platformStatic native fun createEngine(): Unit = null!!

        public platformStatic native fun createBufferQueueAudioPlayer(): Unit = null!!
        public platformStatic native fun createAssetAudioPlayer(assetManager: AssetManager, filename: String): Boolean = null!!
        // true == PLAYING, false == PAUSED = null!!
        public platformStatic native fun setPlayingAssetAudioPlayer(isPlaying: Boolean): Unit = null!!

        public platformStatic native fun createUriAudioPlayer(uri: String): Boolean = null!!
        public platformStatic native fun setPlayingUriAudioPlayer(isPlaying: Boolean): Unit = null!!
        public platformStatic native fun setLoopingUriAudioPlayer(isLooping: Boolean): Unit = null!!
        public platformStatic native fun setChannelMuteUriAudioPlayer(chan: Int, mute: Boolean): Unit = null!!
        public platformStatic native fun setChannelSoloUriAudioPlayer(chan: Int, solo: Boolean): Unit = null!!
        public platformStatic native fun getNumChannelsUriAudioPlayer(): Int = null!!
        public platformStatic native fun setVolumeUriAudioPlayer(millibel: Int): Unit = null!!
        public platformStatic native fun setMuteUriAudioPlayer(mute: Boolean): Unit = null!!
        public platformStatic native fun enableStereoPositionUriAudioPlayer(enable: Boolean): Unit = null!!
        public platformStatic native fun setStereoPositionUriAudioPlayer(permille: Int): Unit = null!!
        public platformStatic native fun selectClip(which: Int, count: Int): Boolean = null!!
        public platformStatic native fun enableReverb(enabled: Boolean): Boolean = null!!
        public platformStatic native fun createAudioRecorder(): Boolean = null!!
        public platformStatic native fun startRecording(): Unit = null!!
        public platformStatic native fun shutdown(): Unit = null!!

                /** Load jni .so on initialization */
        {
            System.loadLibrary("native-audio-jni")
        }
    }

}
