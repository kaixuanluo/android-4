package com.zdragon.videoio.Camera1mediaCodec2;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class use for Decode Video Frame Data and show to SurfaceTexture
 * Created by zj on 2018/7/29 0029.
 */
public class VideoDecoder {
    private final static String TAG = "VideoEncoder";
    private final static int CONFIGURE_FLAG_DECODE = 0;

    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private Surface mSurface;
    private int mViewWidth;
    private int mViewHeight;

    private VideoEncoder mVideoEncoder;
    private Handler mVideoDecoderHandler;
    private HandlerThread mVideoDecoderHandlerThread = new HandlerThread("VideoDecoder");

    private Handler mVideoReleaseHandler;
    private HandlerThread mVideoReleaseHandlerThread = new HandlerThread("VideoRelease") {
    };
    private MediaCodec.Callback mCallback;

    public VideoDecoder(String mimeType, Surface surface, int viewwidth, int viewheight) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mCallback = new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int id) {
                    ByteBuffer inputBuffer = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        inputBuffer = mediaCodec.getInputBuffer(id);
                    }
                    inputBuffer.clear();

                    byte[] dataSources = null;
                    if (mVideoEncoder == null) {
                        Log.d(TAG, "mVideoEncoder == null ... ");
                    } else {
                        Log.d(TAG, "mVideoEncoder != null ... ");
                        dataSources = mVideoEncoder.pollFrameFromEncoder();
                    }
                    int length = 0;
                    if (dataSources == null) {
                        Log.d(TAG, "dataSources == null ... ");
                    } else {
                        Log.d(TAG, "dataSources != null ... ");
                        inputBuffer.put(dataSources);
                        length = dataSources.length;
                    }
                    mediaCodec.queueInputBuffer(id, 0, length, 0, 0);
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int id,
                                                    @NonNull MediaCodec.BufferInfo bufferInfo) {
    //            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(id);
    //            MediaFormat outputFormat = mMediaCodec.getOutputFormat(id);
    //            if(mMediaFormat == outputFormat && outputBuffer != null && bufferInfo.size > 0){
    //                byte [] buffer = new byte[outputBuffer.remaining()];
    //                outputBuffer.get(buffer);
    //            }
    //            mMediaCodec.releaseOutputBuffer(id, true);

                    Message message = mVideoReleaseHandler.obtainMessage();
                    Object o = new Object[]{id, bufferInfo};
                    message.obj = o;
                    mVideoReleaseHandler.sendMessage(message);
                }

                @Override
                public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
                    Log.d(TAG, "------> onError");
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec,
                                                  @NonNull MediaFormat mediaFormat) {
                    Log.d(TAG, "------> onOutputFormatChanged");
                }
            };
        }

        try {
            mMediaCodec = MediaCodec.createDecoderByType(mimeType);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mMediaCodec = null;
            return;
        }

        if (surface == null) {
            return;
        }

        this.mViewWidth = viewwidth;
        this.mViewHeight = viewheight;
        this.mSurface = surface;

        mVideoDecoderHandlerThread.start();
        mVideoDecoderHandler = new Handler(mVideoDecoderHandlerThread.getLooper());

        mVideoReleaseHandlerThread.start();
        mVideoReleaseHandler = new Handler(mVideoReleaseHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Object[] obj = (Object[]) msg.obj;
                int id = (int) obj[0];
                MediaCodec.BufferInfo bufferInfo = (MediaCodec.BufferInfo) obj[1];

                ByteBuffer outputBuffer = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffer = mMediaCodec.getOutputBuffer(id);
                }
                MediaFormat outputFormat = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    outputFormat = mMediaCodec.getOutputFormat(id);
                }
                if (mMediaFormat == outputFormat && outputBuffer != null && bufferInfo.size > 0) {
                    byte[] buffer = new byte[outputBuffer.remaining()];
                    outputBuffer.get(buffer);
                }
                mMediaCodec.releaseOutputBuffer(id, true);
            }
        };

        mMediaFormat = MediaFormat.createVideoFormat(mimeType, mViewWidth, mViewHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mViewWidth * mViewHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
    }

    public void setEncoder(VideoEncoder videoEncoder) {
        this.mVideoEncoder = videoEncoder;
    }

    public void startDecoder() {
        if (mMediaCodec != null && mSurface != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mMediaCodec.setCallback(mCallback, mVideoDecoderHandler);
            }
            mMediaCodec.configure(mMediaFormat, mSurface, null, CONFIGURE_FLAG_DECODE);
            mMediaCodec.start();
        } else {
            throw new IllegalArgumentException("startDecoder failed, please check the MediaCodec " +
                    "is init correct");
        }
    }

    public void stopDecoder() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
        }
    }

    /**
     * release all resource that used in Encoder
     */
    public void release() {
        if (mMediaCodec != null) {
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }
}
