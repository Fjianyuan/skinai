package com.example.tor210.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.tor210.database.models.ClockInRecord;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 打卡记录数据库助手类
 */
public class ClockInDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "ClockInDatabaseHelper";
    
    // 数据库信息
    private static final String DATABASE_NAME = "clock_in_records.db";
    private static final int DATABASE_VERSION = 1;
    
    // 表名
    private static final String TABLE_RECORDS = "clock_in_records";
    
    // 列名
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_CLOCK_IN_DATE = "clock_in_date";
    private static final String COLUMN_IMAGE_PATH = "image_path";
    private static final String COLUMN_SKIN_STATUS = "skin_status";
    private static final String COLUMN_NOTES = "notes";
    private static final String COLUMN_STREAK_DAYS = "streak_days";
    
    // 日期格式
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    // 创建表SQL语句
    private static final String CREATE_TABLE_RECORDS = 
            "CREATE TABLE " + TABLE_RECORDS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USER_ID + " TEXT,"
            + COLUMN_CLOCK_IN_DATE + " TEXT,"
            + COLUMN_IMAGE_PATH + " TEXT,"
            + COLUMN_SKIN_STATUS + " TEXT,"
            + COLUMN_NOTES + " TEXT,"
            + COLUMN_STREAK_DAYS + " INTEGER"
            + ")";
    
    // 单例模式
    private static ClockInDatabaseHelper instance;
    
    public static synchronized ClockInDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ClockInDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    public ClockInDatabaseHelper(Context context) {
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
     * 添加一条打卡记录
     */
    public long addClockInRecord(ClockInRecord record) {
        SQLiteDatabase db = null;
        long id = -1;
        
        try {
            db = this.getWritableDatabase();
            
            // 设置时间为当前时间
            if (record.getClockInDate() == null) {
                record.setClockInDate(new Date());
            }
            
            // 检查连续打卡情况并设置连续天数
            int streakDays = getUserStreakDays(record.getUserId());
            
            // 判断是否是连续打卡
            boolean isConsecutive = isConsecutiveClockIn(record.getUserId());
            if (isConsecutive) {
                streakDays++;
            } else {
                streakDays = 1; // 非连续，重置为1
            }
            
            record.setStreakDays(streakDays);
            
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_ID, record.getUserId());
            values.put(COLUMN_CLOCK_IN_DATE, DATE_FORMAT.format(record.getClockInDate()));
            values.put(COLUMN_IMAGE_PATH, record.getImagePath());
            values.put(COLUMN_SKIN_STATUS, record.getSkinStatus());
            values.put(COLUMN_NOTES, record.getNotes());
            values.put(COLUMN_STREAK_DAYS, record.getStreakDays());
            
            id = db.insert(TABLE_RECORDS, null, values);
            Log.d(TAG, "添加打卡记录成功，ID: " + id);
        } catch (Exception e) {
            Log.e(TAG, "添加打卡记录失败: " + e.getMessage(), e);
        }
        
        return id;
    }
    
    /**
     * 计算用户的连续打卡天数
     */
    private int calculateStreakDays(String userId) {
        int streakDays = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        
        try {
            // 获取当前日期并格式化为yyyy-MM-dd
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            
            // 检查今天是否已经打卡，如果已打卡则不再计算
            String today = dayFormat.format(cal.getTime());
            String checkTodayQuery = "SELECT COUNT(*) FROM " + TABLE_RECORDS + 
                    " WHERE " + COLUMN_USER_ID + " = ?" +
                    " AND strftime('%Y-%m-%d', " + COLUMN_CLOCK_IN_DATE + ") = ?";
            
            Cursor todayCursor = db.rawQuery(checkTodayQuery, new String[] { userId, today });
            if (todayCursor.moveToFirst() && todayCursor.getInt(0) > 0) {
                todayCursor.close();
                
                // 已经打卡，获取上一条记录的连续天数
                String getLastStreakQuery = "SELECT " + COLUMN_STREAK_DAYS + 
                        " FROM " + TABLE_RECORDS + 
                        " WHERE " + COLUMN_USER_ID + " = ?" +
                        " ORDER BY " + COLUMN_CLOCK_IN_DATE + " DESC LIMIT 1";
                
                Cursor streakCursor = db.rawQuery(getLastStreakQuery, new String[] { userId });
                if (streakCursor.moveToFirst()) {
                    streakDays = streakCursor.getInt(0);
                }
                streakCursor.close();
                return streakDays;
            }
            todayCursor.close();
            
            // 获取用户最近的打卡记录
            cal.add(Calendar.DAY_OF_YEAR, -1); // 检查昨天
            String yesterday = dayFormat.format(cal.getTime());
            
            String lastClockInQuery = "SELECT COUNT(*) FROM " + TABLE_RECORDS + 
                    " WHERE " + COLUMN_USER_ID + " = ?" +
                    " AND strftime('%Y-%m-%d', " + COLUMN_CLOCK_IN_DATE + ") = ?";
            
            Cursor cursor = db.rawQuery(lastClockInQuery, new String[] { userId, yesterday });
            
            if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
                // 昨天有打卡，获取昨天的连续天数并+1
                cursor.close();
                
                String getLastStreakQuery = "SELECT " + COLUMN_STREAK_DAYS + 
                        " FROM " + TABLE_RECORDS + 
                        " WHERE " + COLUMN_USER_ID + " = ?" +
                        " AND strftime('%Y-%m-%d', " + COLUMN_CLOCK_IN_DATE + ") = ?" +
                        " ORDER BY " + COLUMN_CLOCK_IN_DATE + " DESC LIMIT 1";
                
                Cursor streakCursor = db.rawQuery(getLastStreakQuery, new String[] { userId, yesterday });
                if (streakCursor.moveToFirst()) {
                    streakDays = streakCursor.getInt(0) + 1;
                } else {
                    streakDays = 1; // 昨天打卡但无法获取连续天数，视为1
                }
                streakCursor.close();
            } else {
                streakDays = 1; // 昨天没打卡，从1开始计算
            }
            cursor.close();
            
        } catch (Exception e) {
            Log.e(TAG, "计算连续打卡天数时出错: " + e.getMessage(), e);
            streakDays = 1; // 错误情况下，默认为1
        }
        
        return streakDays;
    }
    
    /**
     * 获取用户的所有打卡记录
     */
    public List<ClockInRecord> getAllClockInRecords(String userId) {
        List<ClockInRecord> recordList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = COLUMN_USER_ID + " = ?";
        String[] selectionArgs = { userId };
        
        Cursor cursor = db.query(
                TABLE_RECORDS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                COLUMN_CLOCK_IN_DATE + " DESC" // 按时间倒序
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                ClockInRecord record = cursorToRecord(cursor);
                recordList.add(record);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return recordList;
    }
    
    /**
     * 获取用户的连续打卡天数
     */
    public int getUserStreakDays(String userId) {
        if (userId == null) {
            return 0;
        }
        
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT MAX(" + COLUMN_STREAK_DAYS + ") FROM " + TABLE_RECORDS + 
                       " WHERE " + COLUMN_USER_ID + " = ?";
        
        Cursor cursor = db.rawQuery(query, new String[] { userId });
        int streakDays = 0;
        
        if (cursor.moveToFirst()) {
            streakDays = cursor.getInt(0);
        }
        cursor.close();
        
        return streakDays;
    }
    
    /**
     * 判断是否是连续打卡（前一天有打卡记录）
     */
    public boolean isConsecutiveClockIn(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        // 获取当前日期
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        // 获取前一天日期
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);
        yesterday.set(Calendar.MILLISECOND, 0);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String yesterdayStr = sdf.format(yesterday.getTime());
        
        // 检查昨天是否有打卡记录
        String query = "SELECT COUNT(*) FROM " + TABLE_RECORDS + 
                " WHERE " + COLUMN_USER_ID + " = ?" +
                " AND strftime('%Y-%m-%d', " + COLUMN_CLOCK_IN_DATE + ") = ?";
        
        Cursor cursor = db.rawQuery(query, new String[] { userId, yesterdayStr });
        boolean isConsecutive = false;
        
        if (cursor.moveToFirst()) {
            isConsecutive = cursor.getInt(0) > 0;
        }
        cursor.close();
        
        return isConsecutive;
    }
    
    /**
     * 检查用户今天是否已经打卡
     */
    public boolean hasUserClockedInToday(String userId) {
        if (userId == null) {
            return false;
        }
        
        SQLiteDatabase db = this.getReadableDatabase();
        
        // 获取今天的日期字符串
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());
        
        // 检查今天是否已经打卡
        String query = "SELECT COUNT(*) FROM " + TABLE_RECORDS + 
                " WHERE " + COLUMN_USER_ID + " = ?" +
                " AND strftime('%Y-%m-%d', " + COLUMN_CLOCK_IN_DATE + ") = ?";
        
        Cursor cursor = db.rawQuery(query, new String[] { userId, today });
        boolean hasClockIn = false;
        
        if (cursor.moveToFirst()) {
            hasClockIn = cursor.getInt(0) > 0;
        }
        cursor.close();
        
        return hasClockIn;
    }
    
    /**
     * 获取用户某月的打卡日期列表
     */
    public List<Date> getMonthlyClockInDates(String userId, int year, int month) {
        List<Date> dateList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // 月份范围：0-11
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String yearMonth = sdf.format(calendar.getTime());
        
        String query = "SELECT " + COLUMN_CLOCK_IN_DATE + " FROM " + TABLE_RECORDS + 
                " WHERE " + COLUMN_USER_ID + " = ?" +
                " AND strftime('%Y-%m', " + COLUMN_CLOCK_IN_DATE + ") = ?";
        
        Cursor cursor = db.rawQuery(query, new String[] { userId, yearMonth });
        SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        if (cursor.moveToFirst()) {
            do {
                String dateStr = cursor.getString(0);
                try {
                    Date date = fullSdf.parse(dateStr);
                    if (date != null) {
                        dateList.add(date);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "解析日期时出错: " + e.getMessage());
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return dateList;
    }
    
    /**
     * 删除用户的所有打卡记录
     */
    public void deleteAllUserClockInRecords(String userId) {
        SQLiteDatabase db = null;
        
        try {
            db = this.getWritableDatabase();
            int rowsDeleted = db.delete(TABLE_RECORDS, COLUMN_USER_ID + "=?", new String[]{userId});
            Log.d(TAG, "删除用户所有打卡记录: " + userId + "，共删除 " + rowsDeleted + " 条记录");
        } catch (Exception e) {
            Log.e(TAG, "删除用户打卡记录失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除单条打卡记录
     * @param recordId 要删除的记录ID
     * @return 是否删除成功
     */
    public boolean deleteClockInRecord(long recordId) {
        SQLiteDatabase db = null;
        boolean success = false;
        
        try {
            db = this.getWritableDatabase();
            int rowsDeleted = db.delete(TABLE_RECORDS, COLUMN_ID + "=?", new String[]{String.valueOf(recordId)});
            success = rowsDeleted > 0;
            Log.d(TAG, "删除打卡记录: " + recordId + "，结果: " + (success ? "成功" : "失败"));
        } catch (Exception e) {
            Log.e(TAG, "删除打卡记录失败: " + e.getMessage(), e);
        }
        
        return success;
    }
    
    /**
     * 将Cursor转换为ClockInRecord对象
     */
    private ClockInRecord cursorToRecord(Cursor cursor) {
        ClockInRecord record = new ClockInRecord();
        
        record.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        record.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
        
        String dateStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLOCK_IN_DATE));
        try {
            record.setClockInDate(DATE_FORMAT.parse(dateStr));
        } catch (ParseException e) {
            Log.e(TAG, "日期解析错误: " + dateStr, e);
            record.setClockInDate(new Date()); // 使用当前日期作为回退
        }
        
        record.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)));
        record.setSkinStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SKIN_STATUS)));
        record.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)));
        record.setStreakDays(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STREAK_DAYS)));
        
        return record;
    }
} 