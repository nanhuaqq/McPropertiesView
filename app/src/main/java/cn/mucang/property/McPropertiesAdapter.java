package cn.mucang.property;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by nanhuaqq on 2016/5/12.
 */
public interface McPropertiesAdapter {

    /**
     * 区域数目 （顶部的 tableHeader算一个section 这里需要注意 要特殊处理）
     * @return
     */
    int getSectionCount();
    int getRowCount(int section);
    int getTotalRowCount();
    int getSectionIndex(int rowIndex);
    boolean isSectionTitle(int rowIndex);
    int getRowIndexInSection(int rowIndex);
    int getColumnCount();

    void registerDataSetObserver(DataSetObserver observer);

    void unregisterDataSetObserver(DataSetObserver observer);


    /**
     * @param section
     * @param row
     * @param column
     * @param convertView
     * @param parent
     * @return
     */
    public View getCellView(int section, int row, int column, View convertView, ViewGroup parent);

    public View getCellTitleView(int section, int row, View convertView, ViewGroup parent);

    public View getTableHeaderView(int column, View convertView, ViewGroup parent);

    public View getSectionHeaderView(int section, View convertView, ViewGroup parent);

    public View getLeftCornerView(View convertView, ViewGroup parent);

    public int getItemViewType(int section, int row, int column);

    public int getViewTypeCount();
}
