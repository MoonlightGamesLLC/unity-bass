/*
	BASSHLS 2.4 Java class
	Copyright (c) 2015-2019 Un4seen Developments Ltd.

	See the BASSHLS.CHM file for more detailed documentation
*/

package com.un4seen.bass;

import java.nio.ByteBuffer;

public class BASSHLS
{
	// additional BASS_SetConfig options
	public static final int BASS_CONFIG_HLS_DOWNLOAD_TAGS = 0x10900;
	public static final int BASS_CONFIG_HLS_BANDWIDTH = 0x10901;
	public static final int BASS_CONFIG_HLS_DELAY = 0x10902;

	// additional sync type
	public static final int BASS_SYNC_HLS_SEGMENT = 0x10300;

	// additional tag types
	public static final int BASS_TAG_HLS_EXTINF = 0x14000; // segment's EXTINF tag : String
	public static final int BASS_TAG_HLS_STREAMINF = 0x14001; // EXT-X-STREAM-INF tag : UTF-8 string
	public static final int BASS_TAG_HLS_DATE = 0x14002; // EXT-X-PROGRAM-DATE-TIME tag : UTF-8 string

	// additional BASS_StreamGetFilePosition mode
	public static final int BASS_FILEPOS_HLS_SEGMENT = 0x10000;	// segment sequence number

	public static native int BASS_HLS_StreamCreateFile(String file, long offset, long length, int flags);
	public static native int BASS_HLS_StreamCreateFile(ByteBuffer file, long offset, long length, int flags);
	public static native int BASS_HLS_StreamCreateURL(String url, int flags, BASS.DOWNLOADPROC proc, Object user);
	
    static {
        System.loadLibrary("basshls");
    }
}
