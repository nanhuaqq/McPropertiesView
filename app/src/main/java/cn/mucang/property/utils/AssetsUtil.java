package cn.mucang.property.utils;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.InputStream;
import java.util.List;

import cn.mucang.android.core.utils.LogUtils;
import cn.mucang.android.core.utils.MiscUtils;

/**
 * Created by qinqun on 2016/2/19.
 */
public class AssetsUtil {

    public static <T>  T readEntityFromAssets(Context context,String path,Class<T> clazz){
        String jsonStr = null;
        try {
            InputStream in = context.getResources().getAssets().open(path);
            int lenght = in.available();
            byte[]  buffer = new byte[lenght];
            in.read(buffer);
            jsonStr = new String(buffer, "utf-8");
        } catch (Exception e) {
            LogUtils.i("AssetsUtil", e.getMessage());
        }
        JSONObject jsonObject = null;
        if ( !MiscUtils.isEmpty(jsonStr) ){
            jsonObject = JSON.parseObject(jsonStr);
        }

        return JSON.parseObject(jsonObject.getString("data"), clazz);
    }

    public static <T> List<T> readEntityListFromAssets(Context context, String path, Class<T> clazz){
        String jsonStr = null;
        try {
            InputStream in = context.getResources().getAssets().open(path);
            int lenght = in.available();
            byte[]  buffer = new byte[lenght];
            in.read(buffer);
            jsonStr = new String(buffer, "utf-8");
        } catch (Exception e) {
        }
        JSONObject jsonObject = null;
        if ( !MiscUtils.isEmpty(jsonStr) ){
            jsonObject = JSON.parseObject(jsonStr);
        }
        return JSON.parseArray(jsonObject.getString("data"), clazz);
    }

}
