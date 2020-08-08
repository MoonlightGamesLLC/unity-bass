/*
	BASSWEBM 2.4 Java class
	Copyright (c) 2018-2019 Un4seen Developments Ltd.

	See the BASSWEBM.CHM file for more detailed documentation
*/

package com.un4seen.bass;

import java.nio.ByteBuffer;

public class BASSWEBM
{
	// Additional error codes returned by BASS_ErrorGetCode
	public static final int BASS_ERROR_WEBM_TRACK = 8000;

	// Additional tag types
	public static final int BASS_TAG_WEBM = 0x15000; // file tags : String array
	public static final int BASS_TAG_WEBM_TRACK = 0x15001; // track tags : String array

	// Additional attributes
	public static final int BASS_ATTRIB_WEBM_TRACK = 0x16000;
	public static final int BASS_ATTRIB_WEBM_TRACKS = 0x16001;

	public static native int BASS_WEBM_StreamCreateFile(String file, long offset, long length, int flags, int track);
	public static native int BASS_WEBM_StreamCreateFile(ByteBuffer file, long offset, long length, int flags, int track);
	public static native int BASS_WEBM_StreamCreateURL(String url, int offset, int flags, BASS.DOWNLOADPROC proc, Object user, int track);
	public static native int BASS_WEBM_StreamCreateFileUser(int system, int flags, BASS.BASS_FILEPROCS procs, Object user, int track);

	static {
		System.loadLibrary("basswebm");
	}
}
