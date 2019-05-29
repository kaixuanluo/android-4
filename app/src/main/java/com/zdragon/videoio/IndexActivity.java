package com.zdragon.videoio;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.zdragon.videoio.Camera1mediaCodec1.CameraActivity3;
import com.zdragon.videoio.Camera1mediaCodec1OpenGl.CameraActivity4;
import com.zdragon.videoio.Camera1mediaCodec2.CameraActivity1;
import com.zdragon.videoio.Camera2mediaCodec1.CameraActivity2;

/**
 * @author created by luokaixuan
 * @date 2019/5/24
 * 这个类是用来干嘛的
 */
public class IndexActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_index);

        findViewById(R.id.index_camera_1_bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(IndexActivity.this, CameraActivity1.class));
            }
        });

        findViewById(R.id.index_camera_2_bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(IndexActivity.this, CameraActivity2.class));
            }
        });

        findViewById(R.id.index_camera_3_bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(IndexActivity.this, CameraActivity3.class));
            }
        });

        findViewById(R.id.index_camera_4_bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(IndexActivity.this, CameraActivity4.class));
            }
        });

    }
}
