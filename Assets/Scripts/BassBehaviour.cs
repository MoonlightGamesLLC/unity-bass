using System;
using System.Collections;
using System.Collections.Generic;
using Un4seen.Bass;
using UnityEngine;
using UnityEngine.UI;

public class BassBehaviour : MonoBehaviour
{
    public Text status;

	private bool isInitialized;

	private string statusUpdateString;

	private void Start()
    {
        status.text = "Initializing";

		EncoderLoad();
	}

    private void OnDestroy()
    {
        if(isInitialized)
        {
			Bass.BASS_Free();
        }
    }

    private void Update()
    {
        if(!string.IsNullOrEmpty(statusUpdateString))
        {
			status.text = statusUpdateString;
			statusUpdateString = null;
        }
    }

    private void EncoderLoad()
	{
		try
        {
			BassNet.Registration("zippo227@gmail.com", "test");

			isInitialized = Bass.BASS_Init(-1, 44100, BASSInit.BASS_DEVICE_DEFAULT, IntPtr.Zero);
			if (!isInitialized)
			{
				statusUpdateString = "Bass_Init error!";
			}
			else
			{
				statusUpdateString = "Bass_Init success!";
			}

			// init your recording device (we use the default device)
			//if (!Bass.BASS_RecordInit(-1))
            //{
			//	statusUpdateString = "Bass_RecordInit error!";
			//}
		}
		catch (Exception ex)
        {
			statusUpdateString = $"Bass_Init error! {ex.Message}";
		}
	}

}
