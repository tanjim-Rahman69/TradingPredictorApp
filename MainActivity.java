package com.example.tradingpredictor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_SCREENSHOT = 1001;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private ImageReader imageReader;

    private int width, height, density;

    private Button startButton;
    private TextView predictionText;

    private ModelPredictor modelPredictor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startButton);
        predictionText = findViewById(R.id.predictionText);

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        modelPredictor = new ModelPredictor(this);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;

        startButton.setOnClickListener(v -> {
            Intent intent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_SCREENSHOT);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SCREENSHOT && resultCode == Activity.RESULT_OK && data != null) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);

            imageReader = ImageReader.newInstance(width, height, ImageFormat.RGB_565, 2);
            Surface surface = imageReader.getSurface();

            mediaProjection.createVirtualDisplay("ScreenCapture",
                    width, height, density,
                    0, surface, null, null);

            imageReader.setOnImageAvailableListener(reader -> {
                try (Image image = reader.acquireLatestImage()) {
                    if (image != null) {
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * width;

                        Bitmap bitmap = Bitmap.createBitmap(
                                width + rowPadding / pixelStride,
                                height, Bitmap.Config.RGB_565);
                        bitmap.copyPixelsFromBuffer(buffer);

                        float[] ohlc = ChartProcessor.extractOHLCFromBitmap(bitmap);
                        float[] prediction = modelPredictor.predictNextCandle(ohlc);

                        runOnUiThread(() -> predictionText.setText("Next Candle: " + prediction[0]));

                        image.close();
                    }
                } catch (Exception e) {
                    Log.e("CaptureError", "Error capturing image: " + e.getMessage());
                }
            }, null);

        } else {
            predictionText.setText("Screen capture permission denied.");
        }
    }
}
