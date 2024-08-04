package com.hexatrip.plugins.hexa_image_picker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.media.ExifInterface;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;

public class FileUtils {

    private static final String TAG = "ImagePickerFileUtils";
    private static final String PRIMARY_VOLUME_NAME = "primary";

    public static String getFileName(Uri uri, final Context context) {
        String result = null;

        try {

            if (uri.getScheme().equals("content")) {
                Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        } catch (Exception ex){
            Log.e(TAG, "Failed to handle file name: " + ex.toString());
        }

        return result;
    }

    public static boolean clearCache(final Context context) {
        try {
            final File cacheDir = new File(context.getCacheDir() + "/image_picker/");
            final File[] files = cacheDir.listFiles();

            if (files != null) {
                for (final File file : files) {
                    file.delete();
                }
            }
        } catch (final Exception ex) {
            Log.e(TAG, "There was an error while clearing cached files: " + ex.toString());
            return false;
        }
        return true;
    }


    public static FileInfo openFileStream(final Context context, final Uri uri) {

        Log.i(TAG, "Caching from URI: " + uri.toString());
        FileOutputStream fos = null;
        final FileInfo.Builder fileInfo = new FileInfo.Builder();
        final String fileName = FileUtils.getFileName(uri, context);
        final String path = context.getCacheDir().getAbsolutePath() + "/image_picker/" + (fileName != null ? fileName : System.currentTimeMillis());

        final File file = new File(path);
        if(!file.exists()) {
            file.delete();
        }
        file.getParentFile().mkdirs();
        try {
            fos = new FileOutputStream(path);
            try {
                final BufferedOutputStream out = new BufferedOutputStream(fos);
                final InputStream in = context.getContentResolver().openInputStream(uri);
                final byte[] buffer = new byte[8192];
                int len = 0;

                while ((len = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, len);
                }

                out.flush();
            } finally {
                fos.getFD().sync();
            }
        } catch (final Exception e) {
            try {
                fos.close();
            } catch (final IOException | NullPointerException ex) {
                Log.e(TAG, "Failed to close file streams: " + e.getMessage(), null);
                return null;
            }
            Log.e(TAG, "Failed to retrieve path: " + e.getMessage(), null);
            return null;
        }

        Log.d(TAG, "File loaded and cached at:" + path);

        fileInfo
                .withPath(path)
                .withName(fileName)
                .withUri(uri)
                .withSize(Long.parseLong(String.valueOf(file.length())));

        return fileInfo.build();
    }

}
