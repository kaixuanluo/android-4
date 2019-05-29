package com.zdragon.videoio.Camera1mediaCodec1OpenGl;

/**
 * @author created by luokaixuan
 * @date 2019/5/21
 * 这个类是用来干嘛的
 */
import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShaderUtil {
    private static final String TAG = "ShaderUtil";

    public static final String vertexSource = "attribute vec4 av_Position;//顶点位置\n" + "attribute vec2 af_Position;//纹理位置\n" + "varying vec2 v_texPo;//纹理位置  与fragment_shader交互\n" + "void main() {\n" + "    v_texPo = af_Position;\n" + "    gl_Position = av_Position;\n" + "}";
    public static final String fragmentSource = "precision mediump float;//精度 为float\n" + "varying vec2 v_texPo;//纹理位置  接收于vertex_shader\n" + "uniform sampler2D sampler_y;//纹理y\n" + "uniform sampler2D sampler_u;//纹理u\n" + "uniform sampler2D sampler_v;//纹理v\n" + "\n" + "void main() {\n" + "    //yuv420->rgb\n" + "    float y,u,v;\n" + "    y = texture2D(sampler_y,v_texPo).r;\n" + "    u = texture2D(sampler_u,v_texPo).r- 0.5;\n" + "    v = texture2D(sampler_v,v_texPo).r- 0.5;\n" + "    vec3 rgb;\n" + "    rgb.r = y + 1.403 * v;\n" + "    rgb.g = y - 0.344 * u - 0.714 * v;\n" + "    rgb.b = y + 1.770 * u;\n" + "\n" + "    gl_FragColor=vec4(rgb,1);\n" + "}";

    public static String readRawTxt(Context context, int rawId) {
        InputStream inputStream = context.getResources().openRawResource(rawId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer sb = new StringBuffer();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static int loadShader(int shaderType, String source) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            //添加代码到shader
            GLES20.glShaderSource(shader, source);
            //编译shader
            GLES20.glCompileShader(shader);
            int[] compile = new int[1];
            //检测是否编译成功
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compile, 0);
            if (compile[0] != GLES20.GL_TRUE) {
                Log.d(TAG, "shader compile error");
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        //获取vertex shader
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        //获取fragment shader
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            return 0;
        }
        //创建一个空的渲染程序
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            //添加vertexShader到渲染程序
            GLES20.glAttachShader(program, vertexShader);
            //添加fragmentShader到渲染程序
            GLES20.glAttachShader(program, fragmentShader);
            //关联为可执行渲染程序
            GLES20.glLinkProgram(program);
            int[] linsStatus = new int[1];
            //检测是否关联成功
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linsStatus, 0);
            if (linsStatus[0] != GLES20.GL_TRUE) {
                Log.d(TAG, "link program error");
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;

    }

}