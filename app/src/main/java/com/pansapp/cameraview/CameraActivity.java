package com.pansapp.cameraview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * Created by pandiarajan on 13/9/16.
 */
public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        CameraFragment cameraFragment = new CameraFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_camera_preview, cameraFragment).commit();
    }
}
