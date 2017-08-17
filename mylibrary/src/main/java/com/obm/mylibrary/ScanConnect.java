package com.obm.mylibrary;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 扫描
 */
public class ScanConnect {
    private SerialPortModel comSerialport;
    public InputStream is;
    public OutputStream os;
    private Handler handler;
    private Open open = new Open();
    private boolean isScan = true;
    public int RECV_SCAN = 11;
    private byte[] responseData = new byte[1025];
    private Ringtone r;

    public ScanConnect(Context context, Handler handler) {
        this.handler = handler;
        open.openScan();
        Uri notification = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.wet);
        r = RingtoneManager.getRingtone(context, notification);
        test();
    }

    public void setHandler(Handler handler){
        this.handler=handler;
    }

    /**
     * 打开扫描串口
     */
    private void test() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    comSerialport = new SerialPortModel(new File("/dev/ttyHSL2"), 9600, 8, "None", 1, "None");
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                is = comSerialport.getInputStream();
                os = comSerialport.getOutputStream();
            }
        }).start();
    }

    /**
     * 扫描条码并通过handler返回数据
     */
    public void scan() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isScan) {
                    isScan = false;
                    readscanpdata();
                }
            }
        }).start();
    }

    /**
     * 播放扫描声音
     */
    private void song() {
        if (r.isPlaying()) {
            r.stop();
        }
        r.play();
    }

    /**
     * 读取扫描到的条码
     */
    private void readscanpdata() {

        String code = "";
        int readcount;
        Arrays.fill(responseData, (byte) 0);
        FlushUartBuffer();

        open.startScan();
        readcount = read();
        if (readcount > 0) {
            code = new String(responseData, 0, readcount);
        }
        if(!TextUtils.isEmpty(code)) {
            song();
            code=replaceBlank(code);
            Message msg = new Message();
            msg.what = RECV_SCAN;
            msg.obj = code;
            handler.sendMessage(msg);
        }
        isScan = true;
        open.stopScan();
    }

    private void FlushUartBuffer() {
        byte[] buffer_tmp = new byte[1024];
        try {
            is.read(buffer_tmp);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private int read() {
        int available = 0;
        int index = 0;
        try {
            while (index < 11) {
                Thread.sleep(100);
                available = is.available();
                if (available > 0)
                    break;
                index++;
            }
            if (available > 0) {
                available = is.read(responseData);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return available;
    }

    /**
     * 去除条码中的换行符
     */
    private String replaceBlank(String str) {
        String dest = "";
        if (str!=null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }

    /**
     * 退出关闭IO和串口
     */
    public void stop() {
        try {
            open.closeScan();
            if(comSerialport!=null) {
                is.close();
                os.close();
                comSerialport.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
