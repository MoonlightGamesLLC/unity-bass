/*
	BASSMIDI test player
	Copyright (c) 2006-2019 Un4seen Developments Ltd.
*/

package com.example.miditest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;

import com.un4seen.bass.BASS;
import com.un4seen.bass.BASSMIDI;

public class MidiTest extends Activity {
	int chan;           // channel handle
	int font;           // soundfont
	float speed = 1;    // tempo adjustment
	int updatefont;
	String lyrics;      // lyrics buffer

	Handler handler = new Handler();
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
				new AlertDialog.Builder(MidiTest.this)
						.setMessage((String) param)
						.setPositiveButton("OK", null)
						.show();
			}
		});
	}

	BASS.SYNCPROC LyricSync = new BASS.SYNCPROC() {
		public void SYNCPROC(int handle, int channel, int data, Object user) {
			BASSMIDI.BASS_MIDI_MARK mark = new BASSMIDI.BASS_MIDI_MARK();
			BASSMIDI.BASS_MIDI_StreamGetMark(channel, (Integer) user, data, mark); // get the lyric/text
			if (mark.text.charAt(0) == '@') return; // skip info
			if (mark.text.charAt(0) == '\\') // clear display
				lyrics += "\n\n" + mark.text.substring(1);
			else if (mark.text.charAt(0) == '/') // new line
				lyrics += "\n" + mark.text.substring(1);
			else
				lyrics += mark.text;
			int lines, nlpos = 0;
			for (lines = 1; (nlpos = lyrics.indexOf('\n', nlpos)) >= 0; lines++, nlpos++)
				; // count lines
			if (lines > 3) { // remove old lines so that new lines fit in display...
				nlpos = 0;
				do {
					nlpos = lyrics.indexOf('\n', nlpos) + 1;
				} while (--lines > 3);
				lyrics = lyrics.substring(nlpos);
			}
			runOnUiThread(new Runnable() {
				public void run() {
					((TextView) findViewById(R.id.lyrics)).setText(lyrics);
				}
			});
		}
	};

	BASS.SYNCPROC EndSync = new BASS.SYNCPROC() {
		public void SYNCPROC(int handle, int channel, int data, Object user) {
			lyrics = ""; // clear lyrics
			runOnUiThread(new Runnable() {
				public void run() {
					((TextView) findViewById(R.id.lyrics)).setText("");
				}
			});
		}
	};

	// look for a marker (eg. loop points)
	boolean FindMarker(int handle, String text, BASSMIDI.BASS_MIDI_MARK mark) {
		for (int a = 0; BASSMIDI.BASS_MIDI_StreamGetMark(handle, BASSMIDI.BASS_MIDI_MARK_MARKER, a, mark); a++) {
			if (mark.text.equals(text)) return true; // found it
		}
		return false;
	}

	BASS.SYNCPROC LoopSync = new BASS.SYNCPROC() {
		public void SYNCPROC(int handle, int channel, int data, Object user) {
			BASSMIDI.BASS_MIDI_MARK mark = new BASSMIDI.BASS_MIDI_MARK();
			if (FindMarker(channel, "loopstart", mark)) // found a loop start point
				BASS.BASS_ChannelSetPosition(channel, mark.pos, BASS.BASS_POS_BYTE | BASSMIDI.BASS_MIDI_DECAYSEEK); // rewind to it (and let old notes decay)
			else
				BASS.BASS_ChannelSetPosition(channel, 0, BASS.BASS_POS_BYTE | BASSMIDI.BASS_MIDI_DECAYSEEK); // else rewind to the beginning instead
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
							BASS.BASS_StreamFree(chan); // free old stream before opening new
							((TextView) findViewById(R.id.lyrics)).setText(""); // clear lyrics display
							if ((chan = BASSMIDI.BASS_MIDI_StreamCreateFile(file, 0, 0, BASS.BASS_SAMPLE_LOOP | (((CheckBox) findViewById(R.id.effects)).isChecked() ? 0 : BASSMIDI.BASS_MIDI_NOFX), 1)) == 0) {
								// it ain't a MIDI
								((Button) findViewById(R.id.open)).setText("press here to open a file");
								((TextView) findViewById(R.id.title)).setText("");
								((SeekBar) findViewById(R.id.position)).setMax(0);
								Error("Can't play the file");
								return;
							}
							((Button) findViewById(R.id.open)).setText(file);
							// set the title (track name of first track)
							BASSMIDI.BASS_MIDI_MARK mark = new BASSMIDI.BASS_MIDI_MARK();
							if (BASSMIDI.BASS_MIDI_StreamGetMark(chan, BASSMIDI.BASS_MIDI_MARK_TRACK, 0, mark) && mark.track == 0)
								((TextView) findViewById(R.id.title)).setText(mark.text);
							else
								((TextView) findViewById(R.id.title)).setText("");
							// update pos scroller range (using tick length)
							((SeekBar) findViewById(R.id.position)).setMax((int) BASS.BASS_ChannelGetLength(chan, BASSMIDI.BASS_POS_MIDI_TICK) / 120);
							// set looping syncs
							if (FindMarker(chan, "loopend", mark)) // found a loop end point
								BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_POS | BASS.BASS_SYNC_MIXTIME, mark.pos, LoopSync, 0); // set a sync there
							BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_END | BASS.BASS_SYNC_MIXTIME, 0, LoopSync, 0); // set one at the end too (eg. in case of seeking past the loop point)
							// clear lyrics buffer and set lyrics syncs
							lyrics = "";
							if (BASSMIDI.BASS_MIDI_StreamGetMark(chan, BASSMIDI.BASS_MIDI_MARK_LYRIC, 0, mark)) // got lyrics
								BASS.BASS_ChannelSetSync(chan, BASSMIDI.BASS_SYNC_MIDI_MARK, BASSMIDI.BASS_MIDI_MARK_LYRIC, LyricSync, (Integer) BASSMIDI.BASS_MIDI_MARK_LYRIC);
							else if (BASSMIDI.BASS_MIDI_StreamGetMark(chan, BASSMIDI.BASS_MIDI_MARK_TEXT, 20, mark)) // got text instead (over 20 of them)
								BASS.BASS_ChannelSetSync(chan, BASSMIDI.BASS_SYNC_MIDI_MARK, BASSMIDI.BASS_MIDI_MARK_TEXT, LyricSync, (Integer) BASSMIDI.BASS_MIDI_MARK_TEXT);
							BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_END, 0, EndSync, 0);
							BASSMIDI.BASS_MIDI_StreamEvent(chan, 0, BASSMIDI.MIDI_EVENT_SPEED, (int) (speed * 10000)); // apply tempo adjustment
							// get default soundfont in case of matching soundfont being used
							BASSMIDI.BASS_MIDI_FONT[] sf = new BASSMIDI.BASS_MIDI_FONT[1];
							if (BASSMIDI.BASS_MIDI_StreamGetFonts(chan, sf, 1) > 0)
								font = sf[0].font;
							// limit CPU usage to 70% (also enables async sample loading) and start playing
							BASS.BASS_ChannelSetAttribute(chan, BASSMIDI.BASS_ATTRIB_MIDI_CPU, 70);
							BASS.BASS_ChannelPlay(chan, false);
						}
					}
				})
				.show();
	}

	public void OpenFontClicked(View v) {
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
				.setTitle("Choose a soundfont to use")
				.setItems(filelist, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						File sel;
						if (filelist[which].equals("..")) sel = filepath.getParentFile();
						else sel = new File(filepath, filelist[which]);
						if (sel.isDirectory()) {
							filepath = sel;
							OpenFontClicked(null);
						} else {
							String file = sel.getPath();
							int newfont = BASSMIDI.BASS_MIDI_FontInit(file, 0);
							if (newfont == 0) {
								Error("Can't load the file");
							} else {
								BASSMIDI.BASS_MIDI_FONT[] sf = {new BASSMIDI.BASS_MIDI_FONT()};
								sf[0].font = newfont;
								sf[0].preset = -1; // use all presets
								sf[0].bank = 0; // use default bank(s)
								BASSMIDI.BASS_MIDI_StreamSetFonts(0, sf, 1); // set default soundfont
								BASSMIDI.BASS_MIDI_StreamSetFonts(chan, sf, 1); // set for current stream too
								BASSMIDI.BASS_MIDI_FontFree(font); // free old soundfont
								font = newfont;
							}
						}
					}
				})
				.show();
	}

	public void EffectsClicked(View v) {
		if (((CheckBox) v).isChecked())
			BASS.BASS_ChannelFlags(chan, 0, BASSMIDI.BASS_MIDI_NOFX); // enable FX
		else
			BASS.BASS_ChannelFlags(chan, BASSMIDI.BASS_MIDI_NOFX, BASSMIDI.BASS_MIDI_NOFX); // disable FX
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

		// load optional plugins for packed soundfonts
		String libpath = getApplicationInfo().nativeLibraryDir;
		BASS.BASS_PluginLoad(libpath + "/libbassflac.so", 0);
		BASS.BASS_PluginLoad(libpath + "/libbasswv.so", 0);

		((SeekBar) findViewById(R.id.position)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					BASS.BASS_ChannelSetPosition(chan, progress * 120, BASSMIDI.BASS_POS_MIDI_TICK); // set the position
					// clear lyrics
					lyrics = "";
					((TextView) findViewById(R.id.lyrics)).setText("");
				}
			}
		});

		((SeekBar) findViewById(R.id.tempo)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					speed = (10 + progress) / 20.f; // up to +/- 50% bpm
					BASSMIDI.BASS_MIDI_StreamEvent(chan, 0, BASSMIDI.MIDI_EVENT_SPEED, (int) (speed * 10000)); // apply tempo adjustment
				}
			}
		});

		// timer to update the display
		Runnable timer = new Runnable() {
			public void run() {
				if (chan != 0) {
					int tick = (int) BASS.BASS_ChannelGetPosition(chan, BASSMIDI.BASS_POS_MIDI_TICK); // get position in ticks
					int tempo = BASSMIDI.BASS_MIDI_StreamGetEvent(chan, 0, BASSMIDI.MIDI_EVENT_TEMPO); // get the file's tempo
					((SeekBar) findViewById(R.id.position)).setProgress(tick / 120); // update position
					((TextView) findViewById(R.id.bpm)).setText(String.format("%.1f", speed * 60000000.0 / tempo)); // update bpm
				}
				if ((++updatefont & 1) == 1) { // only updating font info once a second
					String text = "no soundfont";
					BASSMIDI.BASS_MIDI_FONTINFO i = new BASSMIDI.BASS_MIDI_FONTINFO();
					if (BASSMIDI.BASS_MIDI_FontGetInfo(font, i))
						text = String.format("name: %s\nloaded: %d / %d", i.name, i.samload, i.samsize);
					((TextView) findViewById(R.id.fontinfo)).setText(text);
				}
				handler.postDelayed(this, 500);
			}
		};
		handler.postDelayed(timer, 500);
	}

	@Override
	public void onDestroy() {
		// free the output device and all plugins and the soundfont
		BASS.BASS_Free();
		BASS.BASS_PluginFree(0);
		BASSMIDI.BASS_MIDI_FontFree(font);

		super.onDestroy();
	}
}