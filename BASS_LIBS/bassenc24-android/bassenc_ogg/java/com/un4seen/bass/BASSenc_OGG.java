/*
	BASSenc_OGG 2.4 Java class
	Copyright (c) 2016 Un4seen Developments Ltd.

	See the BASSENC_OGG.CHM file for more detailed documentation
*/

package com.un4seen.bass;

public class BASSenc_OGG
{
	public static native int BASS_Encode_OGG_GetVersion();

	public static native int BASS_Encode_OGG_Start(int handle, String options, int flags, BASSenc.ENCODEPROC proc, Object user);
	public static native int BASS_Encode_OGG_StartFile(int handle, String options, int flags, String filename);

    static {
        System.loadLibrary("bassenc");
        System.loadLibrary("bassenc_ogg");
    }
}
