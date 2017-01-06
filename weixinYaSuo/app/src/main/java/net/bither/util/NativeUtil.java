/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class NativeUtil {
	 private static int DEFAULT_QUALITY = 95;

	    /**
	     * @param bit      bitmap对象
	     * @param fileName 指定保存目录名
	     * @param optimize 是否采用哈弗曼表数据计算 品质相差5-10倍
	     * @Description: JNI基本压缩
	     */
	    public static void compressBitmap(Bitmap bit, String fileName, boolean optimize) {
	        saveBitmap(bit, DEFAULT_QUALITY, fileName, optimize);
	    }
	    /**
	     * @param image    bitmap对象
	     * @param filePath 要保存的指定目录
	     * @Description: 通过JNI图片压缩把Bitmap保存到指定目录
	     */
	    public static void compressBitmap(Bitmap image, String filePath) {
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        // 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
	        int options = 20;
	        // JNI调用保存图片到SD卡 这个关键
	        NativeUtil.saveBitmap(image, options, filePath, true);
	    }

	    /**
	     * 计算缩放比
	     *
	     * @param bitWidth  当前图片宽度
	     * @param bitHeight 当前图片高度
	     * @return
	     * @Description:函数描述
	     */
	    public static int getRatioSize(int bitWidth, int bitHeight) {
	        // 图片最大分辨率
	        int imageHeight = 1920;
	        int imageWidth = 1080;
	        // 缩放比
	        int ratio = 1;
	        // 缩放比,由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
	        if (bitWidth > bitHeight && bitWidth > imageWidth) {
	            // 如果图片宽度比高度大,以宽度为基准
	            ratio = bitWidth / imageHeight;
	        } else if (bitWidth < bitHeight && bitHeight > imageHeight) {
	            // 如果图片高度比宽度大，以高度为基准
	            ratio = bitHeight / imageHeight;
	        }
	        // 最小比率为1
	        if (ratio <= 0)
	            ratio = 1;
	        return ratio;
	    }

	    /**
	     * 调用native方法
	     *
	     * @param bit
	     * @param quality
	     * @param fileName
	     * @param optimize
	     * @Description:函数描述
	     */
	    public static void saveBitmap(Bitmap bit, int quality, String fileName, boolean optimize) {
	        compressBitmap(bit, bit.getWidth(), bit.getHeight(), quality, fileName.getBytes(), optimize);
	    }

	    /**
	     * 调用底层 bitherlibjni.c中的方法
	     *
	     * @param bit
	     * @param w
	     * @param h
	     * @param quality
	     * @param fileNameBytes
	     * @param optimize
	     * @return
	     * @Description:函数描述
	     */
	    public static native String compressBitmap(Bitmap bit, int w, int h, int quality, byte[] fileNameBytes,
	                                                boolean optimize);

	    /**
	     * 加载lib下两个so文件
	     */
	    static {
	        System.loadLibrary("jpegbither");
	        System.loadLibrary("bitherjni");
	    }


	    /**
	     * 1. 质量压缩
			     设置bitmap options属性，降低图片的质量，像素不会减少
			     第一个参数为需要压缩的bitmap图片对象，第二个参数为压缩后图片保存的位置
			     设置options 属性0-100，来实现压缩
	     * @param bmp
	     * @param file
	     */
	    public static void compressImageToFile(Bitmap bmp,File file) {
	        // 0-100 100为不压缩
	        int options = 20;
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        // 把压缩后的数据存放到baos中
	        bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
	        try {
	            FileOutputStream fos = new FileOutputStream(file);
	            fos.write(baos.toByteArray());
	            fos.flush();
	            fos.close();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

	    /**
	     *
	     * 2. 尺寸压缩
	     通过缩放图片像素来减少图片占用内存大小
	     * @param bmp
	     * @param file
	     */

	    public static void compressBitmapToFile(Bitmap bmp, File file){
	        // 尺寸压缩倍数,值越大，图片尺寸越小
	        int ratio = 4;
	        // 压缩Bitmap到对应尺寸
	        Bitmap result = Bitmap.createBitmap(bmp.getWidth() / ratio, bmp.getHeight() / ratio, Config.ARGB_8888);
	        Canvas canvas = new Canvas(result);
	        Rect rect = new Rect(0, 0, bmp.getWidth() / ratio, bmp.getHeight() / ratio);
	        canvas.drawBitmap(bmp, null, rect, null);

	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        // 把压缩后的数据存放到baos中
	        result.compress(Bitmap.CompressFormat.JPEG, 100 ,baos);
	        try {
	            FileOutputStream fos = new FileOutputStream(file);
	            fos.write(baos.toByteArray());
	            fos.flush();
	            fos.close();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }


	    /**
	     * 设置图片的采样率，降低图片像素
	     * @param filePath
	     * @param file
	     */
	    public static void compressBitmap1(InputStream filePath, File file){
	        // 数值越高，图片像素越低
	        int inSampleSize = 4;
	        BitmapFactory.Options options = new BitmapFactory.Options();
//	        options.inJustDecodeBounds = false;
	        options.inJustDecodeBounds = true;//为true的时候不会真正加载图片，而是得到图片的宽高信息。
	        //采样率
	        options.inSampleSize = inSampleSize;
	        Bitmap bitmap = BitmapFactory.decodeStream(filePath,null ,options);

	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        // 把压缩后的数据存放到baos中
	        bitmap.compress(Bitmap.CompressFormat.JPEG, 100 ,baos);
	        try {
	            if(file.exists())
	            {
	                file.delete();
	            }
	            else {
	                file.createNewFile();
	            }
	            FileOutputStream fos = new FileOutputStream(file);
	            fos.write(baos.toByteArray());
	            fos.flush();
	            fos.close();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }


}
