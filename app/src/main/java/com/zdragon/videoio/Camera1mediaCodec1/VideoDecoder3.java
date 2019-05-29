package com.zdragon.videoio.Camera1mediaCodec1;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import com.zdragon.videoio.Constants;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class use for Decode Video Frame Data and show to SurfaceTexture
 * Created by zj on 2018/7/29 0029.
 */
public class VideoDecoder3 {
    private final static String TAG = "VideoEncoder";
    private final static int CONFIGURE_FLAG_DECODE = 0;

    private MediaCodec mMediaDeCodec;
    private MediaFormat mMediaFormat;
    private Surface mSurface;
    private int mViewWidth;
    private int mViewHeight;

    private Handler mVideoDecoderHandler;
    private HandlerThread mVideoDecoderHandlerThread = new HandlerThread("VideoDecoder");

    public VideoDecoder3(Surface surface, int viewwidth, int viewheight) {
        try {
            mMediaDeCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mMediaDeCodec = null;
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

        mMediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mViewWidth,
                mViewHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mViewWidth * mViewHeight * 5);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        initDecoder();
    }

    private void initDecoder() {
        if (mMediaDeCodec != null && mSurface != null) {
            mMediaDeCodec.configure(mMediaFormat, mSurface, null, CONFIGURE_FLAG_DECODE);
            mMediaDeCodec.start();

            mInputBuffers = mMediaDeCodec.getInputBuffers();
            mOutputBuffers = mMediaDeCodec.getOutputBuffers();

        } else {
            throw new IllegalArgumentException("startDecoder failed, please check the MediaCodec " +
                    "is init correct");
        }
    }

    public void stopDecoder() {
        if (mMediaDeCodec != null) {
            mMediaDeCodec.stop();
        }
    }

    /**
     * release all resource that used in Encoder
     */
    public void release() {
        if (mMediaDeCodec != null) {
            mMediaDeCodec.release();
            mMediaDeCodec = null;
        }
    }


    Handler inputLengthHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//            tvInputLength.setText(msg.obj.toString());
        }
    };

    long decodeStartTime;
    long decodeEndTime;
    int outputBufferIndex = 0;
    MediaCodec.BufferInfo bufferInfo;
    ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;

    public void render(byte[] buff) {

        ByteBuffer inputBuffer = null;

        int inputBufferIndex = mMediaDeCodec.dequeueInputBuffer(Constants.DECODE_TIMEOUT_USEC);
        if (inputBufferIndex >= 0) {
//                        inputBuffer = mMediaDeCodec.getInputBuffer(inputBufferIndex);
            inputBuffer = mInputBuffers[inputBufferIndex];

            int length = buff.length;
            if (length > 0) {
                String msg1 = "接收长度 " + length;
                Log.d(TAG, msg1);
                Message msg = new Message();
                msg.obj = msg1;
                inputLengthHandler.sendMessage(msg);
            }

            int le = buff.length;

            inputBuffer.clear();
            inputBuffer.put(buff);

            if (le == -1) {
                return;
            }

            mMediaDeCodec.queueInputBuffer(inputBufferIndex, 0, length,
                    Constants.DECODE_TIMEOUT_USEC, 0);
//                        bytesToImageFile(buff);

        }

        bufferInfo = new MediaCodec.BufferInfo();

        try {
            outputBufferIndex = mMediaDeCodec.dequeueOutputBuffer(bufferInfo,
                    Constants.DECODE_TIMEOUT_USEC);
        } catch (Exception e) {
            e.printStackTrace();
        }

//                    saveYUV2Local();
        render2Surface();

    }

    boolean render = true;

    private void render2Surface() {
        Log.d(TAG, "render2Surface outputBufferIndex " + outputBufferIndex);
        switch (outputBufferIndex) {
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                Log.v(TAG, "format changed");
                break;
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                Log.v(TAG, "解码当前帧超时");
                break;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                mOutputBuffers = mMediaDeCodec.getOutputBuffers();
                Log.v(TAG, "output buffers changed");
                break;
            default:
//                while (outputBufferIndex >= 0) {
                mMediaDeCodec.releaseOutputBuffer(outputBufferIndex, render);
                render = !render;
//                    outputBufferIndex = mMediaDeCodec.dequeueOutputBuffer(bufferInfo,
//                            Constants.DECODE_TIMEOUT_USEC);
//                }
                break;
        }
    }
}
