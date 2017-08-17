package com.obm.mylibrary;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import java.io.OutputStream;

/**
 * 图片打印工具类
 *
 * @author nsz
 *         2015年1月30日
 */
public class PicFromPrintUtils {


    public void init() {
//		Gray = 0.29900 * R + 0.58700 * G + 0.11400 * B
    }

    /*************************************************************************
     * 360*360的图片，8个字节（8个像素点）是一个二进制，将二进制转化为十进制数值
     * y轴：24个像素点为一组，即360就是15组（0-14）
     * x轴：360个像素点（0-359）
     * 里面的每一组（24*360），每8个像素点为一个二进制，（每组有3个，3*8=24）
     **************************************************************************/
    /**
     * 把一张Bitmap图片转化为打印机可以打印的bit(将图片压缩为360*360)
     */
    public static void writeBit(final Bitmap bit, final OutputStream os) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int size = bit.getHeight() / 24;
                    for (int j = 0; j < size; j++) {
                        int k = 0;
                        byte[] data = new byte[1086];
                        data[k++] = 0x1B;
                        data[k++] = 0x2A;
                        data[k++] = 33; // m=33时，选择24点双密度打印，分辨率达到200DPI。
                        data[k++] = 0x68;
                        data[k++] = 0x01;
                        for (int i = 0; i < bit.getWidth(); i++) {
                            for (int m = 0; m < 3; m++) {
                                for (int n = 0; n < 8; n++) {
                                    byte b = px2Byte(i, j * 24 + m * 8 + n, bit);
                                    data[k] += data[k] + b;
                                }
                                k++;
                            }
                        }
                        data[k++] = 0;
                        os.write(data);
                        Thread.sleep(50);
                    }
                    os.write(0x0a);
                    os.flush();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 图片二值化，黑色是1，白色是0
     *
     * @param x   横坐标
     * @param y   纵坐标
     * @param bit 位图
     * @return
     */
    public static byte px2Byte(int x, int y, Bitmap bit) {
        byte b;
        int pixel = bit.getPixel(x, y);
        int red = (pixel & 0x00ff0000) >> 16; // 取高两位
        int green = (pixel & 0x0000ff00) >> 8; // 取中两位
        int blue = pixel & 0x000000ff; // 取低两位
        int gray = RGB2Gray(red, green, blue);
        if (gray < 128) {
            b = 1;
        } else {
            b = 0;
        }
        return b;
    }

    /**
     * 图片灰度的转化
     *
     * @param r
     * @param g
     * @param b
     * @return
     */
    private static int RGB2Gray(int r, int g, int b) {
        int gray = (int) (0.29900 * r + 0.58700 * g + 0.11400 * b);  //灰度转化公式
        return gray;
    }

    /**
     * 对图片进行压缩（去除透明度）360*360
     *
     * @param bitmapOrg
     */
    public static Bitmap compressPic(Bitmap bitmapOrg) {
        // 获取这个图片的宽和高
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        // 定义预转换成的图片的宽度和高度
        int newWidth = 360;
        int newHeight = 360;
        Bitmap targetBmp = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Canvas targetCanvas = new Canvas(targetBmp);
        targetCanvas.drawColor(0xffffffff);
        targetCanvas.drawBitmap(bitmapOrg, new Rect(0, 0, width, height), new Rect(0, 0, newWidth, newHeight), null);
        return targetBmp;
    }

    /**
     * 对图片进行压缩（去除透明度）360*120,一维码专用
     *
     * @param bitmapOrg
     */
    public static Bitmap compressonePic(Bitmap bitmapOrg) {
        // 获取这个图片的宽和高
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        // 定义预转换成的图片的宽度和高度
        int newWidth = 360;
        int newHeight = 120;
        Bitmap targetBmp = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Canvas targetCanvas = new Canvas(targetBmp);
        targetCanvas.drawColor(0xffffffff);
        targetCanvas.drawBitmap(bitmapOrg, new Rect(0, 0, width, height), new Rect(0, 0, newWidth, newHeight), null);
        return targetBmp;
    }


    /**
     * 得到位图的某个点的像素值
     *
     * @param bitmap
     * @return
     */
    public static byte[] getPicPx(Bitmap bitmap) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];// 保存所有的像素的数组，图片宽×高
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < pixels.length; i++) {
            int clr = pixels[i];
            int red = (clr & 0x00ff0000) >> 16; // 取高两位
            int green = (clr & 0x0000ff00) >> 8; // 取中两位
            int blue = clr & 0x000000ff; // 取低两位
            System.out.println("r=" + red + ",g=" + green + ",b=" + blue);
        }
        return null;
    }


}
