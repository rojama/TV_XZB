package com.fstar.tv.tools;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String CREATE_COLECTION = "create table colection (" +
            "media_id text primary key , " +
            "image text, " +
            "media_name text," +
            "time text)";

    public static final String CREATE_HISTORY = "create table history (" +
            "media_id text primary key , " +
            "image text, " +
            "media_name text," +
            "time text)";

    public static final String DROP_COLECTION = "drop table colection";
    public static final String DROP_HISTORY = "drop table history";

    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_COLECTION);
        db.execSQL(CREATE_HISTORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_COLECTION);
        db.execSQL(DROP_HISTORY);
        db.execSQL(CREATE_COLECTION);
        db.execSQL(CREATE_HISTORY);
    }


}
