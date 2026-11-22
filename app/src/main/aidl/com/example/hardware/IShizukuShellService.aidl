package com.example.hardware;

interface IShizukuShellService {
    int enableGlobalFreeformSettings();
    String resolveLaunchComponent(String packageName);
    int launchWindowMode5(String packageName, String activityName);
    void destroy();
}
