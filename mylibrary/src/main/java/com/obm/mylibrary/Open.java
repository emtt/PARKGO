package com.obm.mylibrary;

import java.util.Timer;
import java.util.TimerTask;


public class Open {
    private static final int LEVEL_HIGH = 1;
    private static final int LEVEL_LOW = 0;

    public void openPrint() {
        nativeSetGPIO(88, LEVEL_HIGH);
    }

    public void closePrint() {
        nativeSetGPIO(88, LEVEL_LOW);
    }

    //开启扫描头
    public void openScan() {
        nativeSetGPIO(9, LEVEL_HIGH);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
                           public void run() {
                               nativeSetGPIO(8, LEVEL_LOW);
                           }
                       },
                150);
    }

    //关闭扫描头
    public void closeScan() {
        nativeSetGPIO(9, LEVEL_LOW);
    }

    //开启扫描
    public void startScan() {
        nativeSetGPIO(8, LEVEL_HIGH);
    }

    //停止扫描
    public void stopScan() {
        nativeSetGPIO(8, LEVEL_LOW);
    }


    public native int nativeSetGPIO(int gpio, int level);

    static {
        System.loadLibrary("setGPIO");
    }
}
