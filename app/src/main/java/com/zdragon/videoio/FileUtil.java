package com.zdragon.videoio;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author created by luokaixuan
 * @date 2019/5/22
 * 这个类是用来干嘛的
 */
public class FileUtil {

    private static final String SD_CARD_ROOT_PATH = "/storage/emulated/0/yuv/";

    private final static String TAG = FileUtil.class.getSimpleName();
    private FileOutputStream mOutPutStream;
    private File mInputFile;

    public FileUtil init() {
        mInputFile = getInputFile();
        mOutPutStream = getOutPutStream();
        return this;
    }

    private File getInputFile() {
        getSdCardPath();
        File file = new File(SD_CARD_ROOT_PATH, "aaa" + new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss"
        ).format(new Date()));
        if (file.getParentFile().exists()) {
            Log.d(TAG, "file.getParentFile().exists()");
        } else {
            Log.d(TAG, "file.getParentFile() !exists()");
            boolean mkdirs = file.mkdirs();
            Log.d(TAG, "file.mkdirs() " + mkdirs);
        }
        try {
            boolean newFile = file.createNewFile();
            Log.d(TAG, "file.createNewFile(); " + newFile);
        } catch (IOException e) {
            Log.d(TAG, "IOException e " + e);
            e.printStackTrace();
        }
        return file;
    }

    private File getSdCardPath() {
        File[] files;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            files = MyApplication.getContext().getExternalFilesDirs(Environment.MEDIA_MOUNTED);
            if (files.length > 0) {
                for (File file : files) {
                    if (file == null) {
                        Log.e(TAG, "存储卡的路径 file == null ... ");
                    } else {
                        Log.e(TAG, "存储卡的路径 " + file.getPath());
                    }
                }
                return files[0];
            } else {
                return new File(SD_CARD_ROOT_PATH);
            }
        }
        return new File(SD_CARD_ROOT_PATH);
    }

    static FileOutputStream fileOutputStream;

    private FileOutputStream getOutPutStream() {
        try {
            if (fileOutputStream == null) {
                fileOutputStream = new FileOutputStream(mInputFile);
            }
            return fileOutputStream;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    //保存H264视频到本地
//    byte[] bytes = new byte[mOutputBuffer.remaining()];
    public void saveH264DataToFile(byte[] dataToWrite) {
        try {
            Log.d(TAG, "saveH264DataToFile(byte[] dataToWrite) length " + dataToWrite.length);
            mOutPutStream.write(dataToWrite, 0, dataToWrite.length);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "FileNotFoundException e " + e);
        } catch (IOException e) {
            Log.d(TAG, "IOException e " + e);
        }
    }

}
