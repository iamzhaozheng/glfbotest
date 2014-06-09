package com.hisrv.glfbotest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;

public class TestRenderer implements Renderer {

	private static final int POS_DATA_SIZE = 2;
	private static final int BYTES_PER_FLOAT = 4;
	private int mTextureHandle, mTextureCurveHandle,
			mTextureHighlightCurveHandle;
	private FrameBufferInfo mFrameBufferA, mFrameBufferB, mFrameBufferOriginal;
	private ShaderInfo mOriginalShader, mSmoothBlurHorizontalShader,
			mSmoothBlurVerticalShader, mSmoothExtractionShader,
			mSmoothTemplateShader, mSmoothApplyShader;
	private int mTextureWidth, mTextureHeight, mSurfaceWidth, mSurfaceHeight, mDisplayedTextureWidth, mDisplayedTextureHeight;
	private Context mAppContext;
	private FloatBuffer mFrameTexPos, mDisplayedTexPos;
	private float[] mMVPMatrix = new float[16];
	private float[] mFixMVPMatrix = new float[16];
	private Bitmap mInputBitmap;
	private OnRenderCompleteListener mOnRenderCompleteListener;

	public TestRenderer(Context cx, Bitmap bm) {
		mAppContext = cx.getApplicationContext();
		mInputBitmap = bm;
		mTextureWidth = bm.getWidth();
		mTextureHeight = bm.getHeight();
		mDisplayedTextureWidth = mTextureHeight;
		mDisplayedTextureHeight = mTextureWidth;
		mFrameTexPos = getTextureCoordinate(mTextureWidth, mTextureHeight);
		mDisplayedTexPos = getTextureCoordinate(mDisplayedTextureWidth, mDisplayedTextureHeight);
		initMVPMatrix();
	}
	
	private FloatBuffer getTextureCoordinate(int w, int h) {
		float wr = 1.0f * w
				/ MathUtils.nextPowerOfTwo(w);
		float hr = 1.0f * h
				/ MathUtils.nextPowerOfTwo(h);
		float[] frameTexCoords = new float[] { 0, 0, wr, 0, 0, hr, wr, 0, wr,
				hr, 0, hr };
		FloatBuffer frameTexPos = ByteBuffer
				.allocateDirect(frameTexCoords.length * BYTES_PER_FLOAT)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		frameTexPos.put(frameTexCoords).position(0);
		return frameTexPos;
	}

	@Override
	public void onDrawFrame(GL10 arg0) {
		// TODO Auto-generated method stub
		checkGLError();
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		drawOriginal(mOriginalShader, mFrameBufferOriginal,
				mTextureHandle);
		drawSmoothBlur(mSmoothBlurHorizontalShader, mFrameBufferA,
				mFrameBufferOriginal.textureHandle);
		drawSmoothBlur(mSmoothBlurVerticalShader, mFrameBufferB,
				mFrameBufferA.textureHandle);
//		drawSmoothBlur(mSmoothBlurHorizontalShader, mFrameBufferA,
//				mFrameBufferB.textureHandle);
//		drawSmoothBlur(mSmoothBlurVerticalShader, mFrameBufferB,
//				mFrameBufferA.textureHandle);
//		drawSmoothBlur(mSmoothBlurHorizontalShader, mFrameBufferA,
//				mFrameBufferB.textureHandle);
//		drawSmoothBlur(mSmoothBlurVerticalShader, mFrameBufferB,
//				mFrameBufferA.textureHandle);
		drawExtraction(mSmoothExtractionShader, mFrameBufferA,
				mFrameBufferOriginal.textureHandle, mFrameBufferB.textureHandle);
		drawTemplate(mSmoothTemplateShader, mFrameBufferB,
				mFrameBufferA.textureHandle, mTextureHighlightCurveHandle);
		drawSmoothApply(mSmoothApplyShader, null,
				mFrameBufferOriginal.textureHandle,
				mFrameBufferB.textureHandle, mTextureCurveHandle);
		outputFrameBuffer();
		checkGLError();
	}

	@Override
	public void onSurfaceChanged(GL10 arg0, int width, int height) {
		// TODO Auto-generated method stub
		checkGLError();
		mSurfaceWidth = width;
		mSurfaceHeight = height;
		// GLES20.glViewport(0, 0, width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
		// TODO Auto-generated method stub
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		mOriginalShader = generateShader(R.raw.vertex_shader,
				R.raw.original_shader, "aPosition", "uTexture", "uMVPMatrix");
		mSmoothBlurHorizontalShader = generateShader(
				R.raw.smooth_blur_horizontal_vertex_shader,
				R.raw.smooth_blur_fragment_shader, "position",
				"inputImageTexture", "uMVPMatrix", "texelWidthOffset",
				"texelHeightOffset");
		mSmoothBlurVerticalShader = generateShader(
				R.raw.smooth_blur_vertical_vertex_shader,
				R.raw.smooth_blur_fragment_shader, "position",
				"inputImageTexture", "uMVPMatrix", "texelWidthOffset",
				"texelHeightOffset");
		mSmoothExtractionShader = generateShader(R.raw.vertex_shader,
				R.raw.smooth_extract_selection_fragment_shader, "aPosition",
				"uTexture", "uTextureBlur", "uMVPMatrix");
		mSmoothTemplateShader = generateShader(R.raw.vertex_shader,
				R.raw.smooth_template_fragment_shader, "aPosition",
				"uMVPMatrix", "uTexture", "uTextureCurve");
		mSmoothApplyShader = generateShader(R.raw.vertex_shader,
				R.raw.smooth_apply_fragment_shader, "aPosition", "uMVPMatrix",
				"uTexture", "uTextureTemplate", "uTextureCurve");
		mTextureHandle = TextureHelper.loadSubTexture(mInputBitmap);
		mTextureCurveHandle = TextureHelper.loadCurveTexture(mAppContext,
				"skin_smooth.dat");
		mTextureHighlightCurveHandle = TextureHelper.loadCurveTexture(
				mAppContext, "highlight4.dat");
		mFrameBufferA = generateFrameBuffer();
		mFrameBufferB = generateFrameBuffer();
		mFrameBufferOriginal = generateFrameBuffer();
	}

	private void initMVPMatrix() {
		float[] modelViewMatrix = new float[16];
		Matrix.setLookAtM(modelViewMatrix, 0, 0, 0, 1, 0, 0,
				0, 1, 0, 0);
		float[] projectionMatrix = new float[16];
		float hr = (float) mTextureHeight
				/ MathUtils.nextPowerOfTwo(mTextureHeight);
		float wr = (float) mTextureWidth
				/ MathUtils.nextPowerOfTwo(mTextureWidth);
		Matrix.orthoM(projectionMatrix, 0, -hr, 0, 0, wr, -1, 1);
		Matrix.multiplyMM(mMVPMatrix, 0, projectionMatrix, 0, modelViewMatrix,
				0);
		
		hr = (float) mDisplayedTextureHeight
				/ MathUtils.nextPowerOfTwo(mDisplayedTextureHeight);
		wr = (float) mDisplayedTextureWidth
				/ MathUtils.nextPowerOfTwo(mDisplayedTextureWidth);
		float[] fixModelViewMatrix = new float[16];
		Matrix.setLookAtM(fixModelViewMatrix, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
		float[] fixProjectionMatrix = new float[16];
		Matrix.orthoM(fixProjectionMatrix, 0, 0, wr, 0, hr, 1, -1);
		Matrix.multiplyMM(mFixMVPMatrix, 0, fixProjectionMatrix, 0, fixModelViewMatrix,
				0);
	}

	private ShaderInfo generateShader(int vertexId, int fragmentId,
			String attrib, String... uniforms) {

		int vShader = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER,
				RawResourceReader.readTextFileFromRawResource(mAppContext,
						vertexId));
		int fShader = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER,
				RawResourceReader.readTextFileFromRawResource(mAppContext,
						fragmentId));
		int programHandle = ShaderHelper.createAndLinkProgram(vShader, fShader,
				new String[] { attrib });
		ShaderInfo info = new ShaderInfo(programHandle);
		info.attribute = GLES20.glGetAttribLocation(programHandle, attrib);
		for (int i = 0; i < uniforms.length; i++) {
			info.uniforms.put(uniforms[i],
					GLES20.glGetUniformLocation(programHandle, uniforms[i]));

		}
		return info;
	}

	private FrameBufferInfo generateFrameBuffer() {
		int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);
		int frameBufferTexture = textures[0];
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
				MathUtils.nextPowerOfTwo(mDisplayedTextureWidth),
				MathUtils.nextPowerOfTwo(mDisplayedTextureHeight), 0, GLES20.GL_RGBA,
				GLES20.GL_UNSIGNED_BYTE, null);

		int[] frameBuffers = new int[1];
		GLES20.glGenFramebuffers(1, frameBuffers, 0);
		int frameBufferHandle = frameBuffers[0];
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferHandle);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
				GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
				frameBufferTexture, 0);
		int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
		if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			throw new RuntimeException("frame buffer incompleted " + status);
		}
		return new FrameBufferInfo(frameBufferHandle, frameBufferTexture);
	}

	private void drawOriginal(ShaderInfo shader,
			FrameBufferInfo fbi, int textureHandle) {
		GLES20.glUseProgram(shader.program);
		GLES20.glViewport(0, 0, fbi == null ? mSurfaceWidth : mDisplayedTextureWidth,
				fbi == null ? mSurfaceHeight : mDisplayedTextureHeight);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbi == null ? 0
				: fbi.frameBufferHandle);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
		GLES20.glUniform1i(shader.uniforms.get("uTexture"), 0);

		GLES20.glVertexAttribPointer(shader.attribute, POS_DATA_SIZE,
				GLES20.GL_FLOAT, false, 0, mFrameTexPos.position(0));
		GLES20.glEnableVertexAttribArray(shader.attribute);
		GLES20.glUniformMatrix4fv(shader.uniforms.get("uMVPMatrix"), 1, false,
				mMVPMatrix, 0);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
	}

	private void drawSmoothBlur(ShaderInfo shader,
			FrameBufferInfo fbi, int textureHandle) {
		GLES20.glUseProgram(shader.program);
		GLES20.glViewport(0, 0, fbi == null ? mSurfaceWidth : mDisplayedTextureWidth,
				fbi == null ? mSurfaceHeight : mDisplayedTextureHeight);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbi == null ? 0
				: fbi.frameBufferHandle);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
		GLES20.glUniform1i(shader.uniforms.get("inputImageTexture"), 0);
		GLES20.glUniform1f(shader.uniforms.get("texelWidthOffset"),
				1f / mDisplayedTextureWidth);
		GLES20.glUniform1f(shader.uniforms.get("texelHeightOffset"),
				1f / mDisplayedTextureHeight);

		GLES20.glVertexAttribPointer(shader.attribute, POS_DATA_SIZE,
				GLES20.GL_FLOAT, false, 0, mDisplayedTexPos.position(0));
		GLES20.glEnableVertexAttribArray(shader.attribute);
		GLES20.glUniformMatrix4fv(shader.uniforms.get("uMVPMatrix"), 1, false,
				mFixMVPMatrix, 0);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
	}

	private void drawExtraction(ShaderInfo shader,
			FrameBufferInfo fbi, int textureHandle, int textureBlurHandle) {
		GLES20.glUseProgram(shader.program);
		GLES20.glViewport(0, 0, fbi == null ? mSurfaceWidth : mDisplayedTextureWidth,
				fbi == null ? mSurfaceHeight : mDisplayedTextureHeight);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbi == null ? 0
				: fbi.frameBufferHandle);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
		GLES20.glUniform1i(shader.uniforms.get("uTexture"), 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureBlurHandle);
		GLES20.glUniform1i(shader.uniforms.get("uTextureBlur"), 1);
		GLES20.glVertexAttribPointer(shader.attribute, POS_DATA_SIZE,
				GLES20.GL_FLOAT, false, 0, mDisplayedTexPos.position(0));
		GLES20.glEnableVertexAttribArray(shader.attribute);
		GLES20.glUniformMatrix4fv(shader.uniforms.get("uMVPMatrix"), 1, false,
				mFixMVPMatrix, 0);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
	}

	private void drawTemplate(ShaderInfo shader,
			FrameBufferInfo fbi, int textureHandle, int textureCurveHandle) {
		GLES20.glUseProgram(shader.program);
		GLES20.glViewport(0, 0, fbi == null ? mSurfaceWidth : mDisplayedTextureWidth,
				fbi == null ? mSurfaceHeight : mDisplayedTextureHeight);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbi == null ? 0
				: fbi.frameBufferHandle);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
		GLES20.glUniform1i(shader.uniforms.get("uTexture"), 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureCurveHandle);
		GLES20.glUniform1i(shader.uniforms.get("uTextureCurve"), 1);
		GLES20.glVertexAttribPointer(shader.attribute, POS_DATA_SIZE,
				GLES20.GL_FLOAT, false, 0, mDisplayedTexPos.position(0));
		GLES20.glEnableVertexAttribArray(shader.attribute);
		GLES20.glUniformMatrix4fv(shader.uniforms.get("uMVPMatrix"), 1, false,
				mFixMVPMatrix, 0);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
	}

	private void drawSmoothApply(ShaderInfo shader,
			FrameBufferInfo fbi, int textureHandle, int textureTemplateHandle,
			int textureCurveHandle) {
		GLES20.glUseProgram(shader.program);
		GLES20.glViewport(0, 0, fbi == null ? mSurfaceWidth : mDisplayedTextureWidth,
				fbi == null ? mSurfaceHeight : mDisplayedTextureHeight);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbi == null ? 0
				: fbi.frameBufferHandle);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
		GLES20.glUniform1i(shader.uniforms.get("uTexture"), 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureTemplateHandle);
		GLES20.glUniform1i(shader.uniforms.get("uTextureTemplate"), 1);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureCurveHandle);
		GLES20.glUniform1i(shader.uniforms.get("uTextureCurve"), 2);
		GLES20.glVertexAttribPointer(shader.attribute, POS_DATA_SIZE,
				GLES20.GL_FLOAT, false, 0, mDisplayedTexPos.position(0));
		GLES20.glEnableVertexAttribArray(shader.attribute);
		GLES20.glUniformMatrix4fv(shader.uniforms.get("uMVPMatrix"), 1, false,
				mFixMVPMatrix, 0);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
	}

	private void checkGLError() {
		int error = GLES20.glGetError();
		if (error != GLES20.GL_NO_ERROR) {
			throw new RuntimeException("OpenGL Error: " + error);
		}
	}

	private void outputFrameBuffer() {
		if (mOnRenderCompleteListener != null) {
			IntBuffer buf = IntBuffer.allocate(mDisplayedTextureWidth * mDisplayedTextureHeight);
			GLES20.glReadPixels(0, 0, mDisplayedTextureWidth, mDisplayedTextureHeight,
					GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf.position(0));
			Bitmap reverseBitmap = Bitmap.createBitmap(mDisplayedTextureWidth,
					mDisplayedTextureHeight, Bitmap.Config.ARGB_8888);
			reverseBitmap.copyPixelsFromBuffer(buf);
			android.graphics.Matrix matrix = new android.graphics.Matrix();
			matrix.postScale(1, -1);
			Bitmap outputBitmap = Bitmap.createBitmap(reverseBitmap, 0, 0,
					mDisplayedTextureWidth, mDisplayedTextureHeight, matrix, true);
			mOnRenderCompleteListener.onBitmapComplete(outputBitmap);
			mOnRenderCompleteListener = null;
		}

	}

	public void setOnRenderCompleteListener(OnRenderCompleteListener l) {
		mOnRenderCompleteListener = l;
	}

	public interface OnRenderCompleteListener {
		public void onBitmapComplete(Bitmap bm);
	}

	public static class FrameBufferInfo {
		public int frameBufferHandle;
		public int textureHandle;

		public FrameBufferInfo(int frameBuffer, int texture) {
			frameBufferHandle = frameBuffer;
			textureHandle = texture;
		}
	}

	public static class ShaderInfo {
		public int program;
		public int attribute;
		public HashMap<String, Integer> uniforms = new HashMap<String, Integer>();

		public ShaderInfo(int program) {
			this.program = program;
		}
	}
}
