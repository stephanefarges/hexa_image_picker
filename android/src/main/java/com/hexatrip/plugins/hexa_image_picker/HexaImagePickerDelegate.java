package com.hexatrip.plugins.hexa_image_picker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Message;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

public class HexaImagePickerDelegate implements PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener {

    private static final String TAG = "HexaImagePickerDelegate";
    private static final int REQUEST_CODE = (HexaImagePickerDelegate.class.hashCode() + 43) & 0x0000ffff;

    private final Activity activity;
    private final PermissionManager permissionManager;
    private MethodChannel.Result pendingResult;

    public HexaImagePickerDelegate(final Activity activity) {
        this(
                activity,
                null,
                new PermissionManager() {
                    @Override
                    public boolean isPermissionGranted(final String permissionName) {
                        return ActivityCompat.checkSelfPermission(activity, permissionName)
                                == PackageManager.PERMISSION_GRANTED;
                    }
                    public boolean askForPermissionsIfNeeded(final String[] permissions) {
                        List<String> notAlreadyGranted = new ArrayList<String>();
                        for(String permission : permissions) {
                            if (!isPermissionGranted(permission)) {
                                notAlreadyGranted.add(permission);
                            }
                        }
                        if (notAlreadyGranted.size() > 0) {
                            String[] permissionsToRequest = new String[notAlreadyGranted.size()];
                            notAlreadyGranted.toArray(permissionsToRequest);
                            ActivityCompat.requestPermissions(activity, permissionsToRequest, REQUEST_CODE);
                            return true;
                        } else {
                            return false;
                        }

                    }
                }
        );
    }

    @VisibleForTesting
    HexaImagePickerDelegate(final Activity activity, final MethodChannel.Result result, final PermissionManager permissionManager) {
        this.activity = activity;
        this.pendingResult = result;
        this.permissionManager = permissionManager;
    }


    @Override
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (data != null) {
                        final ArrayList<FileInfo> files = new ArrayList<>();

                        if (data.getClipData() != null) {
                            final int count = data.getClipData().getItemCount();
                            int currentItem = 0;
                            while (currentItem < count) {
                                final Uri currentUri = data.getClipData().getItemAt(currentItem).getUri();
                                final FileInfo file = FileUtils.openFileStream(HexaImagePickerDelegate.this.activity, currentUri);

                                if(file != null) {
                                    files.add(file);
                                    Log.d(HexaImagePickerDelegate.TAG, "[MultiFilePick] File #" + currentItem + " - URI: " + currentUri.getPath());
                                }
                                currentItem++;
                            }

                            finishWithSuccess(files);
                        } else if (data.getData() != null) {
                            Uri uri = data.getData();

                            final FileInfo file = FileUtils.openFileStream(HexaImagePickerDelegate.this.activity, uri);

                            if(file != null) {
                                files.add(file);
                            }

                            if (!files.isEmpty()) {
                                Log.d(HexaImagePickerDelegate.TAG, "File path:" + files.toString());
                                finishWithSuccess(files);
                            } else {
                                finishWithError("unknown_path", "Failed to retrieve path.");
                            }

                        } else if (data.getExtras() != null){
                            Bundle bundle = data.getExtras();
                            if (bundle.keySet().contains("selectedItems")) {
                                ArrayList<Parcelable> fileUris = getSelectedItems(bundle);

                                int currentItem = 0;
                                if (fileUris != null) {
                                    for (Parcelable fileUri : fileUris) {
                                        if (fileUri instanceof Uri) {
                                            Uri currentUri = (Uri) fileUri;
                                            final FileInfo file = FileUtils.openFileStream(HexaImagePickerDelegate.this.activity, currentUri);

                                            if (file != null) {
                                                files.add(file);
                                                Log.d(HexaImagePickerDelegate.TAG, "[MultiFilePick] File #" + currentItem + " - URI: " + currentUri.getPath());
                                            }
                                        }
                                        currentItem++;
                                    }
                                }
                                finishWithSuccess(files);
                            } else {
                                finishWithError("unknown_path", "Failed to retrieve path from bundle.");
                            }
                        } else {
                            finishWithError("unknown_activity", "Unknown activity error, please fill an issue.");
                        }
                    } else {
                        finishWithError("unknown_activity", "Unknown activity error, please fill an issue.");
                    }
                }
            }).start();
            return true;

        } else if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_CANCELED) {
            Log.i(TAG, "User cancelled the picker request");
            finishWithSuccess(null);
            return true;
        } else if (requestCode == REQUEST_CODE) {
            finishWithError("unknown_activity", "Unknown activity error, please fill an issue.");
        }
        return false;
    }

    @Override
    public boolean onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {

        if (REQUEST_CODE != requestCode) {
            return false;
        }

        boolean permissionGranted = true;
        for(int grantResult : grantResults)  {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                permissionGranted = false;
            }
        }
        if (permissionGranted) {
            this.startImagePicker();
        } else {
            finishWithError("permissions_denied", "User did not accept necessary permissions");
        }

        return true;
    }

    private boolean setPendingMethodCallAndResult(final MethodChannel.Result result) {
        if (this.pendingResult != null) {
            return false;
        }
        this.pendingResult = result;
        return true;
    }

    private static void finishWithAlreadyActiveError(final MethodChannel.Result result) {
        result.error("already_active", "File picker is already active", null);
    }

    @SuppressWarnings("deprecation")
    private ArrayList<Parcelable> getSelectedItems(Bundle bundle){
        if(Build.VERSION.SDK_INT >= 33){
            return bundle.getParcelableArrayList("selectedItems", Parcelable.class);
        }

        return bundle.getParcelableArrayList("selectedItems");
    }

    @SuppressWarnings("deprecation")
    private void startImagePicker() {

        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);

        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(pickIntent, "Import photos from");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{galleryIntent});

        if (chooserIntent.resolveActivity(this.activity.getPackageManager()) != null) {
            this.activity.startActivityForResult(chooserIntent, REQUEST_CODE);
        } else {
            Log.e(TAG, "Can't find a valid activity to handle the request. ");
            finishWithError("invalid_format_type", "Can't handle the provided file type.");
        }
    }

    String[] getNeededPermissions () {
        if (Build.VERSION.SDK_INT >= 33) {
            return  new String[] {Manifest.permission.ACCESS_MEDIA_LOCATION, Manifest.permission.READ_MEDIA_IMAGES};
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public void startImagePicker(final MethodChannel.Result result) {

        if (!this.setPendingMethodCallAndResult(result)) {
            finishWithAlreadyActiveError(result);
            return;
        }
        String[] permissions = getNeededPermissions();

        if(this.permissionManager.askForPermissionsIfNeeded(permissions)) {
            return;
        }

        this.startImagePicker();
    }

    @SuppressWarnings("unchecked")
    private void finishWithSuccess(Object data) {

        // Temporary fix, remove this null-check after Flutter Engine 1.14 has landed on stable
        if (this.pendingResult != null) {

            if(data != null && !(data instanceof String)) {
                final ArrayList<HashMap<String, Object>> files = new ArrayList<>();

                for (FileInfo file : (ArrayList<FileInfo>)data) {
                    files.add(file.toMap());
                }
                data = files;
            }

            this.pendingResult.success(data);
            this.clearPendingResult();
        }
    }

    private void finishWithError(final String errorCode, final String errorMessage) {
        if (this.pendingResult == null) {
            return;
        }

        this.pendingResult.error(errorCode, errorMessage, null);
        this.clearPendingResult();
    }

    private void clearPendingResult() {
        this.pendingResult = null;
    }

    interface PermissionManager {
        boolean isPermissionGranted(String permissionName);
        boolean askForPermissionsIfNeeded(final String[] permissions);
    }

}
