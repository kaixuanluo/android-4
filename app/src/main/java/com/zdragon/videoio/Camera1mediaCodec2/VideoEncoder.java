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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This class use for Encode Video Frame Data.
 * Created by zj on 2018/7/29 0029.
 */
public class VideoEncoder {
    private final static String TAG = "VideoEncoder";
    private final static int CONFIGURE_FLAG_ENCODE = MediaCodec.CONFIGURE_FLAG_ENCODE;

    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private int mViewWidth;
    private int mViewHeight;

    private Handler mVideoEncoderHandler;
    private HandlerThread mVideoEncoderHandlerThread = new HandlerThread("VideoEncoder") {
    };

    //This video stream format must be I420
    private final static ArrayBlockingQueue<byte[]> mInputDatasQueue =
            new ArrayBlockingQueue<byte[]>(8);
    //Cachhe video stream which has been encoded.
    private final static ArrayBlockingQueue<byte[]> mOutputDatasQueue =
            new ArrayBlockingQueue<byte[]>(8);

    long generateIndex;
    private MediaCodec.Callback mCallback;

    public VideoEncoder(String mimeType, int viewwidth, int viewheight) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mCallback = new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int id) {
                    Log.d(TAG, "onInputBufferAvailable id " + id);
                    try {
                        ByteBuffer inputBuffer = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            inputBuffer = mediaCodec.getInputBuffer(id);
                        }
                        inputBuffer.clear();
                        byte[] dataSources = mInputDatasQueue.poll();
                        int length = 0;
                        if (dataSources == null) {
                            Log.d(TAG, "onInputBufferAvailable dataSources == null");
                        } else {
                            Log.d(TAG, "onInputBufferAvailable dataSources != null");
                            inputBuffer.put(dataSources);
                            length = dataSources.length;
                            generateIndex++;
                        }
                        mediaCodec.queueInputBuffer(id, 0, length, generateIndex, 0);
                    } catch (MediaCodec.CryptoException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int id,
                                                    @NonNull MediaCodec.BufferInfo bufferInfo) {

    //            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(id);
    //            MediaFormat outputFormat = mMediaCodec.getOutputFormat(id);
    //            Log.d(TAG, "Offer to queue failed, queue in full state");
    //            if (outputBuffer != null && bufferInfo.size > 0) {
    //                byte[] buffer = new byte[outputBuffer.remaining()];
    //                outputBuffer.get(buffer);
    //                boolean result = mOutputDatasQueue.offer(buffer);
    //                Log.d(TAG, "Offer to queue failed, queue in full state");
    //            }
    //            mMediaCodec.releaseOutputBuffer(id, false);

                    Log.d(TAG, "onOutputBufferAvailable ... id " + id);
                    Message message = mVideoEncoderHandler.obtainMessage();
                    Object obj = new Object[]{id, bufferInfo};
                    message.obj = obj;
                    mVideoEncoderHandler.sendMessage(message);

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
            mMediaCodec = MediaCodec.createEncoderByType(mimeType);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mMediaCodec = null;
            return;
        }

        this.mViewWidth = viewwidth;
        this.mViewHeight = viewheight;

        mVideoEncoderHandlerThread.start();
        mVideoEncoderHandler = new Handler(mVideoEncoderHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Object[] obj = (Object[]) msg.obj;

                int id = (int) obj[0];
                MediaCodec.BufferInfo bufferInfo = (MediaCodec.BufferInfo) obj[1];

                ByteBuffer outputBuffer = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffer = mMediaCodec.getOutputBuffer(id);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    MediaFormat outputFormat = mMediaCodec.getOutputFormat(id);
                }
                Log.d(TAG, "Offer to queue failed, queue in full state");

                if (outputBuffer != null && bufferInfo.size > 0) {
                    byte[] buffer = new byte[outputBuffer.remaining()];
                    outputBuffer.get(buffer);
                    boolean result = mOutputDatasQueue.offer(buffer);
                    Log.d(TAG, "Offer to queue failed, queue in full state");
                }

                mMediaCodec.releaseOutputBuffer(id, false);

            }
        };

        mMediaFormat = MediaFormat.createVideoFormat(mimeType, mViewWidth, mViewHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mViewWidth * mViewHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
    }

    /**
     * Input Video stream which need encode to Queue
     *
     * @param needEncodeData I420 format stream
     */
    public void inputFrameToEncoder(byte[] needEncodeData) {
        boolean inputResult = mInputDatasQueue.offer(needEncodeData);
        Log.d(TAG,
                "-----> inputEncoder queue result = " + inputResult + " queue current size = " + mInputDatasQueue.size());
    }

    /**
     * Get Encoded frame from queue
     *
     * @return a encoded frame; it would be null when the queue is empty.
     */
    public byte[] pollFrameFromEncoder() {
        return mOutputDatasQueue.poll();
    }

    /**
     * start the MediaCodec to encode video data
     */
    public void startEncoder() {
        if (mMediaCodec != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mMediaCodec.setCallback(mCallback, mVideoEncoderHandler);
                Log.d(TAG, "Build.VERSION.SDK_INT >= Build.VERSION_CODES.M");
            } else {
                Log.d(TAG, "Build.VERSION.SDK_INT < Build.VERSION_CODES.M");
            }
            mMediaCodec.configure(mMediaFormat, null, null, CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        } else {
            throw new IllegalArgumentException("startEncoder failed,is the MediaCodec has been " + "init correct?");
        }
    }

    /**
     * stop encode the video data
     */
    public void stopEncoder() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mMediaCodec.setCallback(null);
            }
        }
    }

    /**
     * release all resource that used in Encoder
     */
    public void release() {
        if (mMediaCodec != null) {
            stopEncoder();
            mInputDatasQueue.clear();
            mOutputDatasQueue.clear();
            mMediaCodec.release();
        }
    }
}
