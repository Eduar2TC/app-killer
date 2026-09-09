package com.appcontrol.shizuku;

interface IAppControlService {
    void destroy() = 16777114;
    int forceStop(in String packageName) = 1;
    int killBackground(in String packageName) = 2;
    String ping() = 3;
}