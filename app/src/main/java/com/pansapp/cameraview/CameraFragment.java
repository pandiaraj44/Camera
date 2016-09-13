package com.pansapp.cameraview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by pandiarajan on 13/9/16.
 */
public class CameraFragment extends Fragment implements SurfaceHolder.Callback {
    public static final int CAMERA = 1;
    public static final int GALLERY = 2;


    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final String IMAGE_PATH = "/camera";
    private static final String PREF_CAMERA = "PREF_CAMERA";
    private static final String CAMERA_PREFERENCES = "CAMERA_PREFERENCES";
    private View mView;
    private String filePath;
    private Bitmap bitmap;
    public static int degrees = 0;
    public ImageView camera;
    public ImageView gallery, cameraToggle;
    OrientationEventListener mOrientationEventListener;
    private int mOrientation =  -1;
    private static final int ORIENTATION_PORTRAIT_NORMAL =  1;
    private static final int ORIENTATION_PORTRAIT_INVERTED =  2;
    private static final int ORIENTATION_LANDSCAPE_NORMAL =  3;
    private static final int ORIENTATION_LANDSCAPE_INVERTED =  4;
    private int onClickOrientation =  1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_camera, container, false);

        // Create our Preview view and set it as the content of our activity.
        mSurfaceView = (SurfaceView) mView.findViewById(R.id.camera_preview);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        camera = (ImageView) mView.findViewById(R.id.camera);
        gallery = (ImageView) mView.findViewById(R.id.gallery);
        cameraToggle = (ImageView) mView.findViewById(R.id.camera_toggle);

        int numCameras = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            numCameras = Camera.getNumberOfCameras();
        }

        if(numCameras > 1){
            cameraToggle.setVisibility(View.VISIBLE);
        }else {
            cameraToggle.setVisibility(View.GONE);
        }

        if(getCameraPreferences(getActivity()) == Camera.CameraInfo.CAMERA_FACING_BACK){
            cameraToggle.setImageResource(R.drawable.ic_camera_front_white_36dp);
        }else if(getCameraPreferences(getActivity()) == Camera.CameraInfo.CAMERA_FACING_FRONT){
            cameraToggle.setImageResource(R.drawable.ic_camera_rear_white_36dp);
        }

        camera.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        try {
                            onClickOrientation = mOrientation;
                            mCamera.takePicture(null, null, mPicture);
                        }catch (Exception e){
                            Log.e("Tag","Error taking picture : " + e.getMessage());
                        }
                    }
                }
        );

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto, GALLERY);
            }
        });

        cameraToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getCameraPreferences(getActivity()) == Camera.CameraInfo.CAMERA_FACING_BACK){
                    setCameraPreferences(Camera.CameraInfo.CAMERA_FACING_FRONT);
                    cameraToggle.setImageResource(R.drawable.ic_camera_rear_white_36dp);
                }else if(getCameraPreferences(getActivity()) == Camera.CameraInfo.CAMERA_FACING_FRONT){
                    setCameraPreferences(Camera.CameraInfo.CAMERA_FACING_BACK);
                    cameraToggle.setImageResource(R.drawable.ic_camera_front_white_36dp);
                }

                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                mCamera = getCameraInstance(getActivity());
                refreshCamera();

            }
        });

        return mView;
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(Activity context){
        Camera c = null;
        try {
            c = Camera.open(getCameraPreferences(context)); // attempt to get a Camera instance
            //c.setDisplayOrientation(90);

            setCameraDisplayOrientation(context, getCameraPreferences(context),c);
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)

            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.e("TAG","Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                /*fos.write(data);
                fos.close();*/
                filePath = pictureFile.getPath();

                /*Bitmap bitmap = ExifUtil.rotateBitmap(6, BitmapFactory.decodeFile(filePath));

                FileOutputStream fos1 = new FileOutputStream(pictureFile);

                bitmap.compress(Bitmap.CompressFormat.JPEG,85,fos1);

                filePath = pictureFile.getPath();*/


                try {

                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = false;
                    opts.inPreferredConfig = Bitmap.Config.RGB_565;
                    opts.inDither = true;

                    Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length,opts);

                    ExifInterface exif = new ExifInterface(pictureFile.toString());

                    Log.d("TAG", "EXIF value >>" + exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                    if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")) {
                        realImage = rotate(realImage, 90);
                    } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")) {
                        realImage = rotate(realImage, 270);
                    } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")) {
                        realImage = rotate(realImage, 180);
                    } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("0")) {
                        if(getCameraPreferences(getActivity()) == Camera.CameraInfo.CAMERA_FACING_BACK){
                            switch (onClickOrientation){
                                case ORIENTATION_PORTRAIT_NORMAL:
                                    realImage = rotate(realImage, 90);
                                    break;
                                case ORIENTATION_PORTRAIT_INVERTED:
                                    realImage = rotate(realImage, -90);
                                    break;
                                case ORIENTATION_LANDSCAPE_NORMAL:
                                    break;
                                case ORIENTATION_LANDSCAPE_INVERTED:
                                    realImage = rotate(realImage, 180);
                                    break;
                            }
                        }else{
                            switch (onClickOrientation){
                                case ORIENTATION_PORTRAIT_NORMAL:
                                    realImage = rotate(realImage, -90);
                                    break;
                                case ORIENTATION_PORTRAIT_INVERTED:
                                    realImage = rotate(realImage, 90);
                                    break;
                                case ORIENTATION_LANDSCAPE_NORMAL:
                                    break;
                                case ORIENTATION_LANDSCAPE_INVERTED:
                                    realImage = rotate(realImage, -180);
                                    break;
                            }
                        }
                    }
                    boolean bo = realImage.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                }catch (Exception e){

                    e.printStackTrace();
                }

                fos.close();


                Intent cameraPreviewIntent = new Intent(getActivity(), CameraPreviewDisplayActivity.class);
                cameraPreviewIntent.putExtra("filePath", filePath);
                if(getArguments() !=null && getArguments().getString("isFrom") != null) {
                    cameraPreviewIntent.putExtra("isFrom", getArguments().getString("isFrom"));
                }
                startActivityForResult(cameraPreviewIntent, CAMERA);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Rotate the bitmap
     * @param bitmap
     * @param degrees
     * @return
     */
    Bitmap rotate(Bitmap bitmap, int degrees){
        try {
            Matrix matrix = new Matrix();
            matrix.setRotate(degrees);
            Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return oriented;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = Environment.getExternalStorageDirectory();
        File myFilePath = new File(mediaStorageDir.getAbsolutePath()
                + IMAGE_PATH);
        if (!myFilePath.exists())
            myFilePath.mkdir();

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(myFilePath.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(myFilePath.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        mCamera = getCameraInstance(getActivity());
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        refreshCamera();
    }

    public void refreshCamera(){
        if (mSurfaceHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
            e.printStackTrace();
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private void setCameraPreferences(int cameraPreferencesId){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(PREF_CAMERA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(CAMERA_PREFERENCES,cameraPreferencesId);
        editor.commit();
    }

    public static int getCameraPreferences(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_CAMERA, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(CAMERA_PREFERENCES,Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    public static void clearPreferences(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_CAMERA, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mOrientationEventListener == null) {
            mOrientationEventListener = new OrientationEventListener(getActivity()) {
                @Override
                public void onOrientationChanged(int orientation) {
                    // determine our orientation based on sensor response
                    int lastOrientation = mOrientation;

                    if (orientation >= 315 || orientation < 45) {
                        if (mOrientation != ORIENTATION_PORTRAIT_NORMAL) {
                            mOrientation = ORIENTATION_PORTRAIT_NORMAL;
                        }
                    }
                    else if (orientation < 315 && orientation >= 225) {
                        if (mOrientation != ORIENTATION_LANDSCAPE_NORMAL) {
                            mOrientation = ORIENTATION_LANDSCAPE_NORMAL;
                        }
                    }
                    else if (orientation < 225 && orientation >= 135) {
                        if (mOrientation != ORIENTATION_PORTRAIT_INVERTED) {
                            mOrientation = ORIENTATION_PORTRAIT_INVERTED;
                        }
                    }
                    else { // orientation <135 && orientation > 45
                        if (mOrientation != ORIENTATION_LANDSCAPE_INVERTED) {
                            mOrientation = ORIENTATION_LANDSCAPE_INVERTED;
                        }
                    }
                }
            };
        }

        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mOrientationEventListener.disable();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == CAMERA){
                /*getActivity().setResult(Activity.RESULT_OK,data);
                getActivity().finish();*/
            }

            if(requestCode == GALLERY){
                Uri selectedImage = data.getData();
                String[] filePaths = {MediaStore.Images.Media.DATA};
                Cursor c = getActivity().getContentResolver().query(selectedImage, filePaths, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePaths[0]);
                filePath = c.getString(columnIndex);
                c.close();
                Intent cameraPreviewIntent = new Intent(getActivity(), CameraPreviewDisplayActivity.class);
                cameraPreviewIntent.putExtra("filePath", filePath);
                if(getArguments() !=null && getArguments().getString("isFrom") != null) {
                    cameraPreviewIntent.putExtra("isFrom", getArguments().getString("isFrom"));
                }
                startActivityForResult(cameraPreviewIntent, CAMERA);
            }
        }
    }

}
