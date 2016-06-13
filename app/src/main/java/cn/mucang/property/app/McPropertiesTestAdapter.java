package cn.mucang.property.app;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.BitSet;
import java.util.List;

import cn.mucang.property.McBasePropertiesAdapter;
import cn.mucang.property.McPropertyDataType;
import cn.mucang.property.R;
import cn.mucang.property.data.CanPeiCarEntity;
import cn.mucang.property.data.CanPeiGroup;
import cn.mucang.property.data.CanPeiRow;
import cn.mucang.property.data.CanPeiRowCell;
import cn.mucang.property.data.GetCarPropertiesResultEntity;

/**
 * Created by qinqun on 2016/6/12.
 * Email : qinqun@mucang.cn
 */
public class McPropertiesTestAdapter extends McBasePropertiesAdapter{
    private GetCarPropertiesResultEntity getCarPropertiesResultEntity;
    private List<CanPeiGroup> sections;
    private List<CanPeiCarEntity> headerCars;
    private LayoutInflater inflater;

    private SparseArray<Integer> rowIndexMapSectionArray;
    private SparseArray<Integer> rowIndexMapRowIndexInSectionArray;
    private BitSet sectionTitlePositionsSet;
    private int totalSectionCount;
    private int totalRowCount;

    public McPropertiesTestAdapter(Context context,GetCarPropertiesResultEntity getCarPropertiesResultEntity) {
        this.getCarPropertiesResultEntity = getCarPropertiesResultEntity;
        this.inflater = LayoutInflater.from(context);
        if (  this.getCarPropertiesResultEntity != null ){
            this.sections = this.getCarPropertiesResultEntity.getItems();
            this.headerCars = this.getCarPropertiesResultEntity.getCars();
        }
    }

    @Override
    public int getSectionCount() {
        if ( this.sections == null ){
            totalSectionCount = 0;
        }else{
            totalSectionCount = this.sections.size();
        }
        return totalSectionCount;
    }

    @Override
    public int getRowCount( int sectionIndex ) {
        if ( sectionIndex >= getSectionCount() ){
            return 0;
        }else {
            CanPeiGroup sectionData = this.sections.get(sectionIndex);
            return sectionData == null || sectionData.getItems() == null  ? 0 : sectionData.getItems().size();
        }
    }

    @Override
    public int getTotalRowCount() {

        if ( rowIndexMapSectionArray == null ){
            rowIndexMapSectionArray = new SparseArray<>();
        }

        if ( rowIndexMapRowIndexInSectionArray == null ){
            rowIndexMapRowIndexInSectionArray = new SparseArray<>();
        }
        if ( sectionTitlePositionsSet == null ){
            sectionTitlePositionsSet = new BitSet();
        }

        //1 + sectionCount + 每一个getRowCount(int sectionIndex)
        if ( totalRowCount == 0 ){
            totalRowCount = 1;
            rowIndexMapSectionArray.put(0,-1);
            int sectionCount = getSectionCount();
            for ( int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++ ){
                int rowCountInThisSection = getRowCount(sectionIndex);
                int endRowIndex = totalRowCount + (rowCountInThisSection + 1);
                int rowIndexInSection = 0;
                for ( int startRowIndex = totalRowCount; startRowIndex < endRowIndex; startRowIndex++,rowIndexInSection ++ ){
                    rowIndexMapSectionArray.put(startRowIndex,sectionIndex);
                    rowIndexMapRowIndexInSectionArray.put(startRowIndex,rowCountInThisSection);
                    if ( startRowIndex == totalRowCount ){ //说明是sectionTitle区域
                        sectionTitlePositionsSet.set(startRowIndex,true);
                    }else{
                        sectionTitlePositionsSet.set(startRowIndex,false);
                    }

                }
                totalRowCount += (rowCountInThisSection + 1);
            }

        }
        return totalRowCount;
    }

    @Override
    public int getSectionIndex(int rowIndex) {
       return rowIndexMapSectionArray.get(rowIndex);
    }

    @Override
    public boolean isSectionTitle(int rowIndex) {
        return sectionTitlePositionsSet.get(rowIndex);
    }

    @Override
    public int getRowIndexInSection(int rowIndex) {
        return rowIndexMapRowIndexInSectionArray.get(rowIndex);
    }

    @Override
    public int getColumnCount() {
        return this.headerCars == null? 0:this.headerCars.size();
    }

    @Override
    public View getCellView(int section, int row, int column, View convertView, ViewGroup parent) {
        TextView tvCellView = null;
        if ( convertView == null ){
            convertView = inflater.inflate(R.layout.table_cell_view,parent,false);
        }
        tvCellView = (TextView) convertView;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("(cell").append(section).append(",").append(row).append(",").append(column).append(")");
            CanPeiRowCell canPeiRowCell = sections.get(section).getItems().get(row).getValues().get(column);
            tvCellView.setText(sb.toString() + canPeiRowCell.getValue());
        }catch (NullPointerException e){}
        return convertView;
    }

    @Override
    public View getCellTitleView(int section, int row, View convertView, ViewGroup parent) {
        TextView tvCellTitleView = null;
        if ( convertView == null ){
            convertView = inflater.inflate(R.layout.table_cell_view,parent,false);
        }
        tvCellTitleView = (TextView) convertView;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("(cellTitle").append(section).append(",").append(row).append(")");
            CanPeiRow canPeiRow = sections.get(section).getItems().get(row);
            tvCellTitleView.setText(sb.toString() + canPeiRow.getName());
        }catch (NullPointerException e){}
        return convertView;
    }

    @Override
    public View getTableHeaderView(int column, View convertView, ViewGroup parent) {
        TextView tvTableView = null;
        if ( convertView == null ){
            convertView = inflater.inflate(R.layout.table_cell_view,parent,false);
        }
        tvTableView = (TextView) convertView;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("(header").append(column).append(")");
            CanPeiCarEntity carEntity = headerCars.get(column);
            tvTableView.setText(sb.toString()+carEntity.getCarName());
        }catch (NullPointerException e){}
        return convertView;
    }

    @Override
    public View getSectionHeaderView(int section, View convertView, ViewGroup parent) {
        TextView tvSectionHeaderView = null;
        if ( convertView == null ){
            convertView = inflater.inflate(R.layout.table_cell_view,parent,false);
        }
        tvSectionHeaderView = (TextView) convertView;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("(").append("section"+section).append(")");
            CanPeiGroup canPeiGroup = sections.get(section);
            tvSectionHeaderView.setText(sb.toString()+canPeiGroup.getName());
        }catch (NullPointerException e){}
        return convertView;
    }

    @Override
    public View getLeftCornerView(View convertView, ViewGroup parent) {
        TextView leftCornerView = (TextView) inflater.inflate(R.layout.table_cell_view,parent,false);
        leftCornerView.setText("left cornerView");
        return leftCornerView;
    }

    @Override
    public int getItemViewType(int section, int row, int column) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return McPropertyDataType.TYPE_TOTAL_COUNT;
    }
}
