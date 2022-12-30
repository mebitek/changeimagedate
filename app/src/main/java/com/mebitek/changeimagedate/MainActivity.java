package com.mebitek.changeimagedate;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.mebitek.changeimagedate.databinding.ActivityMainBinding;

@RequiresApi(api = Build.VERSION_CODES.R)
public class MainActivity extends AppCompatActivity {

 private static final int REQUEST_EXTERNAL_STORAGE = 1;
 private static final String[] PERMISSIONS_STORAGE = {
         Manifest.permission.READ_EXTERNAL_STORAGE,
         Manifest.permission.WRITE_EXTERNAL_STORAGE,
         Manifest.permission.MANAGE_EXTERNAL_STORAGE,
 };

 @Override
 protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);

  ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
  setContentView(binding.getRoot());

  verifyStoragePermissions(this);
 }

 public void verifyStoragePermissions(Activity activity) {
  // Check if we have write permission
  if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
          ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
          ActivityCompat.checkSelfPermission(activity, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
   ActivityCompat.requestPermissions(
           activity,
           PERMISSIONS_STORAGE,
           REQUEST_EXTERNAL_STORAGE
   );
  }
  if (Build.VERSION.SDK_INT >= 30) {
   if (!Environment.isExternalStorageManager()) {
    Intent getpermission = new Intent();
    getpermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
    startActivity(getpermission);
   }
  }
 }

}