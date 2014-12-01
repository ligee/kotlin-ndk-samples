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

import android.graphics.SurfaceTexture
import android.util.Log

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.AttributeSet

public class MyGLSurfaceView(context: Context, attributeSet: AttributeSet? = null) : GLSurfaceView(context, attributeSet) {

    var mRenderer: MyRenderer? = null

    {
        init()
    }

    private fun init() {
        setEGLContextClientVersion(2)
        mRenderer = MyRenderer()
        setRenderer(mRenderer)
        Log.i("@@@", "setrenderer")
    }

    override fun onPause() {
        mRenderer!!.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mRenderer!!.onResume()
    }

    public fun getSurfaceTexture(): SurfaceTexture? {
        return mRenderer!!.surfaceTexture
    }
}

class MyRenderer : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    public fun onPause() {
    }

    public fun onResume() {
        mLastTime = SystemClock.elapsedRealtimeNanos()
    }

    override fun onDrawFrame(glUnused: GL10) {
        synchronized (this) {
            if (updateSurface) {
                surfaceTexture!!.updateTexImage()

                surfaceTexture!!.getTransformMatrix(mSTMatrix)
                updateSurface = false
            }
        }

        // Ignore the passed-in GL10 interface, and use the GLES20
        // class's static methods instead.
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(mProgram)
        checkGlError("glUseProgram")

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID)

        mVertices!!.position(VERTICES_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, mVertices)
        checkGlError("glVertexAttribPointer maPosition")
        GLES20.glEnableVertexAttribArray(maPositionHandle)
        checkGlError("glEnableVertexAttribArray maPositionHandle")

        mVertices!!.position(VERTICES_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, mVertices)
        checkGlError("glVertexAttribPointer maTextureHandle")
        GLES20.glEnableVertexAttribArray(maTextureHandle)
        checkGlError("glEnableVertexAttribArray maTextureHandle")

        val now = SystemClock.elapsedRealtimeNanos()
        mRunTime += (now - mLastTime)
        mLastTime = now
        val d = (mRunTime.toDouble()) / 1000000000
        Matrix.setIdentityM(mMMatrix, 0)
        Matrix.rotateM(mMMatrix, 0, 30f, Math.sin(d).toFloat(), Math.cos(d).toFloat(), 0f)
        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0)

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
    }

    override fun onSurfaceChanged(glUnused: GL10, width: Int, height: Int) {
        // Ignore the passed-in GL10 interface, and use the GLES20
        // class's static methods instead.
        GLES20.glViewport(0, 0, width, height)
        mRatio = width.toFloat() / height.toFloat()
        Matrix.frustumM(mProjMatrix, 0, -mRatio, mRatio, (-1).toFloat(), 1f, 3f, 7f)
    }

    override fun onSurfaceCreated(glUnused: GL10, config: EGLConfig) {
        // Ignore the passed-in GL10 interface, and use the GLES20
        // class's static methods instead.

        /* Set up alpha blending and an Android background color */
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glClearColor(0.643.toFloat(), 0.776.toFloat(), 0.223.toFloat(), 1.0.toFloat())

        /* Set up shaders and handles to their variables */
        mProgram = createProgram(mVertexShader, mFragmentShader)
        if (mProgram == 0) {
            return
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition")
        checkGlError("glGetAttribLocation aPosition")
        if (maPositionHandle == -1) {
            throw RuntimeException("Could not get attrib location for aPosition")
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord")
        checkGlError("glGetAttribLocation aTextureCoord")
        if (maTextureHandle == -1) {
            throw RuntimeException("Could not get attrib location for aTextureCoord")
        }

        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        checkGlError("glGetUniformLocation uMVPMatrix")
        if (muMVPMatrixHandle == -1) {
            throw RuntimeException("Could not get attrib location for uMVPMatrix")
        }

        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix")
        checkGlError("glGetUniformLocation uSTMatrix")
        if (muMVPMatrixHandle == -1) {
            throw RuntimeException("Could not get attrib location for uSTMatrix")
        }

        checkGlError("glGetUniformLocation uCRatio")
        if (muMVPMatrixHandle == -1) {
            throw RuntimeException("Could not get attrib location for uCRatio")
        }

        /*
         * Create our texture. This has to be done each time the
         * surface is created.
         */

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)

        mTextureID = textures[0]
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID)
        checkGlError("glBindTexture mTextureID")

        // Can't do mipmapping with camera source
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        // Clamp to edge is the only option
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        checkGlError("glTexParameteri mTextureID")

        /*
         * Create the SurfaceTexture that will feed this textureID, and pass it to the camera
         */

        surfaceTexture = SurfaceTexture(mTextureID)
        surfaceTexture!!.setOnFrameAvailableListener(this)

        Matrix.setLookAtM(mVMatrix, 0, 0f, 0f, 4.toFloat(), 0.toFloat(), 0.toFloat(), 0.toFloat(), 0.toFloat(), 1.0.toFloat(), 0.0.toFloat())

        synchronized (this) {
            updateSurface = false
        }
    }

    synchronized override fun onFrameAvailable(surface: SurfaceTexture) {
        /* For simplicity, SurfaceTexture calls here when it has new
         * data available.  Call may come in from some random thread,
         * so let's be safe and use synchronize. No OpenGL calls can be done here.
         */
        updateSurface = true
        //Log.v(TAG, "onFrameAvailable " + surface.getTimestamp());
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        var shader = GLES20.glCreateShader(shaderType)
        if (shader != 0) {
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":")
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader))
                GLES20.glDeleteShader(shader)
                shader = 0
            }
        }
        return shader
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            return 0
        }
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) {
            return 0
        }

        var program = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader)
            checkGlError("glAttachShader")
            GLES20.glAttachShader(program, pixelShader)
            checkGlError("glAttachShader")
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ")
                Log.e(TAG, GLES20.glGetProgramInfoLog(program))
                GLES20.glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }

    private fun checkGlError(op: String) {
        var error: Int
        while (true) {
            error = GLES20.glGetError()
            if (error == GLES20.GL_NO_ERROR) break
            Log.e(TAG, op + ": glError " + error)
            throw RuntimeException(op + ": glError " + error)
        }
    }

    private val mVerticesData = floatArray(
        // X, Y, Z, U, V
        -1.25f, -1.0f, 0f, 0f, 0f,
         1.25f, -1.0f, 0f, 1f, 0f,
        -1.25f,  1.0f, 0f, 0f, 1f,
         1.25f,  1.0f, 0f, 1f, 1f )

    private var mVertices: FloatBuffer? = null

    private val mVertexShader = "uniform mat4 uMVPMatrix;\n" + "uniform mat4 uSTMatrix;\n" + "attribute vec4 aPosition;\n" + "attribute vec4 aTextureCoord;\n" + "varying vec2 vTextureCoord;\n" + "void main() {\n" + "  gl_Position = uMVPMatrix * aPosition;\n" + "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" + "}\n"

    private val mFragmentShader = "#extension GL_OES_EGL_image_external : require\n" + "precision mediump float;\n" + "varying vec2 vTextureCoord;\n" + "uniform samplerExternalOES sTexture;\n" + "void main() {\n" + "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" + "}\n"

    private val mMVPMatrix = FloatArray(16)
    private val mProjMatrix = FloatArray(16)
    private val mMMatrix = FloatArray(16)
    private val mVMatrix = FloatArray(16)
    private val mSTMatrix = FloatArray(16)

    private var mProgram: Int = 0
    private var mTextureID: Int = 0
    private var muMVPMatrixHandle: Int = 0
    private var muSTMatrixHandle: Int = 0
    private var maPositionHandle: Int = 0
    private var maTextureHandle: Int = 0

    private var mRatio = 1.0.toFloat()
    public var surfaceTexture: SurfaceTexture? = null
        private set
    private var updateSurface = false
    private var mLastTime = (-1).toLong()
    private var mRunTime: Long = 0

    {
        mVertices = ByteBuffer.allocateDirect(mVerticesData.count() * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer()
        mVertices!!.put(mVerticesData).position(0)

        Matrix.setIdentityM(mSTMatrix, 0)
    }

    class object {

        private val FLOAT_SIZE_BYTES = 4
        private val VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
        private val VERTICES_DATA_POS_OFFSET = 0
        private val VERTICES_DATA_UV_OFFSET = 3

        private val TAG = "MyRenderer"

        // Magic key
        private val GL_TEXTURE_EXTERNAL_OES = 36197
    }
}
