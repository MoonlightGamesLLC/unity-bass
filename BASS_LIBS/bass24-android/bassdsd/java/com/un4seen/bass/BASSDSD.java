/*
	BASSDSD 2.4 Java class
	Copyright (c) 2014-2017 Un4seen Developments Ltd.

	See the BASSDSD.CHM file for more detailed documentation
*/

package com.un4seen.bass;

import java.nio.ByteBuffer;

public class BASSDSD
{
	// Additional BASS_SetConfig options
	public static final int BASS_CONFIG_DSD_FREQ = 0x10800;
	public static final int BASS_CONFIG_DSD_GAIN = 0x10801;

	// Additional BASS_DSD_StreamCreateFile/etc flags
	public static final int BASS_DSD_RAW = 0x200;
	public static final int BASS_DSD_DOP = 0x400;
	public static final int BASS_DSD_DOP_AA = 0x800;

	// BASS_CHANNELINFO type
	public static final int BASS_CTYPE_STREAM_DSD = 0x11700;

	// Additional tag types
	public static final int BASS_TAG_DSD_ARTIST = 0x13000; // DSDIFF artist : String
	public static final int BASS_TAG_DSD_TITLE = 0x13001; // DSDIFF title : String
	public static final int BASS_TAG_DSD_COMMENT = 0x13100; // + index, DSDIFF comment : TAG_DSD_COMMENT

	public static class TAG_DSD_COMMENT {
		short timeStampYear;	// creation year
		byte TimeStampMonth;	// creation month
		byte timeStampDay;		// creation day
		byte timeStampHour;		// creation hour
		byte timeStampMinutes;	// creation minutes
		short cmtType;			// comment type
		short cmtRef;			// comment reference
		String commentText;		// text
	}

	// Additional attributes
	public static final int BASS_ATTRIB_DSD_GAIN = 0x14000;
	public static final int BASS_ATTRIB_DSD_RATE = 0x14001;

	public static native int BASS_DSD_StreamCreateFile(String file, long offset, long length, int flags, int freq);
	public static native int BASS_DSD_StreamCreateFile(ByteBuffer file, long offset, long length, int flags, int freq);
	public static native int BASS_DSD_StreamCreateURL(String url, int offset, int flags, BASS.DOWNLOADPROC proc, Object user, int freq);
	public static native int BASS_DSD_StreamCreateFileUser(int system, int flags, BASS.BASS_FILEPROCS procs, Object user, int freq);
	
    static {
        System.loadLibrary("bassdsd");
    }
}
