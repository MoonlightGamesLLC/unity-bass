using UnityEngine;
using System.Collections;
using UnityEditor;
using UnityEditor.Build;
using UnityEditor.Build.Reporting;

public class BuildAutomation : MonoBehaviour, IPostprocessBuildWithReport
{
    public int callbackOrder => 1;

    //POSTPROCESSOR
    public void OnPostprocessBuild(BuildReport report)
    {
        string path = report.summary.outputPath;
        Debug.Log($"path: {path}");

#if UNITY_IPHONE
        //TODO: BASS.NET will need to enable BITCODE
        //within their ios libs
        string projectPath = path + "/Unity-iPhone.xcodeproj/project.pbxproj";
        var pbxProject = new UnityEditor.iOS.Xcode.PBXProject();
        pbxProject.ReadFromFile(projectPath);
        //The unity project
        string target = pbxProject.GetUnityMainTargetGuid();
        pbxProject.SetBuildProperty(target, "ENABLE_BITCODE", "NO");

        //the unity framework
        string targetFramework = pbxProject.GetUnityFrameworkTargetGuid();
        pbxProject.SetBuildProperty(targetFramework, "ENABLE_BITCODE", "NO");

        pbxProject.WriteToFile(projectPath);
#endif
    }
}
