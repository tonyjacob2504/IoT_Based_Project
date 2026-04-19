package com.aumento.blindstick.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageUtil {

    public static Bitmap toBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer y = planes[0].getBuffer();
        ByteBuffer u = planes[1].getBuffer();
        ByteBuffer v = planes[2].getBuffer();

        byte[] nv21 = new byte[y.remaining() + u.remaining() + v.remaining()];
        y.get(nv21, 0, y.remaining());
        v.get(nv21, y.remaining(), v.remaining());
        u.get(nv21, y.remaining() + v.remaining(), u.remaining());

        YuvImage yuvImage = new YuvImage(
                nv21, ImageFormat.NV21,
                image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(
                new Rect(0, 0, image.getWidth(), image.getHeight()), 90, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public static ByteBuffer bitmapToBuffer(Bitmap bitmap) {

        ByteBuffer buffer =
                ByteBuffer.allocateDirect(300 * 300 * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[300 * 300];
        bitmap.getPixels(pixels, 0, 300, 0, 0, 300, 300);

        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
            buffer.put((byte) (pixel & 0xFF));         // B
        }

        buffer.rewind();
        return buffer;
    }
}