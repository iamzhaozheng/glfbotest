package com.hisrv.glfbotest;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;

public class TestRenderer implements Renderer {

	private int mFrameBufferHandle, mTextureHandle;
	private int mTextureWidth = 256, mTextureHeight = 256;

	@Override
	public void onDrawFrame(GL10 arg0) {
		// TODO Auto-generated method stub
		checkGLError();
		GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		
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

		int[] frameBuffers = new int[1];
		GLES20.glGenFramebuffers(1, frameBuffers, 0);
		mFrameBufferHandle = frameBuffers[0];
		GLES20.glBindFramebuffer(GLES20.GL_TEXTURE_2D, mFrameBufferHandle);
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

}
