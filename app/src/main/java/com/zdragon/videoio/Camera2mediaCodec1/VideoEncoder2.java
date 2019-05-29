package com.zdragon.videoio.Camera2mediaCodec1;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.zdragon.videoio.Constants;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This class use for Encode Video Frame Data.
 * Created by zj on 2018/7/29 0029.
 */
public class VideoEncoder2 {
    private final static String TAG = "VideoEncoder";
    private final static int CONFIGURE_FLAG_ENCODE = MediaCodec.CONFIGURE_FLAG_ENCODE;

    private MediaCodec  mMediaCodec;
    private MediaFormat mMediaFormat;
    private int         mViewWidth;
    private int         mViewHeight;

    private Handler mVideoEncoderHandler;
    private HandlerThread mVideoEncoderHandlerThread = new HandlerThread("VideoEncoder");

    //This video stream format must be I420
    private final static ArrayBlockingQueue<byte []> mInputDatasQueue = new ArrayBlockingQueue<byte []>(8);
    //Cachhe video stream which has been encoded.
    private final static ArrayBlockingQueue<byte []> mOutputDatasQueue = new ArrayBlockingQueue<byte[]>(8);

    CameraActivity2.EncodeListener mEncodeListener;

    public void setEncodeListener(CameraActivity2.EncodeListener encodeListener) {
        this.mEncodeListener = encodeListener;
    }

    public VideoEncoder2(int viewwidth, int viewheight){
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mMediaCodec = null;
            return;
        }

        this.mViewWidth  = viewwidth;
        this.mViewHeight = viewheight;

        mVideoEncoderHandlerThread.start();
        mVideoEncoderHandler = new Handler(mVideoEncoderHandlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                byte[] buffer = (byte[]) msg.obj;
                if (mEncodeListener != null) {
                    mEncodeListener.onEncode(buffer);
                }
            }
        };

        mMediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mViewWidth, mViewHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mViewWidth * mViewHeight * 5);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        startEncoder();
    }

    /**
     * Input Video stream which need encode to Queue
     * @param needEncodeData I420 format stream
     */
//    public void inputFrameToEncoder(byte [] needEncodeData){
////        boolean inputResult = mInputDatasQueue.offer(needEncodeData);
//        mMediaCodec.queueInputBuffer();
//        Log.d(TAG, "-----> inputEncoder queue result = " + inputResult + " queue current size = " + mInputDatasQueue.size());
//    }

    long generateIndex;
    public void encodeData(byte[] data) {

        // 从输入缓冲区队列中拿到可用缓冲区，填充数据，再入队
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(Constants.ENCODE_TIMEOUT_USEC);
        if (inputBufferIndex >= 0) {
            // 计算时间戳
            long pts = generateIndex;
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            Log.d(TAG, "data.length " +data.length);
            Log.d(TAG, "inputBuffer.limit() " +inputBuffer.limit());
            ByteBuffer.allocate(data.length);
            try {
                inputBuffer.put(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, inputBuffer.limit(), pts, 0);
            generateIndex += 1;
        }

        ByteBuffer[] encoderOutputBuffers = new ByteBuffer[0];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        }

        boolean encoderDone = false;
        MediaCodec.BufferInfo bufferInfo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            bufferInfo = new MediaCodec.BufferInfo();
        }
        String infoString;

        Log.d(TAG, "解码开始。");
        while (!encoderDone) {
            int encoderStatus = 0;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    encoderStatus = mMediaCodec.dequeueOutputBuffer(bufferInfo, Constants.ENCODE_TIMEOUT_USEC);
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                break;
            }

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                //Log.d(TAG, "no output from encoder available");
                continue;
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    encoderOutputBuffers = mMediaCodec.getOutputBuffers();
                }
                Log.d(TAG, "encoder output buffers changed");
                break;
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // not expected for an encoder
                MediaFormat newFormat = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    newFormat = mMediaCodec.getOutputFormat();
                }
                Log.d(TAG, "encoder output format changed: " + newFormat);
                break;
            } else if (encoderStatus < 0) {
                Log.e(TAG, "encoderStatus < 0");
                continue;
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    Log.d(TAG, "============It's NULL. BREAK!=============");
                    continue;
                }

//                        infoString = info.offset + "," + info.size + "," +
//                                info.presentationTimeUs + "," + info.flags;
//                        try {
//                            mDos.write(infoString.getBytes());
//                            Log.d(TAG, "输出 info " + infoString);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }

                final byte[] b = new byte[bufferInfo.size];
                try {
                    if (bufferInfo.size != 0) {
                        encodedData.limit(bufferInfo.offset + bufferInfo.size);
                        encodedData.position(bufferInfo.offset);
                        encodedData.get(b, bufferInfo.offset, bufferInfo.offset + bufferInfo.size);

                        Message msg = mVideoEncoderHandler.obtainMessage();
                        msg.obj = b;
                        mVideoEncoderHandler.sendMessage(msg);

                    }

                } catch (BufferUnderflowException e) {
                    e.printStackTrace();
                }

                encoderDone = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                if (encoderDone) {
                    Log.d(TAG, "解码完成 。。。 ");
                    break;
                }

                try {
                    if (mMediaCodec == null) {
                        Log.e("ServerService ", "encoder is null");
                        continue;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                        break;
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }

        }
    }


    /**
     * Get Encoded frame from queue
     * @return a encoded frame; it would be null when the queue is empty.
     */
    public byte [] pollFrameFromEncoder(){
        return mOutputDatasQueue.poll();
    }

    /**
     * start the MediaCodec to encode video data
     */
    public void startEncoder(){
        if(mMediaCodec != null){
            mMediaCodec.configure(mMediaFormat, null, null, CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        }else{
            throw new IllegalArgumentException("startEncoder failed,is the MediaCodec has been init correct?");
        }
    }

    /**
     * stop encode the video data
     */
    public void stopEncoder(){
        if(mMediaCodec != null){
            mMediaCodec.stop();
            mMediaCodec.setCallback(null);
        }
    }

    /**
     * release all resource that used in Encoder
     */
    public void release(){
        if(mMediaCodec != null){
            mInputDatasQueue.clear();
            mOutputDatasQueue.clear();
            mMediaCodec.release();
        }
    }
}
