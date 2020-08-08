/*
	BASS simple DSP test
	Copyright (c) 2000-2019 Un4seen Developments Ltd.
*/

package com.example.dsptest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import com.un4seen.bass.BASS;

public class DSPTest extends Activity {
	int chan;                // channel handle
	boolean fixeddsp;        // fixed-point DSP
	File filepath;
	String[] filelist;

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
				new AlertDialog.Builder(DSPTest.this)
						.setMessage((String) param)
						.setPositiveButton("OK", null)
						.show();
			}
		});
	}

	// "rotate"
	int rotdsp;        // DSP handle
	float[] rotpos = new float[2];    // sin/cos pos
	int[] rotposx = new int[2];    // sin/cos pos (fixed-point, 2.30 bit)

	BASS.DSPPROC Rotate = new BASS.DSPPROC() {
		public void DSPPROC(int handle, int channel, ByteBuffer buffer, int length, Object user) {
			buffer.order(null); // little-endian
			if (fixeddsp) { // fixed-point data
				IntBuffer ibuffer = buffer.asIntBuffer();
				int[] d = new int[length / 4]; // allocate array for data
				ibuffer.get(d); // copy data from buffer to array
				for (int a = 0; a < length / 4; a += 2) {
					d[a] = (int) (((long) d[a] * Math.abs(rotposx[0])) >> 30);
					d[a + 1] = (int) (((long) d[a + 1] * Math.abs(rotposx[1])) >> 30);
					rotposx[0] -= rotposx[1] >> 16;
					rotposx[1] += rotposx[0] >> 16;
				}
				ibuffer.rewind();
				ibuffer.put(d); // copy modified data back to buffer
			} else { // floating-point data
				FloatBuffer ibuffer = buffer.asFloatBuffer();
				float[] d = new float[length / 4]; // allocate array for data
				ibuffer.get(d); // copy data from buffer to array
				for (int a = 0; a < length / 4; a += 2) {
					d[a] = d[a] * rotpos[0];
					d[a + 1] = d[a + 1] * rotpos[1];
					rotpos[0] -= rotpos[1] * 0.000015f;
					rotpos[1] += rotpos[0] * 0.000015f;
				}
				ibuffer.rewind();
				ibuffer.put(d); // copy modified data back to buffer
			}
		}
	};

	// "echo"
	int echdsp = 0;    // DSP handle
	static final int ECHBUFLEN = 1200;    // buffer length
	float[][] echbuf;    // buffer
	int[][] echbufx;    // buffer (fixed-point)
	int echpos;    // cur.pos

	BASS.DSPPROC Echo = new BASS.DSPPROC() {
		public void DSPPROC(int handle, int channel, ByteBuffer buffer, int length, Object user) {
			buffer.order(null); // little-endian
			if (fixeddsp) { // fixed-point data
				IntBuffer ibuffer = buffer.asIntBuffer();
				int[] d = new int[length / 4]; // allocate array for data
				ibuffer.get(d); // copy data from buffer to array
				for (int a = 0; a < length / 4; a += 2) {
					int l = d[a] + (echbufx[echpos][1] / 2);
					int r = d[a + 1] + (echbufx[echpos][0] / 2);
					if (true) { // false=echo, true=basic "bathroom" reverb
						echbufx[echpos][0] = d[a] = l;
						echbufx[echpos][1] = d[a + 1] = r;
					} else {
						echbufx[echpos][0] = d[a];
						echbufx[echpos][1] = d[a + 1];
					}
					d[a] = l;
					d[a + 1] = r;
					echpos++;
					if (echpos == ECHBUFLEN) echpos = 0;
				}
				ibuffer.rewind();
				ibuffer.put(d); // copy modified data back to buffer
			} else { // floating-point data
				FloatBuffer ibuffer = buffer.asFloatBuffer();
				float[] d = new float[length / 4]; // allocate array for data
				ibuffer.get(d); // copy data from buffer to array
				for (int a = 0; a < length / 4; a += 2) {
					float l = d[a] + (echbuf[echpos][1] / 2);
					float r = d[a + 1] + (echbuf[echpos][0] / 2);
					if (true) { // false=echo, true=basic "bathroom" reverb
						echbuf[echpos][0] = d[a] = l;
						echbuf[echpos][1] = d[a + 1] = r;
					} else {
						echbuf[echpos][0] = d[a];
						echbuf[echpos][1] = d[a + 1];
					}
					d[a] = l;
					d[a + 1] = r;
					echpos++;
					if (echpos == ECHBUFLEN) echpos = 0;
				}
				ibuffer.rewind();
				ibuffer.put(d); // copy modified data back to buffer
			}
		}
	};

	// "flanger"
	int fladsp = 0;    // DSP handle
	static final int FLABUFLEN = 350;    // buffer length
	float[][] flabuf;    // buffer
	int[][] flabufx;    // buffer (fixed-point)
	int flapos;    // cur.pos
	float flas, flasinc;    // sweep pos/increment
	int flasx, flasincx;    // sweep pos/increment (fixed-point, 23.9 bit)

	BASS.DSPPROC Flange = new BASS.DSPPROC() {
		public void DSPPROC(int handle, int channel, ByteBuffer buffer, int length, Object user) {
			buffer.order(null); // little-endian
			if (fixeddsp) { // fixed-point data
				IntBuffer ibuffer = buffer.asIntBuffer();
				int[] d = new int[length / 4]; // allocate array for data
				ibuffer.get(d); // copy data from buffer to array
				for (int a = 0; a < length / 4; a += 2) {
					int p1 = (flapos + (flasx >> 9)) % FLABUFLEN;
					int p2 = (p1 + 1) % FLABUFLEN;
					int f = flasx & 511;
					int s;
					s = (d[a] + flabufx[p1][0] + (int) (((long) (flabufx[p2][0] - flabufx[p1][0]) * f) >> 9)) * 7 / 10;
					flabufx[flapos][0] = d[a];
					d[a] = s;
					s = (d[a + 1] + flabufx[p1][1] + (int) (((long) (flabufx[p2][1] - flabufx[p1][1]) * f) >> 9)) * 7 / 10;
					flabufx[flapos][1] = d[a + 1];
					d[a + 1] = s;
					flapos++;
					if (flapos == FLABUFLEN) flapos = 0;
					flasx += flasincx;
					if (flasx < 0 || flasx > (FLABUFLEN << 9) - 1) {
						flasincx = -flasincx;
						flasx += flasincx;
					}
				}
				ibuffer.rewind();
				ibuffer.put(d); // copy modified data back to buffer
			} else { // floating-point data
				FloatBuffer ibuffer = buffer.asFloatBuffer();
				float[] d = new float[length / 4]; // allocate array for data
				ibuffer.get(d); // copy data from buffer to array
				for (int a = 0; a < length / 4; a += 2) {
					int p1 = (flapos + (int) flas) % FLABUFLEN;
					int p2 = (p1 + 1) % FLABUFLEN;
					float f = flas - (int) flas;
					float s;
					s = (d[a] + flabuf[p1][0] + (flabuf[p2][0] - flabuf[p1][0]) * f) * 0.7f;
					flabuf[flapos][0] = d[a];
					d[a] = s;
					s = (d[a + 1] + flabuf[p1][1] + (flabuf[p2][1] - flabuf[p1][1]) * f) * 0.7f;
					flabuf[flapos][1] = d[a + 1];
					d[a + 1] = s;
					flapos++;
					if (flapos == FLABUFLEN) flapos = 0;
					flas += flasinc;
					if (flas < 0 || flas > FLABUFLEN - 1) {
						flasinc = -flasinc;
						flas += flasinc;
					}
				}
				ibuffer.rewind();
				ibuffer.put(d); // copy modified data back to buffer
			}
		}
	};

	public void OpenClicked(View v) {
		String[] list = filepath.list();
		if (list == null) list = new String[0];
		if (!filepath.getPath().equals("/")) {
			filelist = new String[list.length + 1];
			filelist[0] = "..";
			System.arraycopy(list, 0, filelist, 1, list.length);
		} else
			filelist = list;
		Arrays.sort(filelist, String.CASE_INSENSITIVE_ORDER);
		new AlertDialog.Builder(this)
				.setTitle("Choose a file to play")
				.setItems(filelist, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						File sel;
						if (filelist[which].equals("..")) sel = filepath.getParentFile();
						else sel = new File(filepath, filelist[which]);
						if (sel.isDirectory()) {
							filepath = sel;
							OpenClicked(null);
						} else {
							String file = sel.getPath();
							// first free the current one (try both stream and MOD - it must be one of them)
							if (!BASS.BASS_StreamFree(chan))
								BASS.BASS_MusicFree(chan);
							if ((chan = BASS.BASS_StreamCreateFile(file, 0, 0, BASS.BASS_SAMPLE_LOOP)) == 0
									&& (chan = BASS.BASS_MusicLoad(file, 0, 0, BASS.BASS_SAMPLE_LOOP | BASS.BASS_MUSIC_RAMP, 1)) == 0) {
								// whatever it is, it ain't playable
								((Button) findViewById(R.id.open)).setText("press here to open a file");
								Error("Can't play the file");
								return;
							}
							((Button) findViewById(R.id.open)).setText(file);
							// setup the DSP and start playing
							RotateClicked(findViewById(R.id.rotate));
							EchoClicked(findViewById(R.id.echo));
							FlangerClicked(findViewById(R.id.flanger));
							BASS.BASS_ChannelPlay(chan, false);
						}
					}
				})
				.show();
	}

	public void RotateClicked(View v) {
		if (((CheckBox) v).isChecked()) {
			if (fixeddsp)
				rotposx[0] = rotposx[1] = 759250125; // sin(PI/4)
			else
				rotpos[0] = rotpos[1] = (float) Math.sin(Math.PI / 4);
			rotdsp = BASS.BASS_ChannelSetDSP(chan, Rotate, 0, 2);
		} else
			BASS.BASS_ChannelRemoveDSP(chan, rotdsp);
	}

	public void EchoClicked(View v) {
		if (((CheckBox) v).isChecked()) {
			if (fixeddsp)
				echbufx = new int[ECHBUFLEN][2];
			else
				echbuf = new float[ECHBUFLEN][2];
			echpos = 0;
			echdsp = BASS.BASS_ChannelSetDSP(chan, Echo, 0, 1);
		} else
			BASS.BASS_ChannelRemoveDSP(chan, echdsp);
	}

	public void FlangerClicked(View v) {
		if (((CheckBox) v).isChecked()) {
			if (fixeddsp) {
				flabufx = new int[FLABUFLEN][2];
				flasx = (FLABUFLEN / 2) << 9;
				flasincx = 1;
			} else {
				flabuf = new float[FLABUFLEN][2];
				flas = FLABUFLEN / 2;
				flasinc = 0.002f;
			}
			flapos = 0;
			fladsp = BASS.BASS_ChannelSetDSP(chan, Flange, 0, 0);
		} else
			BASS.BASS_ChannelRemoveDSP(chan, fladsp);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		if (Build.VERSION.SDK_INT >= 23)
			requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);

		filepath = Environment.getExternalStorageDirectory();

		// enable floating-point (or 8.24 fixed-point) DSP
		BASS.BASS_SetConfig(BASS.BASS_CONFIG_FLOATDSP, 1);
		fixeddsp = (BASS.BASS_GetConfig(BASS.BASS_CONFIG_FLOAT) != 1);

		// initialize default output device
		if (!BASS.BASS_Init(-1, 44100, 0)) {
			Error("Can't initialize device");
			return;
		}
	}

	@Override
	public void onDestroy() {
		BASS.BASS_Free();

		super.onDestroy();
	}
}