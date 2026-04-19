package com.aumento.blindstick.Utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public final class TFLiteUtil {

    private TFLiteUtil() {}

    public static Interpreter loadModel(Context context, String modelName) {
        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);

            // Optional GPU acceleration
           /* try {
                options.addDelegate(new GpuDelegate());
            } catch (Exception ignored) {}*/

            return new Interpreter(loadModelFile(context, modelName), options);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load TFLite model", e);
        }
    }

    private static MappedByteBuffer loadModelFile(
            Context context, String modelName) throws IOException {

        AssetFileDescriptor fileDescriptor =
                context.getAssets().openFd(modelName);

        FileInputStream inputStream =
                new FileInputStream(fileDescriptor.getFileDescriptor());

        FileChannel fileChannel = inputStream.getChannel();

        return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.getStartOffset(),
                fileDescriptor.getDeclaredLength()
        );
    }
}