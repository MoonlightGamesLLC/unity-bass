/*
	BASSenc_OPUS 2.4 Java class
	Copyright (c) 2016 Un4seen Developments Ltd.

	See the BASSENC_OPUS.CHM file for more detailed documentation
*/

package com.un4seen.bass;

public class BASSenc_OPUS
{
	public static native int BASS_Encode_OPUS_GetVersion();

	public static native int BASS_Encode_OPUS_Start(int handle, String options, int flags, BASSenc.ENCODEPROC proc, Object user);
	public static native int BASS_Encode_OPUS_StartFile(int handle, String options, int flags, String filename);

    static {
        System.loadLibrary("bassenc");
        System.loadLibrary("bassenc_opus");
    }
}
