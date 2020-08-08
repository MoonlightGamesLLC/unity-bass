/*
	BASS recording example
	Copyright (c) 2002-2019 Un4seen Developments Ltd.
*/

package com.example.rectest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.*;
import java.nio.channels.FileChannel;

import com.un4seen.bass.BASS;

public class RecTest extends Activity {
	static final int FREQ = 44100;
	static final int CHANS = 1;
	static final int BUFSTEP = 200000;    // memory allocation unit

	int rchan; // recording channel
	int chan; // playback channel
	ByteBuffer recbuf; // recording buffer
	int level = 0;
	Handler handler = new Handler();

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
				new AlertDialog.Builder(RecTest.this)
						.setMessage((String) param)
						.setPositiveButton("OK", null)
						.show();
			}
		});
	}

	BASS.RECORDPROC RecordingCallback = new BASS.RECORDPROC() {
		public boolean RECORDPROC(int handle, ByteBuffer buffer, int length, Object user) {
			// buffer the data
			try {
				recbuf.put(buffer);
			} catch (BufferOverflowException e) {
				// increase buffer size
				ByteBuffer temp;
				try {
					temp = ByteBuffer.allocateDirect(recbuf.position() + length + BUFSTEP);
				} catch (Error e2) {
					runOnUiThread(new Runnable() {
						public void run() {
							Error("Out of memory!");
							StopRecording();
						}
					});
					return false;
				}
				temp.order(ByteOrder.LITTLE_ENDIAN);
				recbuf.limit(recbuf.position());
				recbuf.position(0);
				temp.put(recbuf);
				recbuf = temp;
				recbuf.put(buffer);
			}
			return true; // continue recording
		}
	};

	void StartRecording() {
		if (chan != 0) { // free old recording
			BASS.BASS_StreamFree(chan);
			chan = 0;
			findViewById(R.id.play).setEnabled(false);
			findViewById(R.id.save).setEnabled(false);
		}
		// allocate initial buffer and write the WAVE header
		recbuf = ByteBuffer.allocateDirect(BUFSTEP);
		recbuf.order(ByteOrder.LITTLE_ENDIAN);
		recbuf.put(new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' ', 16, 0, 0, 0});
		recbuf.putShort((short) 1);
		recbuf.putShort((short) CHANS);
		recbuf.putInt(FREQ);
		recbuf.putInt(FREQ * CHANS * 2);
		recbuf.putShort((short) 2);
		recbuf.putShort((short) 16);
		recbuf.put(new byte[]{'d', 'a', 't', 'a', 0, 0, 0, 0});
		// start recording
		rchan = BASS.BASS_RecordStart(FREQ, CHANS, 0, RecordingCallback, 0);
		if (rchan == 0) {
			Error("Couldn't start recording");
			return;
		}
		((Button) findViewById(R.id.record)).setText("Stop");
	}

	void StopRecording() {
		BASS.BASS_ChannelStop(rchan);
		rchan = 0;
		recbuf.limit(recbuf.position());
		((Button) findViewById(R.id.record)).setText("Record");
		// complete the WAVE header
		recbuf.putInt(4, recbuf.position() - 8);
		recbuf.putInt(40, recbuf.position() - 44);
		// enable "save" button
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			findViewById(R.id.save).setEnabled(true);
		// create a stream from the recording
		chan = BASS.BASS_StreamCreateFile(recbuf, 0, recbuf.limit(), 0);
		if (chan != 0)
			findViewById(R.id.play).setEnabled(true); // enable "play" button
	}

	public void Record(View v) {
		if (rchan == 0)
			StartRecording();
		else
			StopRecording();
	}

	public void Play(View v) {
		BASS.BASS_ChannelPlay(chan, true); // play the recorded data
	}

	public void Save(View v) {
		File file = new File(Environment.getExternalStorageDirectory(), "bass.wav");
		try {
			FileChannel fc = new FileOutputStream(file).getChannel();
			recbuf.position(0);
			fc.write(recbuf);
			fc.close();
			new AlertDialog.Builder(RecTest.this)
					.setMessage("Saved to:\n" + file.toString())
					.setPositiveButton("OK", null)
					.show();
		} catch (IOException e) {
			Error("Can't save the file");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		if (Build.VERSION.SDK_INT >= 23)
			requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);

		// initialize default recording device
		if (!BASS.BASS_RecordInit(-1)) {
			Error("Can't initialize recording device");
			return;
		}
		// initialize default output device
		if (!BASS.BASS_Init(-1, FREQ, 0))
			Error("Can't initialize output device");

		// timer to update the display
		Runnable timer = new Runnable() {
			public void run() {
				String text = "-";
				int lev = 0;
				if (rchan != 0) { // recording
					text = String.format("%d", BASS.BASS_ChannelGetPosition(rchan, BASS.BASS_POS_BYTE));
					lev = BASS.BASS_ChannelGetLevel(rchan);
				} else if (chan != 0) {
					if (BASS.BASS_ChannelIsActive(chan) != BASS.BASS_ACTIVE_STOPPED) { // playing
						text = String.format("%d\n%d", BASS.BASS_ChannelGetLength(chan, BASS.BASS_POS_BYTE), BASS.BASS_ChannelGetPosition(chan, BASS.BASS_POS_BYTE));
						lev = BASS.BASS_ChannelGetLevel(chan);
					} else
						text = String.format("%d", BASS.BASS_ChannelGetLength(chan, BASS.BASS_POS_BYTE));
				}
				level = level > 2000 ? level - 2000 : 0;
				if (BASS.Utils.LOWORD(lev) > level)
					level = BASS.Utils.LOWORD(lev); // check left level
				if (CHANS > 1 && BASS.Utils.HIWORD(lev) > level)
					level = BASS.Utils.HIWORD(lev); // check right level (if stereo)
				((TextView) findViewById(R.id.position)).setText(text);
				((ProgressBar) findViewById(R.id.level)).setProgress(level);
				handler.postDelayed(this, 50);
			}
		};
		handler.postDelayed(timer, 50);
	}

	@Override
	public void onDestroy() {
		// release everything
		BASS.BASS_RecordFree();
		BASS.BASS_Free();

		super.onDestroy();
	}
}