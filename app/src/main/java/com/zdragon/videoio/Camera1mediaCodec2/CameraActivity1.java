package com.zdragon.videoio.Camera1mediaCodec2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.zdragon.videoio.BaseActivity;
import com.zdragon.videoio.R;

import java.io.IOException;
import java.util.List;

public class CameraActivity1 extends BaseActivity {

    private final static String TAG = "VideoIO";
    private final static String MIME_FORMAT = "video/avc"; //support h.264

    private TextureView mCameraTexture;
    private TextureView mDecodeTexture;

    private VideoDecoder mVideoDecoder;
    private VideoEncoder mVideoEncoder;

    private Camera mCamera;
    private int mPreviewWidth;
    private int mPreviewHeight;

    private Camera.PreviewCallback mPreviewCallBack = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
//            byte[] i420bytes = new byte[bytes.length];
//            //from YV20 TO i420
//            System.arraycopy(bytes, 0, i420bytes, 0, mPreviewWidth * mPreviewHeight);
//            System.arraycopy(bytes, mPreviewWidth * mPreviewHeight + mPreviewWidth * mPreviewHeight / 4, i420bytes, mPreviewWidth * mPreviewHeight, mPreviewWidth * mPreviewHeight / 4);
//            System.arraycopy(bytes, mPreviewWidth * mPreviewHeight, i420bytes, mPreviewWidth * mPreviewHeight + mPreviewWidth * mPreviewHeight / 4, mPreviewWidth * mPreviewHeight / 4);
            if(mVideoEncoder != null) {
                Log.d(TAG, "mVideoEncoder.inputFrameToEncoder(i420bytes);");
                mVideoEncoder.inputFrameToEncoder(bytes);
            }
        }
    };

    private TextureView.SurfaceTextureListener mCameraTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera(surfaceTexture,i, i1);
            mVideoEncoder = new VideoEncoder(MIME_FORMAT, mPreviewWidth, mPreviewHeight);
            mVideoEncoder.startEncoder();

            mDecodeTexture = (TextureView)findViewById(R.id.camera1decodeTv);
            mDecodeTexture.setSurfaceTextureListener(mDecodeTextureListener);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            if(mVideoEncoder != null){
                mVideoEncoder.release();
            }
            closeCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };

    private TextureView.SurfaceTextureListener mDecodeTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            System.out.println("----------" + i + " ," + i1);

            mVideoDecoder = new VideoDecoder(MIME_FORMAT, new Surface(surfaceTexture), mPreviewWidth, mPreviewHeight);
            mVideoDecoder.setEncoder(mVideoEncoder);
            mVideoDecoder.startDecoder();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            mVideoDecoder.stopDecoder();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        initView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera1);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST","Granted");
            initView();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }
    }

    private void initView(){
        mCameraTexture = (TextureView)findViewById(R.id.camera1PreviewTv);
        mCameraTexture.setSurfaceTextureListener(mCameraTextureListener);

    }

    private void openCamera(SurfaceTexture texture,int width, int height){
        if(texture == null){
            Log.e(TAG, "openCamera need SurfaceTexture");
            return;
        }

        mCamera = Camera.open(0);
        try{
            mCamera.setPreviewTexture(texture);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.YV12);
            List<Camera.Size> list = parameters.getSupportedPreviewSizes();
            for(Camera.Size size: list){
                System.out.println("----size width = " + size.width + " size height = " + size.height);
            }

            mPreviewWidth = 640;
            mPreviewHeight = 480;
            parameters.setPreviewSize(mPreviewWidth,mPreviewHeight);
            mCamera.setParameters(parameters);
            mCamera.setPreviewCallback(mPreviewCallBack);
            mCamera.startPreview();
        }catch(IOException e){
            Log.e(TAG, Log.getStackTraceString(e));
            mCamera = null;
        }
    }

    private void closeCamera(){
        if(mCamera == null){
            Log.e(TAG, "Camera not open");
            return;
        }
        mCamera.stopPreview();
        mCamera.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mVideoEncoder != null){
            mVideoEncoder.release();
        }
    }
}
