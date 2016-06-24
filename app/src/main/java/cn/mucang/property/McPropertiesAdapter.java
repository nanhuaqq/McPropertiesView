package cn.mucang.property;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by qinqun on 2016/6/16.
 * Email : qinqun@mucang.cn
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

    /**
     * 支持额外头部
     * @param column
     * @param convertView
     * @param parent
     * @return
     */
    public View getExtraTableHeaderView(int column, View convertView, ViewGroup parent);

    /**
     * 支持额外cell
     * @param section
     * @param row
     * @param column
     * @param convertView
     * @param parent
     * @return
     */
    public View getExtraCellView(int section, int row, int column, View convertView, ViewGroup parent);

    public int getItemViewType(int section, int row, int column);

    public int getViewTypeCount();

    public void reset();
}
