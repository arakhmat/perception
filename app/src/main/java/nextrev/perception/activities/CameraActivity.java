package nextrev.perception.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import nextrev.perception.Perception;
import nextrev.perception.Prediction;
import nextrev.perception.R;

import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE;

public class CameraActivity extends Activity {

    Perception appContext;

    private static final String TAG = "PERCEPTION";
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private TextureView textureView;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private TextView tv;
    private AssetManager mgr;
    private boolean processing = false;
    private Image image = null;
    private ImageReader reader;

    Prediction prediction;

    static { System.loadLibrary("native-lib");  }
    public native void initializeNeuralNetwork(AssetManager mgr);
    public native Prediction predict(int h, int w, byte[] YUV, int rowStride, int pixelStride);
    private class SetUpNeuralNetwork extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void[] v) {
            try {
                initializeNeuralNetwork(mgr);
            } catch (Exception e) {
                Log.d(TAG, "Could not load neural network.");
            }
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        mgr = getResources().getAssets();

        new SetUpNeuralNetwork().execute();

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.camera_activity);

        appContext = ((Perception) this.getApplication());

        textureView = findViewById(R.id.textureView);
        textureView.setSystemUiVisibility(SYSTEM_UI_FLAG_IMMERSIVE);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        tv = findViewById(R.id.info_text);

        prediction = new Prediction();

    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            int width = 128;
            int height = 128;
            reader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 4);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    try {
                        image = reader.acquireNextImage();
                        if (processing) {
                            image.close();
                            return;
                        }

                        processing = true;
                        int w = image.getWidth();
                        int h = image.getHeight();
                        ByteBuffer YBuffer = image.getPlanes()[0].getBuffer();
                        ByteBuffer UBuffer = image.getPlanes()[1].getBuffer();
                        ByteBuffer VBuffer = image.getPlanes()[2].getBuffer();

                        int rowStride = image.getPlanes()[1].getRowStride();
                        int pixelStride = image.getPlanes()[1].getPixelStride();
                        byte[] YUV = new byte[YBuffer.capacity() + UBuffer.capacity() + VBuffer.capacity()];

                        YBuffer.get(YUV, 0, YBuffer.capacity());
                        UBuffer.get(YUV, YBuffer.capacity(), UBuffer.capacity());
                        VBuffer.get(YUV, YBuffer.capacity() + UBuffer.capacity(), VBuffer.capacity());

//                        Log.d(TAG, "H and W: " + h + " " + w);
//                        Log.d(TAG, "Y rowStride and pixelStride: " + image.getPlanes()[0].getRowStride() + " " + image.getPlanes()[0].getPixelStride());
//                        Log.d(TAG, "U rowStride and pixelStride: " + image.getPlanes()[1].getRowStride() + " " + image.getPlanes()[1].getPixelStride());
//                        Log.d(TAG, "V rowStride and pixelStride: " + image.getPlanes()[2].getRowStride() + " " + image.getPlanes()[2].getPixelStride());
//                        Log.d(TAG, "Y, U and V lengths: " + YBuffer.capacity() + " " + UBuffer.capacity() + " " + VBuffer.capacity());

                        prediction = predict(h, w, YUV, rowStride, pixelStride);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText(prediction.getInfo());
                                appContext.sendPredictionToArduino(Integer.toString(prediction.getValue()));
                                processing = false;
                            }
                        });

                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(reader.getSurface());
            if (appContext.flashlightOn()) {
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
            }

//            captureRequestBuilder.set(CaptureRequest.EDGE_MODE,
//                    CaptureRequest.EDGE_MODE_OFF);
//            captureRequestBuilder.set(
//                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
//                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
//            captureRequestBuilder.set(
//                    CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,
//                    CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF);
//            captureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,
//                    CaptureRequest.NOISE_REDUCTION_MODE_OFF);
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
//                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, true);
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, true);

            cameraDevice.createCaptureSession(Arrays.asList(surface, reader.getSurface()), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == cameraDevice) {
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CameraActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if(null == cameraDevice) {
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(CameraActivity.this, "You can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
}
