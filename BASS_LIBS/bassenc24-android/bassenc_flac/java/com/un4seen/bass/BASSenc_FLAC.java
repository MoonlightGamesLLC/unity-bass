/*
	BASSenc_FLAC 2.4 Java class
	Copyright (c) 2017-2018 Un4seen Developments Ltd.

	See the BASSENC_FLAC.CHM file for more detailed documentation
*/

package com.un4seen.bass;

public class BASSenc_FLAC
{
	public static native int BASS_Encode_FLAC_GetVersion();

	public static native int BASS_Encode_FLAC_Start(int handle, String options, int flags, BASSenc.ENCODEPROCEX proc, Object user);
	public static native int BASS_Encode_FLAC_StartFile(int handle, String options, int flags, String filename);

    static {
        System.loadLibrary("bassenc");
        System.loadLibrary("bassenc_flac");
    }
}
