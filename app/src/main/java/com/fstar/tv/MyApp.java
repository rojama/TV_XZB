package com.fstar.tv;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.util.Log;

import com.fstar.tv.tools.DatabaseHelper;
import com.fstar.tv.tools.ImageFileCache;
import com.fstar.tv.tools.ImageGetFromHttp;
import com.fstar.tv.tools.ImageMemoryCache;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MyApp extends Application{
    private ImageMemoryCache memoryCache;
    private ImageFileCache fileCache;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化图片缓存
        memoryCache=new ImageMemoryCache(this);
        fileCache=new ImageFileCache();
        dbHelper = new DatabaseHelper(this, "TV.db", null, 2);
        db = dbHelper.getWritableDatabase();
    }


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

    //收藏
    public void addColection(HashMap<String, Object> mediaInfo){
        db.execSQL("insert into colection(media_id , image, media_name, time) values(?, ? ,? ,? )"
                , new Object[]{mediaInfo.get("media_id"), mediaInfo.get("image"), mediaInfo.get("media_name"), new Date().toString()});
    }

    //查询收藏
    public ArrayList<HashMap> getColection(){
        ArrayList<HashMap> re = new ArrayList<>();
        Cursor cursor =  db.rawQuery("select * from colection order by time desc", null);
        while (cursor.moveToNext())
        {
            HashMap map = new HashMap();
            String media_id = cursor.getString(cursor.getColumnIndex("media_id"));
            String media_name =  cursor.getString(cursor.getColumnIndex("media_name"));
            String image =  cursor.getString(cursor.getColumnIndex("image"));
            map.put("media_id", media_id);
            map.put("media_name", media_name);
            map.put("image", image);
            re.add(map);
        }
        cursor.close();
        return re;
    }

    //取消收藏
    public void delColection(String media_id){
        db.execSQL("delete from colection where media_id=?", new Object[]{media_id});
    }

    //查询是否收藏
    public boolean isColection(String media_id){
        ArrayList<HashMap> re = new ArrayList<>();
        Cursor cursor =  db.rawQuery("select * from colection where media_id=?", new String[]{media_id});
        boolean has = false;
        while (cursor.moveToNext())
        {
            has = true;
        }
        cursor.close();
        return has;
    }


    //查询历史
    public ArrayList<HashMap> getHistory(){
        ArrayList<HashMap> re = new ArrayList<>();
        Cursor cursor =  db.rawQuery("select * from history order by time desc LIMIT 50", null);
        while (cursor.moveToNext())
        {
            HashMap map = new HashMap();
            String media_id = cursor.getString(cursor.getColumnIndex("media_id"));
            String media_name =  cursor.getString(cursor.getColumnIndex("media_name"));
            String image =  cursor.getString(cursor.getColumnIndex("image"));
            map.put("media_id", media_id);
            map.put("media_name", media_name);
            map.put("image", image);
            re.add(map);
        }
        cursor.close();
        return re;
    }

    //记录观看历史
    public void addHistory(HashMap<String, Object> mediaInfo){
        db.execSQL("delete from history where media_id=?", new Object[]{mediaInfo.get("media_id")});
        db.execSQL("insert into history(media_id , image, media_name, last_series, time) values(?, ? ,? ,? ,?)"
                , new Object[]{mediaInfo.get("media_id"), mediaInfo.get("image"), mediaInfo.get("media_name"), mediaInfo.get("series_no"), new Date().toString()});
    }

    public void updateLastTime(String media_id, int last_time){
        db.execSQL("update history set last_time=? where media_id=?", new Object[]{last_time, media_id});
        System.out.println("play to "+last_time);
    }

    public HashMap getHistroy(String media_id){
        Cursor cursor =  db.rawQuery("select * from history where media_id=?", new String[]{media_id});
        HashMap map = new HashMap();
        while (cursor.moveToNext())
        {
            int last_series =  cursor.getInt(cursor.getColumnIndex("last_series"));
            int last_time =  cursor.getInt(cursor.getColumnIndex("last_time"));
            map.put("last_series", last_series);
            map.put("last_time", last_time);
        }
        cursor.close();
        return map;
    }

    //清空历史记录
    public void clearHistory(){
        db.execSQL("delete from history");
    }
}