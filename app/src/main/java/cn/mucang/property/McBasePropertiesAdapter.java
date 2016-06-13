package cn.mucang.property;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

/**
 * Created by nanhuaqq on 2016/5/12.
 */
public abstract class McBasePropertiesAdapter implements McPropertiesAdapter{
    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    public void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

    public void notifyDataSetInvalidated() {
        mDataSetObservable.notifyInvalidated();
    }
}
