package com.obm.mylibrary;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 打印指令发送工具类
 */

public class PrintUnits {

    /**
     * 打印纸一行最大的字节
     */
    public static final int LINE_BYTE_SIZE = 32;//58mm

    /**
     * 初始化打印机，清除打印缓冲区数据，打印模式被设为上电时的默认值模式。
     */
    public static void setInitialize(OutputStream os) {
        try {
            byte[] d = {0x1B, 0x40};
            os.write(d, 0, d.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印并向前走纸n 行
     */
    public static void setPagerLine(OutputStream os, int n) {
        try {
            for (int i = 0; i < n; i++) {
                os.write(0x0a);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印浓度设置 1,0,2,递增，也可以与速度配合调整浓度
     */
    public static void setConcentration(OutputStream os, int con) {
        try {
            byte[] d = {0x10, (byte) 0xD2, (byte) con};
            os.write(d, 0, d.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印速度设置 打印速度 0-7 递减，打印图片时速度越慢越顺畅
     */
    public static void setSpeed(OutputStream os, int speed) {
        try {
            byte[] d = {0x10, (byte) 0xD1, (byte) speed};
            os.write(d, 0, d.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置默认行间距，如要设置换行用\r 换行符  选择约 3.75mm 行间距。
     */
    public static void setDefaultLinewidth(OutputStream os) {
        try {
            byte[] d = {0x1B, 0x32};
            os.write(d, 0, d.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 行间距设置 0-255，如要设置换行用\r 换行符
     */
    public static void setLinewidth(OutputStream os, int width) {
        try {
            byte[] d = {0x1B, 0x33, (byte) width};
            os.write(d, 0, d.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 右间距设置 0-255
     */
    public static void setRigthwidth(OutputStream os, int width) {
        try {
            byte[] d = {0x1B, 0x20, (byte) width};
            os.write(d, 0, d.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 选择对齐方式  0 ≤ n ≤ 2
     * 0左对齐，1中间对齐，2右对齐
     */
    public static void setAlignment(OutputStream os, int n) {
        try {
            byte[] d = {0x1B, 0x61, (byte) n};
            os.write(d, 0, d.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 选择/取消下划线模式  0 ≤ n ≤2
     * 0 取消下划线模式
     * 1 选择下划线模式(1 点宽)
     * 2 选择下划线模式(2 点宽)
     */
    public static void setUnderline(OutputStream os, int n) {
        try {
            byte[] d = {0x1B, 0x2D, (byte) n};
            os.write(d, 0, d.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 选择 / 取消黑白反显打印模式   0 ≤ n ≤ 255
     * 当 n 的最低位为 0 时，取消反显打印。
     * 当 n 的最低位为 1 时，选择反显打印。
     */
    public static void setReverse(OutputStream os, int n) {
        try {
            byte[] d = {0x1D, 0x42, (byte) n};
            os.write(d, 0, d.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 选择/取消顺时针旋转90 度    0 ≤ n ≤ 1
     * 0 取消顺时针旋转 90 度模式
     * 1 选择顺时针旋转 90 度模式
     */
    public static void setRotate(OutputStream os, int n) {
        try {
            byte[] d = {0x1B, 0x56, (byte) n};
            os.write(d, 0, d.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
