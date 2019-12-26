package com.ez08.imagecropdemo;


import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import de.hdodenhof.circleimageview.CircleImageView;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements  View.OnClickListener,EasyPermissions.PermissionCallbacks{

    private CircleImageView ivTest;

    private File cameraSavePath;//拍照照片路径
    private Uri uri;//照片Uri
    //权限
    private String[] permissions = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private String photoName = System.currentTimeMillis() + ".jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button btnGetPicFromCamera = findViewById(R.id.btn_get_pic_from_camera);
        Button btnGetPicFromPhotoAlbum = findViewById(R.id.btn_get_pic_form_photo_album);
        Button btnGetPermission = findViewById(R.id.btn_get_Permission);
        ivTest = findViewById(R.id.iv_test);

        btnGetPicFromCamera.setOnClickListener(this);
        btnGetPicFromPhotoAlbum.setOnClickListener(this);
        btnGetPermission.setOnClickListener(this);

        cameraSavePath = new File(Environment.getExternalStorageDirectory().getPath()+"/"+photoName);

    }


    //激活相机操作
    private void goCamera(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            //第二个参数为 包名.fileprovider
            uri = FileProvider.getUriForFile(MainActivity.this, "com.ez08.imagecropdemo.FileProvider", cameraSavePath);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }else{
            uri = Uri.fromFile(cameraSavePath);
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, 1);

    }

    //激活相册操作
    private void goPhotoAlbum() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 2);
    }


    private void  getPermission(){
        if (EasyPermissions.hasPermissions(this,permissions)){
            //已经打开权限
            Toast.makeText(this,"已经申请相关权限",Toast.LENGTH_SHORT).show();
        }else{
            //没有打开相关权限，申请权限
            EasyPermissions.requestPermissions(this,"需要获取您的相册,照相使用权限",1,permissions);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //框架要求必须这么写
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);

    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Toast.makeText(this, "相关权限获取成功", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Toast.makeText(this, "请同意相关权限，否则功能无法使用", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        String photoPath;
        if (requestCode == 1 && resultCode == RESULT_OK) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                photoPath = String.valueOf(cameraSavePath);
            } else {
                photoPath = uri.getEncodedPath();
            }
            Log.d("拍照返回图片路径:", photoPath);
           //对拍的照进行裁剪
            photoClip(uri);
            Glide.with(MainActivity.this).load(photoPath).into(ivTest);
        } else if (requestCode == 2 && resultCode == RESULT_OK) {
            photoPath = getPhotoFromPhotoAlbum.getRealPathFromUri(this, data.getData());
            Log.d("相册返回图片路径:", photoPath);
            photoClip(data.getData());
            Glide.with(MainActivity.this).load(photoPath).into(ivTest);
        }else if (requestCode == 3&& resultCode ==RESULT_OK){
            Bundle bundle = data.getExtras();

            if (bundle != null) {
                //在这里获得了剪裁后的Bitmap对象，可以用于上传
                Bitmap image = bundle.getParcelable("data");
                //设置到ImageView上
                ivTest.setImageBitmap(image);
                //也可以进行一些保存、压缩等操作后上传
                String path = saveImage("头像", image);
                Log.d("裁剪路径:", path);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_get_pic_from_camera:
                goCamera();
                break;
            case R.id.btn_get_pic_form_photo_album:
                goPhotoAlbum();
                break;
            case R.id.btn_get_Permission:
                getPermission();
                break;
        }
    }
    //裁剪图片的方法
    private void photoClip(Uri uri){
        //调用系统自带的图片裁剪
        Intent intent = new Intent("com.android.camera.action.CROP");
        //赋予临时访问权限
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(uri,"image/*");
        // 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop","true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        intent.putExtra("return-data", true);
        Uri uritempFile = Uri.parse("file://" + "/" + Environment.getExternalStorageDirectory().getPath() + "/" + System.currentTimeMillis() + ".jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uritempFile);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);

        startActivityForResult(intent, 3);
    }
    public String saveImage(String name, Bitmap bmp) {
        File appDir = new File(Environment.getExternalStorageDirectory().getPath());
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = name + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
