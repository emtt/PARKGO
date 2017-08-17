package com.obm.mylibrary;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 打印
 */
public class PrintConnect {
    private SerialPortModel comSerialport;
    public InputStream is;
    public OutputStream os;
    private Open open = new Open();
    private Context context;

    public PrintConnect(Context context) {
        this.context = context;
        open.openPrint();
        test();
    }

    /**
     * 打开打印头串口
     */
    private void test() {
        new Thread() {
            @Override
            public void run() {
                try {
                    comSerialport = new SerialPortModel(new File("/dev/ttyHSL1"), 115200, 8, "None", 1, "None");
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                is = comSerialport.getInputStream();
                os = comSerialport.getOutputStream();
                try {
                    byte[] a={0x1B,0x40,0x1B,0x52,0x00};
                    os.write(a,0,a.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 退出关闭IO和串口
     */
    public void stop() {
        try {
            if (comSerialport != null) {
                is.close();
                os.close();
                comSerialport.close();
            }
            open.closePrint();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送打印数据，中文为gbk编码
     * @param sendData
     */
    public void send(String sendData) {
        try {
            byte[] data = sendData.getBytes("gbk");
            os.write(data, 0, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印图片，最佳是360*360，打印前请把速度调低，速度为7时最清晰顺畅
     * @param bit
     */
    public void send(Bitmap bit) {
        PicFromPrintUtils.writeBit(bit, os);
    }

    /**
     * 发送打印数据，中文为gbk编码
     * @param data
     */
    public void send(byte[] data) {
        try {
            os.write(data, 0, data.length);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 打印条码，由于纸长度的原因，宽度为2时最多打印14位
     * @param data
     */
    public void sendCode128(String data,int width,int heigth){ //打印条码
        try {
            byte[] n = new byte[]{29, 107, 73};//72为code93，73位code128
            byte[] m = data.getBytes();
            byte[] code = {123, 66};
            byte[] o = new byte[]{29, 104, (byte) heigth};//设置条码高度 0-255
            byte[] p = new byte[]{29, 119, (byte) width};//设置条码宽度 1-2,宽度2只打印14位
            os.write(o, 0, o.length);
            os.write(p, 0, p.length);
            os.write(n, 0, n.length);
            os.write(m.length + code.length + 1);
            os.write(code);
            os.write(m);
            os.write(0x0a);
            os.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
