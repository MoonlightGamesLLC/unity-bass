/*
	BASS DX8 effects test
	Copyright (c) 2001-2019 Un4seen Developments Ltd.
*/

package com.example.fxtest;

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
import android.widget.SeekBar;

import java.io.File;
import java.lang.Math;
import java.util.Arrays;

import com.un4seen.bass.BASS;

public class FXTest extends Activity {
	int floatable = BASS.BASS_SAMPLE_FLOAT;    // floating-point channel support?

	int chan;               // channel handle
	int fxchan;             // output stream handle
	int[] fx = new int[4];  // 3 eq bands + reverb

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
				new AlertDialog.Builder(FXTest.this)
						.setMessage((String) param)
						.setPositiveButton("OK", null)
						.show();
			}
		});
	}

	void UpdateFX(SeekBar sb) {
		int v = sb.getProgress();
		int n = Integer.parseInt((String) sb.getTag());
		if (n < 3) {
			BASS.BASS_DX8_PARAMEQ p = new BASS.BASS_DX8_PARAMEQ();
			BASS.BASS_FXGetParameters(fx[n], p);
			p.fGain = v - 10;
			BASS.BASS_FXSetParameters(fx[n], p);
		} else {
			BASS.BASS_DX8_REVERB p = new BASS.BASS_DX8_REVERB();
			BASS.BASS_FXGetParameters(fx[n], p);
			p.fReverbMix = (float) (v != 0 ? Math.log(v / 20.0) * 20 : -96);
			BASS.BASS_FXSetParameters(fx[n], p);
		}
	}

	void SetupFX() {
		// setup the effects
		int ch = fxchan != 0 ? fxchan : chan; // set on output stream if enabled, else file stream
		fx[0] = BASS.BASS_ChannelSetFX(ch, BASS.BASS_FX_DX8_PARAMEQ, 0);
		fx[1] = BASS.BASS_ChannelSetFX(ch, BASS.BASS_FX_DX8_PARAMEQ, 0);
		fx[2] = BASS.BASS_ChannelSetFX(ch, BASS.BASS_FX_DX8_PARAMEQ, 0);
		fx[3] = BASS.BASS_ChannelSetFX(ch, BASS.BASS_FX_DX8_REVERB, 0);
		BASS.BASS_DX8_PARAMEQ p = new BASS.BASS_DX8_PARAMEQ();
		p.fGain = 0;
		p.fBandwidth = 18;
		p.fCenter = 125;
		BASS.BASS_FXSetParameters(fx[0], p);
		p.fCenter = 1000;
		BASS.BASS_FXSetParameters(fx[1], p);
		p.fCenter = 8000;
		BASS.BASS_FXSetParameters(fx[2], p);
		UpdateFX((SeekBar) findViewById(R.id.eq1));
		UpdateFX((SeekBar) findViewById(R.id.eq2));
		UpdateFX((SeekBar) findViewById(R.id.eq3));
		UpdateFX((SeekBar) findViewById(R.id.reverb));
	}

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
							// first free the current one (try both MOD and stream - it must be one of them)
							BASS.BASS_MusicFree(chan);
							BASS.BASS_StreamFree(chan);
							if ((chan = BASS.BASS_StreamCreateFile(file, 0, 0, BASS.BASS_SAMPLE_LOOP | floatable)) == 0
									&& (chan = BASS.BASS_MusicLoad(file, 0, 0, BASS.BASS_SAMPLE_LOOP | BASS.BASS_MUSIC_RAMP | floatable, 1)) == 0) {
								// whatever it is, it ain't playable
								((Button) findViewById(R.id.open)).setText("press here to open a file");
								Error("Can't play the file");
								return;
							}
							((Button) findViewById(R.id.open)).setText(file);
							if (fxchan == 0)
								SetupFX(); // set effects on file if not using output stream
							BASS.BASS_ChannelPlay(chan, false);
						}
					}
				})
				.show();
	}

	public void OutputClicked(View v) {
		// remove current effects
		int ch = fxchan != 0 ? fxchan : chan;
		BASS.BASS_ChannelRemoveFX(ch, fx[0]);
		BASS.BASS_ChannelRemoveFX(ch, fx[1]);
		BASS.BASS_ChannelRemoveFX(ch, fx[2]);
		BASS.BASS_ChannelRemoveFX(ch, fx[3]);
		if (((CheckBox) findViewById(R.id.output)).isChecked())
			fxchan = BASS.BASS_StreamCreate(0, 0, 0, BASS.STREAMPROC_DEVICE, null); // get device output stream
		else
			fxchan = 0; // stop using device output stream
		SetupFX();
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		if (Build.VERSION.SDK_INT >= 23)
			requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);

		filepath = Environment.getExternalStorageDirectory();

		// initialize default output device
		if (!BASS.BASS_Init(-1, 44100, 0)) {
			Error("Can't initialize device");
			return;
		}

		// check for floating-point capability
		if (BASS.BASS_GetConfig(BASS.BASS_CONFIG_FLOAT) == 0) {
			// no floating-point channel support
			floatable = 0;
			// enable floating-point (8.24 fixed-point in this case) DSP/FX instead
			BASS.BASS_SetConfig(BASS.BASS_CONFIG_FLOATDSP, 1);
		}

		SeekBar.OnSeekBarChangeListener osbcl = new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				UpdateFX(seekBar);
			}
		};
		((SeekBar) findViewById(R.id.eq1)).setOnSeekBarChangeListener(osbcl);
		((SeekBar) findViewById(R.id.eq2)).setOnSeekBarChangeListener(osbcl);
		((SeekBar) findViewById(R.id.eq3)).setOnSeekBarChangeListener(osbcl);
		((SeekBar) findViewById(R.id.reverb)).setOnSeekBarChangeListener(osbcl);
	}

	@Override
	public void onDestroy() {
		BASS.BASS_Free();

		super.onDestroy();
	}
}