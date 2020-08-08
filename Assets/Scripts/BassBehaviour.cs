using System;
using System.Collections;
using System.Collections.Generic;
using Un4seen.Bass;
using UnityEngine;
using UnityEngine.UI;

public class BassBehaviour : MonoBehaviour
{
    public Text status;

    private void Start()
    {
        status.text = "Initializing";

		EncoderLoad();
	}

	private void EncoderLoad()
	{
		try
        {
			BassNet.Registration("zippo227@gmail.com", "test");

			bool isInitialized = Bass.BASS_Init(-1, 44100, BASSInit.BASS_DEVICE_DEFAULT, IntPtr.Zero);
			if (!isInitialized)
			{
				status.text = "Bass_Init error!";
			}
			else
			{
				status.text = "Bass_Init success!";
			}

			// init your recording device (we use the default device)
			//if (!Bass.BASS_RecordInit(-1))
			//	status.text = "Bass_RecordInit error!);
		}
		catch(Exception ex)
        {
			Debug.LogError(ex.Message);
			status.text = "Bass_Init error!";
		}
	}

}
