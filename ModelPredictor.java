package com.example.tradingpredictor;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class ModelPredictor {

    private Interpreter tflite;

    public ModelPredictor(Context context) {
        try {
            MappedByteBuffer model = loadModelFile(context);
            tflite = new Interpreter(model);
        } catch (Exception e) {
            Log.e("ModelPredictor", "Error loading model: " + e.getMessage());
        }
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        FileInputStream inputStream = context.getAssets().openFd("model.tflite").createInputStream();
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = context.getAssets().openFd("model.tflite").getStartOffset();
        long declaredLength = context.getAssets().openFd("model.tflite").getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float[] predictNextCandle(float[] ohlc) {
        float[][] input = new float[1][4];  // open, high, low, close
        input[0] = ohlc;

        float[][] output = new float[1][1];  // e.g., trend = 1 or 0, or predicted close

        if (tflite != null) {
            tflite.run(input, output);
            return output[0];
        } else {
            return new float[]{-1};
        }
    }
}
