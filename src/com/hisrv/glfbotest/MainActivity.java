package com.hisrv.glfbotest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.hisrv.glfbotest.TestRenderer.OnRenderCompleteListener;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;

public class MainActivity extends Activity implements OnRenderCompleteListener {

	private final static String TAG = "MainActivity";
	private ImageView mImageView;
	private GLSurfaceView mGLSurfaceView;
	private TestRenderer mTestRenderer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mImageView = (ImageView) findViewById(R.id.image);
		mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glview);
		mGLSurfaceView.setEGLContextClientVersion(2);
		try {
			Bitmap bm = BitmapFactory.decodeStream(getAssets().open("1280.jpg"));
			mTestRenderer = new TestRenderer(this, bm);
			mTestRenderer.setOnRenderCompleteListener(this);
			mGLSurfaceView.setRenderer(mTestRenderer);
			// mImageView.setImageBitmap(bm);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mGLSurfaceView.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mGLSurfaceView.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onBitmapComplete(final Bitmap bm) {
		// TODO Auto-generated method stub
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mImageView.setImageBitmap(bm);
				Log.d(TAG, "bitmap set");
				FileOutputStream fos;
				try {
					fos = new FileOutputStream(
							Environment.getExternalStorageDirectory()
									+ "/pics/test.jpg");
					bm.compress(CompressFormat.JPEG, 90, fos);
					fos.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

}
