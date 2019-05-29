package com.zdragon.videoio.Camera2mediaCodec1;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;

import com.zdragon.videoio.BaseActivity;
import com.zdragon.videoio.R;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.zdragon.videoio.Constants.PHONE_HEIGHT;
import static com.zdragon.videoio.Constants.PHONE_WIDTH;

public class CameraActivity2 extends BaseActivity {

    TextureView mCameraTexture;
    Surface mCameraSurface;
    /**
     * mImageReader 这个必须定义为全局变量。因为他引用的 mImageReaderSurface 为弱引用，很快会被销毁
     */
    ImageReader mImageReader;
    Surface mImageReaderSurface;
    boolean mFlashSupported;
    String mCameraId;
    CameraDevice mCameraDevice;

    CaptureRequest.Builder mPreviewRequestBuilder;
    CameraCaptureSession mCaptureSession;

    CaptureRequest mPreviewRequest;

    HandlerThread mBackgroundThread;
    Handler mBackgroundHandler;

    HandlerThread mDecodeThread;
    Handler mDecodeHandler;

    HandlerThread mEncodeThread;
    Handler mEncodeHandler;

    HandlerThread mEncodeImgThread;
    Handler mEncodeImgHandler;

    private static final int REQUEST_CAMERA_PERMISSION = 123;
    private static final int sImageFormat = ImageFormat.YUV_420_888;
    private VideoDecoder2 mVideoDecoder2;
    private VideoEncoder2 mVideoEncoder2;
    private TextureView mDecodeTexture;
    private ImageView mDecodeIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera3);

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
                openCamera(mCameraTexture.getSurfaceTexture(), PHONE_WIDTH, PHONE_HEIGHT);
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

        mDecodeIv = findViewById(R.id.camera2DecodeIv);

        mDecodeTexture = findViewById(R.id.camera2decodeTv);
        mDecodeTexture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                initEncode();
                initDecode();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    private void initEncode() {
        mVideoEncoder2 = new VideoEncoder2(PHONE_WIDTH, PHONE_HEIGHT);
        mVideoEncoder2.setEncodeListener(new EncodeListener() {
            @Override
            public void onEncode(byte[] bytes) {
                mVideoDecoder2.render(bytes);
            }
        });
    }

    private void initDecode() {
        mVideoDecoder2 = new VideoDecoder2(new Surface(mDecodeTexture.getSurfaceTexture()), PHONE_WIDTH
                , PHONE_HEIGHT);
    }

    int count = 0;

    private void openCamera(SurfaceTexture texture, final int width, final int height) {
        if (texture == null) {
            Log.e(TAG, "openCamera need SurfaceTexture");
            return;
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //获取可用摄像头列表
            try {
                for (String cameraId : manager.getCameraIdList()) {
                    //获取相机的相关参数

                    mImageReader = ImageReader.newInstance(width, height, sImageFormat, 2);
                    mImageReaderSurface = mImageReader.getSurface();
                    mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(ImageReader reader) {

                            //                            Image img = null;
//                            img = reader.acquireLatestImage();
//                            try {
//                                if (img == null) {
//                                    throw new NullPointerException("cannot be null");
//                                } else {
//                                    ByteBuffer buffer = img.getPlanes()[0].getBuffer();
//                                    byte[] data = new byte[buffer.remaining()];
//                                    buffer.get(data);
//
//                                    Message msg = mEncodeHandler.obtainMessage();
//                                    msg.obj = data;
//                                    mEncodeHandler.sendMessage(msg);
//
//                                    img.close();
////                                    mVideoDecoder2.render(data);
//                                }
//                            } catch (Exception e) {
//                                Log.e(TAG, "img.getPlanes()[0].getBuffer e " + e);
//                            }

//                                                        Image img = null;
//                            img = reader.acquireLatestImage();
//                            try {
//                                if (img == null) {
//                                    throw new NullPointerException("cannot be null");
//                                } else {
////                                    ByteBuffer buffer = img.getPlanes()[0].getBuffer();
////                                    byte[] data = new byte[buffer.remaining()];
////                                    buffer.get(data);
//
////                                    ByteBuffer buffer;
////                                    for (int i = 0; i<3; i++){
////                                        buffer = img.getPlanes()[i].getBuffer();
////                                        byte[] bytes = new byte[buffer.remaining()]; // makes byte array large enough to hold image
////                                        buffer.get(bytes); // copies image from buffer to byte array
//////                                        output.write(bytes);    // write the byte array to file
////                                    }
//
//                                    for (int i = 0; i<3; i++) {
//                                        ByteBuffer buffer = img.getPlanes()[i].getBuffer();
//                                        byte[] data = new byte[buffer.remaining()];
//                                        buffer.get(data);
//                                        Message msg = mEncodeHandler.obtainMessage();
//                                        msg.obj = data;
//                                        mEncodeHandler.sendMessage(msg);
//                                    }
//
//                                    img.close();
////                                    mVideoDecoder2.render(data);
//                                }
//                            } catch (Exception e) {
//                                Log.e(TAG, "img.getPlanes()[0].getBuffer e " + e);
//                            }

//                            Image img = null;
//                            img = reader.acquireLatestImage();
//                            try {
//                                if (img == null) {
//                                    throw new NullPointerException("cannot be null");
//                                } else {
//                                    ByteBuffer buffer = img.getPlanes()[0].getBuffer();
//
//                                    byte[] data = new byte[buffer.remaining()];
//                                    buffer.get(data);
//
//                                    Message msg = mEncodeHandler.obtainMessage();
//                                    msg.obj = data;
//                                    mEncodeHandler.sendMessage(msg);
//
//                                    img.close();
////                                    mVideoDecoder2.render(data);
//                                }
//                            } catch (Exception e) {
//                                Log.e(TAG, "img.getPlanes()[0].getBuffer e " + e);
//                            }

//                            Image img = null;
//                            img = reader.acquireLatestImage();
//                            Log.d(TAG, "img.getFormat() " + img.getFormat()
//                                    + " ImageFormat.YUV_420_888 " + ImageFormat.YUV_420_888
//                            + " " + ImageFormat.JPEG);
//                            try {
//                                if (img == null) {
//                                    throw new NullPointerException("cannot be null");
//                                } else {
////                                    ByteBuffer buffer = img.getPlanes()[0].getBuffer();
////                                    byte[] data = new byte[buffer.remaining()];
////                                    buffer.get(data);
//
//                                    Image.Plane Y = img.getPlanes()[0];
//                                    Image.Plane U = img.getPlanes()[1];
//                                    Image.Plane V = img.getPlanes()[2];
//
//                                    int Yb = Y.getBuffer().remaining();
//                                    int Ub = U.getBuffer().remaining();
//                                    int Vb = V.getBuffer().remaining();
//
//                                    byte[] data = new byte[Yb + Ub + Vb];
//
//                                    Y.getBuffer().get(data, 0, Yb);
//                                    U.getBuffer().get(data, Yb, Ub);
//                                    V.getBuffer().get(data, Yb + Ub, Vb);
//
//                                    Message msg = mEncodeHandler.obtainMessage();
//                                    msg.obj = data;
//                                    mEncodeHandler.sendMessage(msg);
//
//                                    img.close();
////                                    mVideoDecoder2.render(data);
//                                }
//                            } catch (Exception e) {
//                                Log.e(TAG, "img.getPlanes()[0].getBuffer e " + e);
//                            }

                            Image img = null;
                            img = reader.acquireLatestImage();
                            Log.d(TAG, "img.getFormat() " + img.getFormat()
                                    + " ImageFormat.YUV_420_888 " + ImageFormat.YUV_420_888
                                    + " " + ImageFormat.JPEG);
                            try {
                                if (img == null) {
                                    throw new NullPointerException("cannot be null");
                                } else {
//                                    ByteBuffer buffer = img.getPlanes()[0].getBuffer();
//                                    byte[] data = new byte[buffer.remaining()];
//                                    buffer.get(data);

                                    Image.Plane Y = img.getPlanes()[0];
                                    Image.Plane U = img.getPlanes()[1];
                                    Image.Plane V = img.getPlanes()[2];

                                    int Yb = Y.getBuffer().remaining();
                                    int Ub = U.getBuffer().remaining();
                                    int Vb = V.getBuffer().remaining();

                                    byte[] data = new byte[Yb + Ub + Vb];

                                    Y.getBuffer().get(data, 0, Yb);
                                    U.getBuffer().get(data, Yb, Ub);
                                    V.getBuffer().get(data, Yb + Ub, Vb);

                                    Message msg = mEncodeHandler.obtainMessage();
                                    msg.obj = data;
                                    mEncodeHandler.sendMessage(msg);

                                    img.close();
//                                    mVideoDecoder2.render(data);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "img.getPlanes()[0].getBuffer e " + e);
                            }

//                            // 获取捕获的照片数据
//                            Image image = reader.acquireNextImage();
//                            Log.i(TAG, "image format: " + image.getFormat());
//                            // 从image里获取三个plane
//                            Image.Plane[] planes = image.getPlanes();
//
//                            for (int i = 0; i < planes.length; i++) {
//                                ByteBuffer iBuffer = planes[i].getBuffer();
//                                int iSize = iBuffer.remaining();
//                                Log.i(TAG, "pixelStride  " + planes[i].getPixelStride());
//                                Log.i(TAG, "rowStride   " + planes[i].getRowStride());
//                                Log.i(TAG, "width  " + image.getWidth());
//                                Log.i(TAG, "height  " + image.getHeight());
//                                Log.i(TAG, "Finished reading data from plane  " + i);
//                            }
//                            int n_image_size = image.getWidth() * image.getHeight() * 3 / 2;
//                            final byte[] yuv420pbuf = new byte[n_image_size];
//
//                            System.arraycopy(ImageUtil.getBytesFromImageAsType(image, 0), 0, yuv420pbuf, 0, n_image_size);
//
//                            Message msg = mEncodeHandler.obtainMessage();
//                            msg.obj = yuv420pbuf;
//                            mEncodeHandler.sendMessage(msg);
//
//                            image.close();

                        }
                    }, mBackgroundHandler);

                    CameraCharacteristics characteristics =
                            manager.getCameraCharacteristics(cameraId);
                    // 不使用前置摄像头。
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        continue;
                    }
                    StreamConfigurationMap map =
                            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map == null) {
                        continue;
                    }
                    // 检查闪光灯是否支持。
                    Boolean available =
                            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    mFlashSupported = available == null ? false : available;
                    mCameraId = cameraId;
                    Log.e(TAG, " 相机可用 ");
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "CameraAccessException e " + e);
                e.printStackTrace();
            }
        } catch (NullPointerException e) {
            //不支持Camera2API
        }

        try {
            //打开相机预览
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    CameraActivity2.this.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                }
                return;
            } else {
                manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            //创建CameraPreviewSession
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }

    };

    /**
     * 为相机预览创建新的CameraCaptureSession
     */
    private void createCameraPreviewSession() {

        try {
            //设置了一个具有输出Surface的CaptureRequest.Builder。
            mPreviewRequestBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(mCameraSurface);
            mPreviewRequestBuilder.addTarget(mImageReaderSurface);
            //创建一个CameraCaptureSession来进行相机预览。
            mCameraDevice.createCaptureSession(Arrays.asList(mCameraSurface, mImageReaderSurface)
                    , new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // 相机已经关闭
                    if (null == mCameraDevice) {
                        return;
                    }
                    // 会话准备好后，我们开始显示预览
                    mCaptureSession = cameraCaptureSession;
                    try {
                        // 自动对焦应
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        // 闪光灯
                        setAutoFlash(mPreviewRequestBuilder);
                        // 开启相机预览并添加事件
                        mPreviewRequest = mPreviewRequestBuilder.build();
                        //发送请求
                        mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                new CameraCaptureSession.CaptureCallback() {
                        }, mBackgroundHandler);
                        Log.e(TAG, " 开启相机预览并添加事件");
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, " onConfigureFailed 开启预览失败");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, " CameraAccessException 开启预览失败");
            e.printStackTrace();
        }
    }

    private static byte[] YUV_420_888toNV21(Image image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        return out.toByteArray();
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        startEncodeThread();
        startDecodeThread();
        startEncodeImgThread();
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
                mVideoDecoder2.render(data);
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
//                mVideoEncoder2.encodeData(data);

                YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,
                        480, 640, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0,
                                480, 640),
                        100, out);

                byte[] bytes = out.toByteArray();
                final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                Message msg1 = mEncodeImgHandler.obtainMessage();
                msg1.obj = bitmap;
                mEncodeImgHandler.sendMessage(msg1);

//                mFileUtil.saveH264DataToFile(data);
            }
        };
    }

    private void startEncodeImgThread() {
        mEncodeImgHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bitmap bitmap = (Bitmap) msg.obj;
                mDecodeIv.setImageBitmap(bitmap);
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
        if(mVideoEncoder2 != null){
            mVideoEncoder2.release();
        }
    }
}
