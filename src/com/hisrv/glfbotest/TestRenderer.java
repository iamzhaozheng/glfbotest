package com.hisrv.glfbotest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;

public class TestRenderer implements Renderer {

	private static final int POS_DATA_SIZE = 2;
	private static final int BYTES_PER_FLOAT = 4;
	private int mFrameBufferHandle, mTextureHandle, mProgramHandle,
			mPositionHandle, mTextureUniformHandle, mMVPMatrixHandle;
	private int mTextureWidth = 256, mTextureHeight = 256;
	private Context mAppContext;
	private FloatBuffer mFrameTexPos;
	private float[] mMVPMatrix = new float[16];
	private Bitmap mInputBitmap;
	private OnRenderCompleteListener mOnRenderCompleteListener;

	public TestRenderer(Context cx, Bitmap bm) {
		mAppContext = cx.getApplicationContext();
		float[] frameTexCoords = new float[] { 0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 0,
				1 };
		mFrameTexPos = ByteBuffer
				.allocateDirect(frameTexCoords.length * BYTES_PER_FLOAT)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mFrameTexPos.put(frameTexCoords).position(0);
		initModelViewMatrix();
		mInputBitmap = bm;
	}

	@Override
	public void onDrawFrame(GL10 arg0) {
		// TODO Auto-generated method stub
		checkGLError();
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		GLES20.glUseProgram(mProgramHandle);
		GLES20.glVertexAttribPointer(mPositionHandle, POS_DATA_SIZE,
				GLES20.GL_FLOAT, false, 0, mFrameTexPos.position(0));
		GLES20.glEnableVertexAttribArray(mPositionHandle);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureHandle);
		GLES20.glUniform1i(mTextureUniformHandle, 0);
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mInputBitmap, 0);
		
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
		if (mOnRenderCompleteListener != null) {
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferHandle);
			IntBuffer buf = IntBuffer.allocate(mTextureWidth * mTextureHeight);
			GLES20.glReadPixels(0, 0, mTextureWidth, mTextureHeight,
					GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
					buf.position(0));
			Bitmap outputBitmap = Bitmap.createBitmap(mTextureWidth, mTextureHeight, Bitmap.Config.ARGB_8888);
			outputBitmap.copyPixelsFromBuffer(buf);
			mOnRenderCompleteListener.onBitmapComplete(outputBitmap);
			mOnRenderCompleteListener = null;
		}
		checkGLError();
	}

	@Override
	public void onSurfaceChanged(GL10 arg0, int width, int height) {
		// TODO Auto-generated method stub
		checkGLError();
		GLES20.glViewport(0, 0, width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
		// TODO Auto-generated method stub
		initShaders();
		initTextures();
		initFrameBuffers();
	}

	private void initShaders() {
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		int vShader = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER,
				RawResourceReader.readTextFileFromRawResource(mAppContext,
						R.raw.vertex_shader));
		int fShader = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER,
				RawResourceReader.readTextFileFromRawResource(mAppContext,
						R.raw.original_shader));
		mProgramHandle = ShaderHelper.createAndLinkProgram(vShader, fShader,
				new String[] { "aPosition" });
		mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle,
				"aPosition");
		mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle,
				"texture");
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle,
				"uMVPMatrix");

	}

	private void initTextures() {
		int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);
		mTextureHandle = textures[0];
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureHandle);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
				mTextureWidth, mTextureHeight, 0, GLES20.GL_RGBA,
				GLES20.GL_UNSIGNED_BYTE, null);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
	}

	private void initFrameBuffers() {
		int[] frameBuffers = new int[1];
		GLES20.glGenFramebuffers(1, frameBuffers, 0);
		mFrameBufferHandle = frameBuffers[0];
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferHandle);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
				GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
				mFrameBufferHandle, 0);
		int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
		if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			throw new RuntimeException("frame buffer incompleted " + status);
		}
	}

	private void checkGLError() {
		int error = GLES20.glGetError();
		if (error != GLES20.GL_NO_ERROR) {
			throw new RuntimeException("OpenGL Error: " + error);
		}
	}

	private void initModelViewMatrix() {
		final float eyeX = 0.0f;
		final float eyeY = 0.0f;
		float eyeZ = -1.0f;

		final float lookX = 0.0f;
		final float lookY = 0.0f;
		final float lookZ = 0.0f;

		float upX = -1.0f;
		float upY = 0.0f;
		final float upZ = 0.0f;

		upX = -1.0f;
		upY = 0.0f;
		float[] modelViewMatrix = new float[16];
		Matrix.setLookAtM(modelViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY,
				lookZ, upX, upY, upZ);
		float[] projectionMatrix = new float[16];
		Matrix.orthoM(projectionMatrix, 0, -1, 0, -1, 0, -10, 20);
		Matrix.multiplyMM(mMVPMatrix, 0, projectionMatrix, 0, modelViewMatrix,
				0);
	}

	public void setOnRenderCompleteListener(OnRenderCompleteListener l) {
		mOnRenderCompleteListener = l;
	}

	public interface OnRenderCompleteListener {
		public void onBitmapComplete(Bitmap bm);
	}
}
