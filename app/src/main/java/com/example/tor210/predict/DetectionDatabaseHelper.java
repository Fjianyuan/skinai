package com.example.tor210.predict;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SQLite数据库帮助类，用于管理检测记录
 */
public class DetectionDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DetectionDatabaseHelper";
    
    // 数据库信息
    private static final String DATABASE_NAME = "detection_records.db";
    private static final int DATABASE_VERSION = 1;
    
    // 表名
    private static final String TABLE_RECORDS = "detection_records";
    
    // 列名
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_IMAGE_URL = "image_url";
    private static final String COLUMN_RECORD_TIME = "record_time";
    private static final String COLUMN_ACNE_COUNT = "acne_count";
    private static final String COLUMN_SEVERITY = "severity";
    private static final String COLUMN_ACNE_TYPES = "acne_types";
    private static final String COLUMN_RECOMMENDATION = "recommendation";
    
    // 日期格式
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    // 创建表SQL语句
    private static final String CREATE_TABLE_RECORDS = 
            "CREATE TABLE " + TABLE_RECORDS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_IMAGE_URL + " TEXT,"
            + COLUMN_RECORD_TIME + " TEXT,"
            + COLUMN_ACNE_COUNT + " INTEGER,"
            + COLUMN_SEVERITY + " TEXT,"
            + COLUMN_ACNE_TYPES + " TEXT,"
            + COLUMN_RECOMMENDATION + " TEXT"
            + ")";
    
    // 单例模式
    private static DetectionDatabaseHelper instance;
    
    public static synchronized DetectionDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DetectionDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    private DetectionDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_RECORDS);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 升级数据库时删除旧表并创建新表
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS);
        onCreate(db);
    }
    
    /**
     * 添加新的检测记录
     */
    public long addDetectionRecord(DetectionRecord record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_IMAGE_URL, record.getImageUrl());
        values.put(COLUMN_RECORD_TIME, DATE_FORMAT.format(record.getRecordTime()));
        values.put(COLUMN_ACNE_COUNT, record.getAcneCount());
        values.put(COLUMN_SEVERITY, record.getSeverity());
        values.put(COLUMN_ACNE_TYPES, record.getAcneTypes());
        values.put(COLUMN_RECOMMENDATION, record.getRecommendation());
        
        // 插入行并返回主键值
        long id = db.insert(TABLE_RECORDS, null, values);
        db.close();
        
        Log.d(TAG, "添加检测记录，ID: " + id);
        return id;
    }
    
    /**
     * 获取所有检测记录
     */
    public List<DetectionRecord> getAllDetectionRecords() {
        List<DetectionRecord> recordList = new ArrayList<>();
        
        String selectQuery = "SELECT * FROM " + TABLE_RECORDS + " ORDER BY " + COLUMN_RECORD_TIME + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        if (cursor.moveToFirst()) {
            do {
                DetectionRecord record = new DetectionRecord();
                record.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                record.setImageUrl(cursor.getString(cursor.getColumnIndex(COLUMN_IMAGE_URL)));
                
                String dateStr = cursor.getString(cursor.getColumnIndex(COLUMN_RECORD_TIME));
                try {
                    record.setRecordTime(DATE_FORMAT.parse(dateStr));
                } catch (ParseException e) {
                    Log.e(TAG, "日期解析错误: " + dateStr, e);
                    record.setRecordTime(new Date()); // 使用当前日期作为回退
                }
                
                record.setAcneCount(cursor.getInt(cursor.getColumnIndex(COLUMN_ACNE_COUNT)));
                record.setSeverity(cursor.getString(cursor.getColumnIndex(COLUMN_SEVERITY)));
                record.setAcneTypes(cursor.getString(cursor.getColumnIndex(COLUMN_ACNE_TYPES)));
                record.setRecommendation(cursor.getString(cursor.getColumnIndex(COLUMN_RECOMMENDATION)));
                
                recordList.add(record);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        Log.d(TAG, "获取所有检测记录，数量: " + recordList.size());
        return recordList;
    }
    
    /**
     * 获取特定ID的检测记录
     */
    public DetectionRecord getDetectionRecord(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_RECORDS,
                null,
                COLUMN_ID + "=?",
                new String[] { String.valueOf(id) },
                null, null, null, null
        );
        
        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
            return null;
        }
        
        DetectionRecord record = new DetectionRecord();
        record.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
        record.setImageUrl(cursor.getString(cursor.getColumnIndex(COLUMN_IMAGE_URL)));
        
        String dateStr = cursor.getString(cursor.getColumnIndex(COLUMN_RECORD_TIME));
        try {
            record.setRecordTime(DATE_FORMAT.parse(dateStr));
        } catch (ParseException e) {
            Log.e(TAG, "日期解析错误: " + dateStr, e);
            record.setRecordTime(new Date());
        }
        
        record.setAcneCount(cursor.getInt(cursor.getColumnIndex(COLUMN_ACNE_COUNT)));
        record.setSeverity(cursor.getString(cursor.getColumnIndex(COLUMN_SEVERITY)));
        record.setAcneTypes(cursor.getString(cursor.getColumnIndex(COLUMN_ACNE_TYPES)));
        record.setRecommendation(cursor.getString(cursor.getColumnIndex(COLUMN_RECOMMENDATION)));
        
        cursor.close();
        db.close();
        
        return record;
    }
    
    /**
     * 删除检测记录
     */
    public void deleteDetectionRecord(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECORDS, COLUMN_ID + "=?", new String[] { String.valueOf(id) });
        db.close();
        
        Log.d(TAG, "删除检测记录，ID: " + id);
    }
    
    /**
     * 删除所有检测记录
     */
    public void deleteAllDetectionRecords() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECORDS, null, null);
        db.close();
        
        Log.d(TAG, "删除所有检测记录");
    }
} 