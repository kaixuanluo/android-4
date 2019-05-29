package com.zdragon.videoio.Camera1mediaCodec1OpenGl;

/**
 * @author created by luokaixuan
 * @date 2019/5/21
 * 这个类是用来干嘛的
 */
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import com.zdragon.videoio.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VideoRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = VideoRender.class.getSimpleName();

    private Context context;

    private final float[] vertexData = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f

    };

    private final float[] textureData = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;


    //mediacodec
    private int program_mediacodec;
    private int avPosition_mediacodec;
    private int afPosition_mediacodec;
    private int samplerOES_mediacodec;
    private int textureId_mediacodec;
    private SurfaceTexture surfaceTexture;
    private Surface surface;

    private OnSurfaceCreateListener onSurfaceCreateListener;
    private OnRenderListener onRenderListener;

    public VideoRender(Context context) {
        this.context = context;
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);
    }


    public void setOnSurfaceCreateListener(OnSurfaceCreateListener onSurfaceCreateListener) {
        this.onSurfaceCreateListener = onSurfaceCreateListener;
    }

    public void setOnRenderListener(OnRenderListener onRenderListener) {
        this.onRenderListener = onRenderListener;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initRenderMediacodec();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        renderMediacodec();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        Log.d(TAG, "onDrawFrame ");
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

        Log.d(TAG, "onFrameAvailable ");

        if (onRenderListener != null) {
            ////将onFrameAvailable函数回掉到GLSurfaceView调用requestRender()触发onDrawFrame()
            onRenderListener.onRender();
        }
    }

    private void initRenderMediacodec() {

        Log.d(TAG, "initRenderMediacodec ");

        String vertexSource = ShaderUtil.readRawTxt(context, R.raw.vertex_shader);
        String fragmentSource = ShaderUtil.readRawTxt(context, R.raw.fragment_mediacodec);

//        program_mediacodec = ShaderUtil.createProgram(ShaderUtil.vertexSource, ShaderUtil.fragmentSource);
        program_mediacodec = ShaderUtil.createProgram(vertexSource, fragmentSource);

        avPosition_mediacodec = GLES20.glGetAttribLocation(program_mediacodec, "av_Position");
        afPosition_mediacodec = GLES20.glGetAttribLocation(program_mediacodec, "af_Position");
        samplerOES_mediacodec = GLES20.glGetUniformLocation(program_mediacodec, "sTexture");

        int[] textureids = new int[1];
        GLES20.glGenTextures(1, textureids, 0);
        textureId_mediacodec = textureids[0];

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        surfaceTexture = new SurfaceTexture(textureId_mediacodec);
        surface = new Surface(surfaceTexture);
        surfaceTexture.setOnFrameAvailableListener(this);

        if (onSurfaceCreateListener != null) {
            //将Surface回掉出去给MediaCodec绑定渲染
            onSurfaceCreateListener.onSurfaceCreate(surface);
        }
    }

    private void renderMediacodec() {
        Log.d(TAG, "renderMediacodec");
        surfaceTexture.updateTexImage();
        GLES20.glUseProgram(program_mediacodec);

        GLES20.glEnableVertexAttribArray(avPosition_mediacodec);
        GLES20.glVertexAttribPointer(avPosition_mediacodec, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer);

        GLES20.glEnableVertexAttribArray(afPosition_mediacodec);
        GLES20.glVertexAttribPointer(afPosition_mediacodec, 2, GLES20.GL_FLOAT, false, 8, textureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        Log.d(TAG, "GLES20.glBindTexture start ... ");
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId_mediacodec);
        Log.d(TAG, "GLES20.glBindTexture end ... ");
        GLES20.glUniform1i(samplerOES_mediacodec, 0);
    }


    public interface OnSurfaceCreateListener {
        void onSurfaceCreate(Surface surface);
    }

    public interface OnRenderListener {
        void onRender();
    }
}
