/*
	BASS "live" spectrum analyser example
	Copyright (c) 2002-2020 Un4seen Developments Ltd.
*/

package com.example.livespec;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.un4seen.bass.BASS;

public class LiveSpec extends Activity {
	int chan; // recording channel
	Handler handler = new Handler();
	SpectrumView specview;

	class RunnableParam implements Runnable {
		Object param;

		RunnableParam(Object p) {
			param = p;
		}

		public void run() {
		}
	}

	// display error messages
	void Error(String es) {
		// get error code in current thread for display in UI thread
		String s = String.format("%s\n(error code: %d)", es, BASS.BASS_ErrorGetCode());
		runOnUiThread(new RunnableParam(s) {
			public void run() {
				new AlertDialog.Builder(LiveSpec.this)
						.setMessage((String) param)
						.setPositiveButton("OK", null)
						.show();
			}
		});
	}

	class SpectrumView extends View {
		private int[] palette;
		private int specmode, specpos; // spectrum mode (and marker pos for 3D mode)
		private int quietcount;
		private int width, height;
		private int dwidth, dheight;
		int specbuf[];

		public SpectrumView(Context context) {
			super(context);

			// setup palette
			palette = new int[256];
			for (int a = 1; a < 128; a++) {
				palette[a] = Color.rgb(2 * a, 256 - 2 * a, 0);
			}
			for (int a = 0; a < 32; a++) {
				palette[128 + a] = Color.rgb(0, 0, 8 * a);
				palette[128 + 32 + a] = Color.rgb(8 * a, 0, 255);
				palette[128 + 64 + a] = Color.rgb(255, 8 * a, 8 * (31 - a));
				palette[128 + 96 + a] = Color.rgb(255, 255, 8 * a);
			}
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			dwidth = w;
			dheight = h;
			width = Math.min(368, dwidth);
			height = Math.min(127, dheight);
			specbuf = new int[height * width];
			specpos = 0;
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				specmode = (specmode + 1) % 4; // change spectrum mode
				Arrays.fill(specbuf, 0); // clear display
				return true;
			}
			return false;
		}

		@Override
		@SuppressWarnings("deprecation")
		protected void onDraw(Canvas canvas) {
			if (specmode == 3) { // waveform
				Arrays.fill(specbuf, 0);
				ByteBuffer bbuf = ByteBuffer.allocateDirect(width * 2); // allocate a buffer for the sample data
				bbuf.order(null); // little-endian byte order
				BASS.BASS_ChannelGetData(chan, bbuf, width * 2); // get the sample data
				short[] pcm = new short[width]; // allocate a "short" array for the sample data
				bbuf.asShortBuffer().get(pcm); // get the data from the buffer into the array
				for (int x = 0, y = 0; x < width; x++) {
					int v = (32767 - pcm[x]) * height / 65536; // invert and scale to fit display
					if (x == 0) y = v;
					do { // draw line from previous sample...
						if (y < v) y++;
						else if (y > v) y--;
						specbuf[y * width + x] = palette[1];
					} while (y != v);
				}
			} else {
				ByteBuffer bbuf = ByteBuffer.allocateDirect(512 * 4); // allocate a buffer for the FFT data
				bbuf.order(null); // little-endian byte order
				BASS.BASS_ChannelGetData(chan, bbuf, BASS.BASS_DATA_FFT1024); // get the FFT data
				float[] fft = new float[512]; // allocate a "float" array for the FFT data
				bbuf.asFloatBuffer().get(fft); // get the data from the buffer into the array
				if (specmode == 0) { // "normal" FFT
					Arrays.fill(specbuf, 0);
					for (int x = 0, y, y1 = 0; x < width / 2; x++) {
						if (true) {
							y = (int) (Math.sqrt(fft[x + 1]) * 3 * height - 4); // scale it (sqrt to make low values more visible)
						} else {
							y = (int) (fft[x + 1] * 10 * height); // scale it (linearly)
						}
						if (y > height) y = height; // cap it
						if (x > 0) { // interpolate from previous to make the display smoother
							y1 = (y + y1) / 2;
							while (--y1 >= 0)
								specbuf[(height - 1 - y1) * width + x * 2 - 1] = palette[y1 * 127 / height + 1];
						}
						y1 = y;
						while (--y >= 0)
							specbuf[(height - 1 - y) * width + x * 2] = palette[y * 127 / height + 1]; // draw level
					}
				} else if (specmode == 1) { // logarithmic, combine bins
					Arrays.fill(specbuf, 0);
					final int BANDS = 28;
					for (int x = 0, b0 = 0; x < BANDS; x++) {
						float peak = 0;
						int b1 = (int) Math.pow(2, x * 9.0 / (BANDS - 1));
						if (b1 <= b0) b1 = b0 + 1; // make sure it uses at least 1 FFT bin
						if (b1 > 511) b1 = 511;
						for (; b0 < b1; b0++)
							if (peak < fft[1 + b0]) peak = fft[1 + b0];
						int y = (int) (Math.sqrt(peak) * 3 * height - 4); // scale it (sqrt to make low values more visible)
						if (y > height) y = height; // cap it
						while (--y >= 0) {
							int s = (height - 1 - y) * width + x * (width / BANDS);
							Arrays.fill(specbuf, s, s + (width / BANDS) * 9 / 10, palette[y * 127 / height + 1]); // draw bar
						}
					}
				} else { // "3D"
					for (int x = 0; x < height; x++) {
						int y = (int) (Math.sqrt(fft[x + 1]) * 3 * 127); // scale it (sqrt to make low values more visible)
						if (y > 127) y = 127; // cap it
						specbuf[(height - 1 - x) * width + specpos] = palette[128 + y]; // plot it
					}
					// move marker onto next position
					specpos = (specpos + 1) % width;
					for (int x = 0; x < height; x++) specbuf[x * width + specpos] = Color.WHITE;
				}
			}
			// update the display
			canvas.drawBitmap(Bitmap.createBitmap(specbuf, width, height, Bitmap.Config.ARGB_8888),
					new Rect(0, 0, width, height), new Rect(0, 0, dwidth, dheight), null);

			if (BASS.Utils.LOWORD(BASS.BASS_ChannelGetLevel(chan)) < 1000) { // check if it's quiet
				quietcount++;
				if (quietcount > 20 && (quietcount & 8) != 0) { // it's been quiet for over a second
					Paint p = new Paint();
					p.setColor(Color.WHITE);
					p.setTextAlign(Align.CENTER);
					p.setTextSize(Math.min(dwidth / 20, dheight / 16));
					canvas.drawText("make some noise!", dwidth / 2, dheight / 2, p);
				}
			} else
				quietcount = 0; // not quiet
		}
	}

	// Recording callback - not doing anything with the data
	BASS.RECORDPROC RecordProc = new BASS.RECORDPROC() {
		public boolean RECORDPROC(int handle, ByteBuffer buffer, int length, Object user) {
			return true; // continue recording
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		specview = new SpectrumView(this);
		setContentView(specview);

		if (Build.VERSION.SDK_INT >= 23)
			requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 0);

		// initialize default recording device
		if (!BASS.BASS_RecordInit(-1)) {
			Error("Can't initialize device");
			return;
		}
		// start recording (44100hz mono 16-bit)
		chan = BASS.BASS_RecordStart(44100, 1, 0, RecordProc, null);
		if (chan == 0) {
			Error("Can't start recording");
			return;
		}

		// timer to update the display
		Runnable timer = new Runnable() {
			public void run() {
				specview.invalidate();
				handler.postDelayed(this, 50);
			}
		};
		handler.postDelayed(timer, 50);
	}

	@Override
	public void onDestroy() {
		BASS.BASS_RecordFree();

		super.onDestroy();
	}
}