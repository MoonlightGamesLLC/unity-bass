# unity-bass
# A BASS.NET library made with Unity. https://www.un4seen.com/

I made this repository to help document the setup of bass in unity. With your help we can make it capable of running on Windows, macOS, Linux, iOS, and Android in Unity.

If you don't have Unity. Download Unity Hub now! https://unity3d.com/get-unity/download

Install Unity 2019.4.4 or later.
  Add the modules for Android and iOS

Open Unity Hub and add the folder and open the project.

Build to iOS and Android by going to File -> Build Settings

  Select your desired platform i.e. iOS.
  
  Click "Switch Platform"
  
  Click Build
  
    special script runs on iOS to disable Bitcode since BASS does not support bitcode.
    
Application in Unity itself.

  Open the scene BassTest
  
  The app starts and tries to initailize BASS. The status or an exception will be displayed. Goal is for it to say success.

    The bass libraries might not all be fully updated to current versions.
