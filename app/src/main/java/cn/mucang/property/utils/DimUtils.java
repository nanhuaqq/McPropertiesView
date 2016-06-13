package cn.mucang.property.utils;
import android.content.Context;
/**
 * Created by qinqun on 2016/6/12.
 * Email : qinqun@mucang.cn
 */
public class DimUtils {
    public static int dip2px(Context context,float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dpValue * scale + 0.5F);
    }

    public static int px2dip(Context context,float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int)(pxValue / scale + 0.5F);
    }
}
