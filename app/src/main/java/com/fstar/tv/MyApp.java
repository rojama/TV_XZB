package com.fstar.tv;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.Log;

import com.fstar.tv.tools.ImageFileCache;
import com.fstar.tv.tools.ImageGetFromHttp;
import com.fstar.tv.tools.ImageMemoryCache;

public class MyApp extends Application{

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        //初始化图片缓存
        memoryCache=new ImageMemoryCache(this);
        fileCache=new ImageFileCache();
    }


    private ImageMemoryCache memoryCache;
    private ImageFileCache fileCache;

    public Bitmap getBitmap(String url) {
        // 从内存缓存中获取图片
        Bitmap result = memoryCache.getBitmapFromCache(url);
        if (result == null) {
            // 文件缓存中获取
            result = fileCache.getImage(url);
            if (result == null) {
                // 从网络获取
                result = ImageGetFromHttp.downloadBitmap(url);
                if (result != null) {
                    fileCache.saveBitmap(result, url);
                    memoryCache.addBitmapToCache(url, result);
                    Log.i("CACHE", "get from http");
                }
            } else {
                // 添加到内存缓存
                memoryCache.addBitmapToCache(url, result);
                Log.i("CACHE","get from file");
            }
        }else{
            Log.i("CACHE","get from memory");
        }
        return result;
    }

    public ImageFileCache getFileCache() {
        return fileCache;
    }
}