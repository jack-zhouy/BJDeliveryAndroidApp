package com.gc.nfc.utils;

/**
 * Created by Administrator on 2018\5\7 0007.
 */
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.Context;
import android.widget.Toast;

import java.util.Map;

public class SharedPreferencesHelper {
    private static SharedPreferences sharedPreferences;
    /**
     * 保存在手机里面的名字
     */
    public static final String FILE_NAME = "shared_data";
    private static SharedPreferences.Editor editor;

    public SharedPreferencesHelper(Context context) {

    }

    public static void initial(Context context) {
        sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        //Toast.makeText(context, "sharedPreferences initial 成功", Toast.LENGTH_SHORT).show();
    }

    /**
     * 保存数据的方法，拿到数据保存数据的基本类型，然后根据类型调用不同的保存方法
     *
     * @param key
     * @param object
     */
    public static void put(String key, Object object) {
        if (object instanceof String) {
            editor.putString(key, (String) object);
        } else if (object instanceof Integer) {
            editor.putInt(key, (Integer) object);
        } else if (object instanceof Boolean) {
            editor.putBoolean(key, (Boolean) object);
        } else if (object instanceof Float) {
            editor.putFloat(key, (Float) object);
        } else if (object instanceof Long) {
            editor.putLong(key, (Long) object);
        } else if (object instanceof Double) {
            editor.putLong(key, Double.doubleToRawLongBits((Double)object));
        } else {
            editor.putString(key, object.toString());
        }
        editor.commit();
    }

    /**
     * 获取保存数据的方法，我们根据默认值的到保存的数据的具体类型，然后调用相对于的方法获取值
     *
     * @param key           键的值
     * @param defaultObject 默认值
     * @return
     */

    public static Object get(String key, Object defaultObject) {
        if (defaultObject instanceof String) {
            return sharedPreferences.getString(key, (String) defaultObject);
        } else if (defaultObject instanceof Integer) {
            return sharedPreferences.getInt(key, (Integer) defaultObject);
        } else if (defaultObject instanceof Boolean) {
            return sharedPreferences.getBoolean(key, (Boolean) defaultObject);
        } else if (defaultObject instanceof Float) {
            return sharedPreferences.getFloat(key, (Float) defaultObject);
        } else if (defaultObject instanceof Long) {
            return sharedPreferences.getLong(key, (Long) defaultObject);
        } else if (defaultObject instanceof Double) {
            return Double.longBitsToDouble(sharedPreferences.getLong(key, Double.doubleToLongBits((Double) defaultObject)));
        }else {
            return sharedPreferences.getString(key, null);
        }

    }

    /**
     * 移除某个key值已经对应的值
     *
     * @param key
     */
    public static void remove(String key) {
        editor.remove(key);
        editor.commit();
    }

    /**
     * 清除所有的数据
     */
    public static void clear() {
        editor.clear();
        editor.commit();
    }

    /**
     * 查询某个key是否存在
     *
     * @param key
     * @return
     */
    public static boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    /**
     * 返回所有的键值对
     *
     * @return
     */
    public static Map<String, ?> getAll() {
        return sharedPreferences.getAll();
    }
}