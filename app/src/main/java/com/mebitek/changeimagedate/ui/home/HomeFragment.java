package com.mebitek.changeimagedate.ui.home;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import com.mebitek.changeimagedate.FileUtil;
import com.mebitek.changeimagedate.databinding.FragmentHomeBinding;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment {

 private static final ArrayList<String> SUPPORTED_EXIF_EXTENSIONS = new ArrayList<>(List.of(
         "jpg",
         "png",
         "jpeg",
         "webp"
 ));

 private static final boolean logEnabled = false;

 private FragmentHomeBinding binding;

 private final SimpleDateFormat defaultFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
 private final SimpleDateFormat exifDateFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
 private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

 private ActivityResultLauncher<Uri> openDocumentTreeActivity;

 @RequiresApi(api = Build.VERSION_CODES.S)
 public View onCreateView(@NonNull LayoutInflater inflater,
                          ViewGroup container, Bundle savedInstanceState) {

  binding = FragmentHomeBinding.inflate(inflater, container, false);
  View root = binding.getRoot();

  binding.buttonDirectory.setOnClickListener(view -> {
   openDocumentTreeActivity.launch(null);
  });

  binding.buttonStart.setOnClickListener(view -> {

   String defaultYear = binding.defaultYear.getText().toString();

   if (!isNumeric(defaultYear)) {
    Toast.makeText(getContext(),
            "Invalid year",
            Toast.LENGTH_LONG).show();
    return;
   }

   if (binding.textHome.getText().toString().equals("")) {
    Toast.makeText(getContext(),
            "Invalid path",
            Toast.LENGTH_LONG).show();
    return;
   }


   File directory = new File(binding.textHome.getText().toString());
   File[] files = directory.listFiles();
   if (files == null) {
    files = new File[0];
   }

   ProgressDialog progressBar = new ProgressDialog(getContext());
   progressBar.setCancelable(false);
   progressBar.setMessage("Elaborating images");
   progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
   progressBar.setProgress(0);
   progressBar.setMax(files.length);
   progressBar.show();

   File[] finalFiles = files;
   new Thread(() -> {
    for (File file : finalFiles) {
     String fileName = file.getName();
     if (logEnabled) {
      Log.d("Files", fileName);
     }

     String extension = FilenameUtils.getExtension(fileName);
     boolean exifSupported = SUPPORTED_EXIF_EXTENSIONS.contains(extension.toLowerCase()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
     try {
      if (fileName.startsWith("IMG")||fileName.startsWith("PXL")) {
       String[] fileNameParts = new String[0];
       if (fileName.contains("IMG_")||fileName.contains("PXL_")) {
        fileNameParts = fileName.split("_");
       } else if (fileName.contains("IMG-")||fileName.contains("PXL-")) {
        fileNameParts = fileName.split("-");
       }
       String datePart = defaultYear + "0101";
       if (fileNameParts.length == 3) {
        datePart = fileNameParts[1];
       }
       String timePart = "120000";
       if (!fileName.contains("WA") && fileNameParts.length > 2) {
        timePart = fileNameParts[2].substring(0, 6);
       }
       String filledDateString = datePart + timePart;

       setDates(dateFormat, filledDateString, exifSupported, file);

      } else if (fileName.startsWith("VID")) {
       String[] fileNameParts = new String[0];
       if (fileName.contains("VID_")) {
        fileNameParts = fileName.split("_");
       } else if (fileName.contains("VID-")) {
        fileNameParts = fileName.split("-");
       }

       String datePart = fileNameParts[1];
       if (datePart.length() != 8) {
        datePart = defaultYear + "0101";
       }
       String timePart = "120000";
       if (!fileName.contains("WA") && fileNameParts.length > 2) {
        timePart = fileNameParts[2].substring(0, 6);
       }

       Date parsedDate = dateFormat.parse(datePart + timePart);
       setFileDate(file, parsedDate);
      } else {
       //check if the file name is a valid date
       try {
        String[] fileNameParts = new String[0];
        if (fileName.contains("_")) {
         fileNameParts = fileName.split("_");
        } else if (fileName.contains("-")) {
         fileNameParts = fileName.split("-");
        }

        String datePart = fileNameParts[0];
        String timePart = "120000";
        if (!fileName.contains("WA") && fileNameParts.length > 1) {
         timePart = fileNameParts[1].substring(0, 6);
        }
        Date parsedDate = dateFormat.parse(datePart + timePart);
        assert parsedDate != null;
        if (!parsedDate.after(new Date())) {

         setFileDate(file, parsedDate);
        } else {
         setDefaultDate(file, defaultYear, exifSupported);
        }
       } catch (Exception e) {
        setDefaultDate(file, defaultYear, exifSupported);
       }

      }
     } catch (ParseException e) {
      setDefaultDate(file, defaultYear, exifSupported);
     }
     progressBar.setProgress(progressBar.getProgress() + 1);
    }
    progressBar.dismiss();
   }).start();
  });

  return root;
 }

 @RequiresApi(api = Build.VERSION_CODES.S)
 private void setDates(SimpleDateFormat dateFormat, String filledDateString, boolean exifSupported, File file) throws ParseException {
  Date parsedDate = dateFormat.parse(filledDateString);

  if (exifSupported) {
   Long exifDate = setExifDate(file, parsedDate);
   if (exifDate != null) {
    setFileDate(file, getDate(exifDate));
   }
  } else {
   assert parsedDate != null;
   setFileDate(file, parsedDate);
  }
 }

 @RequiresApi(api = Build.VERSION_CODES.S)
 private void setDefaultDate(File file, String defaultYear, boolean exifSupported) {
  try {
   String defaultDate = defaultYear + "0101120000";
   setDates(defaultFormatter, defaultDate, exifSupported, file);
  } catch (Exception ignored) {
  }
 }

 private Date getDate(Long date) {
  Calendar calendar = Calendar.getInstance();
  calendar.setTimeInMillis(date);
  return calendar.getTime();
 }

 @RequiresApi(api = Build.VERSION_CODES.S)
 private Long setExifDate(File file, Date date) {
  try {
   String exifDate = exifDateFormatter.format(date);
   ExifInterface exifInterface = new ExifInterface(file);
   if (exifInterface.getDateTime() == -1) {
    exifInterface.setAttribute(ExifInterface.TAG_DATETIME, exifDate);
    exifInterface.saveAttributes();
   }
   return exifInterface.getDateTime();
  } catch (IOException e) {
   Log.e("error saving exif date:", e.getMessage());
   return null;
  }
 }

 private void setFileDate(File file, Date date) {
  try {
   boolean result = file.setLastModified(date.getTime());
   if (result) {
    FileTime time = FileTime.fromMillis(date.getTime());
    Files.setAttribute(file.toPath(), "creationTime", time);
   }
  } catch (IOException ex) {
   Log.e("error saving file date: ", ex.getMessage());
  }
 }

 public static boolean isNumeric(String string) {
  if (string == null || string.equals("")) {
   return false;
  }

  try {
   Integer.parseInt(string);
   return true;
  } catch (NumberFormatException ignored) {
  }
  return false;
 }

 @Override
 public void onDestroyView() {
  super.onDestroyView();
  binding = null;
 }

 @Override
 public void onCreate(Bundle icicle) {
  super.onCreate(icicle);
  openDocumentTreeActivity = registerForActivityResult(
          new ActivityResultContracts.OpenDocumentTree(),
          uri -> {
           String path = FileUtil.getFullPathFromTreeUri(uri, getContext());
           binding.textHome.setText(path);

          });
 }

}