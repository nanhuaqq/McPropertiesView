package cn.mucang.property;

import android.view.View;

/**
 * Created by YangJin on 2015-02-04.
 */
public class DuiBiBase {
    public enum DuiBiButtonStatus {
        PLUS_DUI_BI,
        QU_XIAO_DUI_BI
    }

    public interface OnDuiBiClickListener {
        void onAddDuiBi(View v);
        void onRemoveDuiBi(View v);
    }
}
