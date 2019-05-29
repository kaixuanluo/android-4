package com.zdragon.videoio.Camera1mediaCodec1OpenGl;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.zdragon.videoio.BaseActivity;
import com.zdragon.videoio.R;

import java.io.IOException;
import java.util.List;

public class CameraActivity4 extends BaseActivity {

    TextureView mCameraTexture;
    Surface mCameraSurface;
    /**
     * mImageReader 这个必须定义为全局变量。因为他引用的 mImageReaderSurface 为弱引用，很快会被销毁
     */

    HandlerThread mBackgroundThread;
    Handler mBackgroundHandler;

    HandlerThread mDecodeThread;
    Handler mDecodeHandler;

    HandlerThread mEncodeThread;
    Handler mEncodeHandler;

    private static final int REQUEST_CAMERA_PERMISSION = 123;
    private static final int sImageFormat = ImageFormat.YUV_420_888;
    private VideoDecoder4 mVideoDecoder4;
    private VideoEncoder4 mVideoEncoder4;
    private VideoGLSurfaceView mDecodeTexture;

    Camera mCamera;

    int mPreviewWidth = 480;
    int mPreviewHeight = 640;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera4);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST", "Granted");
            initView();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            //1 can be another integer
        }
    }

    private void initView() {
        mCameraTexture = (TextureView) findViewById(R.id.camera2PreviewTv);
        mCameraTexture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mCameraSurface = new Surface(mCameraTexture.getSurfaceTexture());
                openCamera(mCameraTexture.getSurfaceTexture());
                Log.d(TAG, "onSurfaceTextureAvailable ");
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureSizeChanged ");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d(TAG, "onSurfaceTextureDestroyed ");
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

        mDecodeTexture = findViewById(R.id.camera2decodeTv);
        mDecodeTexture.getWlRender().setOnSurfaceCreateListener(new VideoRender.OnSurfaceCreateListener() {
            @Override
            public void onSurfaceCreate(Surface surface) {
                initEncode();
                initDecode(surface);
            }
        });
    }

    private void initEncode() {
        mVideoEncoder4 = new VideoEncoder4(mPreviewWidth, mPreviewHeight);
        mVideoEncoder4.setEncodeListener(new EncodeListener() {
            @Override
            public void onEncode(byte[] bytes) {
                enCodeCallback(bytes);
            }
        });
    }

    private void enCodeCallback(byte[] bytes) {
        decodeStart(bytes);
    }

    private void decodeStart(byte[] bytes) {
        mVideoDecoder4.render(bytes);
    }

    private void initDecode(Surface surface) {
        mVideoDecoder4 = new VideoDecoder4(surface,
                mPreviewWidth, mPreviewHeight);
    }

    int count = 0;

    private void openCamera(SurfaceTexture texture) {
        if (texture == null) {
            Log.e(TAG, "openCamera need SurfaceTexture");
            return;
        }

        mCamera = Camera.open(0);
        try {
            mCamera.setPreviewTexture(texture);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.YV12);
            List<Camera.Size> list = parameters.getSupportedPreviewSizes();
            for (Camera.Size size : list) {
                System.out.println("----size width = " + size.width + " size height = " + size.height);
            }

            parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
            mCamera.setParameters(parameters);
            mCamera.setPreviewCallback(mPreviewCallBack);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mCamera = null;
        }
    }

    private Camera.PreviewCallback mPreviewCallBack = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
//            byte[] i420bytes = new byte[bytes.length];
//            //from YV20 TO i420
//            System.arraycopy(bytes, 0, i420bytes, 0, mPreviewWidth * mPreviewHeight);
//            System.arraycopy(bytes, mPreviewWidth * mPreviewHeight + mPreviewWidth *
//            mPreviewHeight / 4, i420bytes, mPreviewWidth * mPreviewHeight, mPreviewWidth *
//            mPreviewHeight / 4);
//            System.arraycopy(bytes, mPreviewWidth * mPreviewHeight, i420bytes, mPreviewWidth *
//            mPreviewHeight + mPreviewWidth * mPreviewHeight / 4, mPreviewWidth * mPreviewHeight
//            / 4);
////            if(mVideoEncoder != null) {
////                mVideoEncoder.inputFrameToEncoder(i420bytes);
////            }
//            byte[] i420bytes = new byte[bytes.length];
//            i420bytes = ImageUtil.yuv420pTo420sp(bytes, i420bytes, mPreviewWidth, mPreviewHeight);

//            byte[] i420bytes = new byte[bytes.length];
//            i420bytes = YV12toYUV420Planar(bytes, i420bytes, mPreviewWidth, mPreviewHeight);

            Message msg = mEncodeHandler.obtainMessage();
            msg.obj = bytes;
            mEncodeHandler.sendMessage(msg);
        }
    };

    public static byte[] YV12toYUV420Planar(byte[] input, byte[] output, int width, int height) {
        /*
         * COLOR_FormatYUV420Planar is I420 which is like YV12, but with U and V reversed.
         * So we just have to reverse U and V.
         */
        final int frameSize = width * height;
        final int qFrameSize = frameSize / 4;

        System.arraycopy(input, 0, output, 0, frameSize); // Y
        System.arraycopy(input, frameSize, output, frameSize + qFrameSize, qFrameSize); // Cr (V)
        System.arraycopy(input, frameSize + qFrameSize, output, frameSize, qFrameSize); // Cb (U)

        return output;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        startEncodeThread();
        startDecodeThread();
    }

    /**
     * 启动一个HandlerThread
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
    }

    private void startDecodeThread() {
        mDecodeThread = new HandlerThread("DecodeBackground");
        mDecodeThread.start();
        mDecodeHandler = new Handler(mDecodeThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                byte[] data = (byte[]) msg.obj;
                mVideoDecoder4.render(data);
            }
        };
    }

    private void startEncodeThread() {
        mEncodeThread = new HandlerThread("EncodeBackground");
        mEncodeThread.start();
        mEncodeHandler = new Handler(mEncodeThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                byte[] data = (byte[]) msg.obj;
                mVideoEncoder4.encodeData(data);
            }
        };
    }

    public interface EncodeListener {
        void onEncode(byte[] bytes);
    }

//    private void closeCamera(){
//        if(mCamera == null){
//            Log.e(TAG, "Camera not open");
//            return;
//        }
//        mCamera.stopPreview();
//        mCamera.release();
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mVideoEncoder4 != null){
            mVideoEncoder4.release();
        }
    }
}
