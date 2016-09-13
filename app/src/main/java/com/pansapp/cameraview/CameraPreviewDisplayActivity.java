package com.pansapp.cameraview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.ion.Ion;


public class CameraPreviewDisplayActivity extends AppCompatActivity {

    private ImageView previewImage, ok, cancel;
    String filePath, imageUrl, isFrom;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera_preview_display);

        try {
            previewImage = (ImageView) findViewById(R.id.preview_image);
            ok = (ImageView) findViewById(R.id.ok);
            cancel = (ImageView) findViewById(R.id.cancel);

            if (getIntent() != null && getIntent().getStringExtra("filePath") != null) {
                filePath = getIntent().getStringExtra("filePath");
                Ion.with(previewImage).load(filePath);


            }
            if (getIntent() != null && getIntent().getStringExtra("filePath") != null) {
                isFrom = getIntent().getStringExtra("isFrom");
            }



            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("filePath", filePath);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }
            });

            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }catch (Exception e){
            Log.e("TAG","TAG " + e.getMessage());
        }
    }

}
