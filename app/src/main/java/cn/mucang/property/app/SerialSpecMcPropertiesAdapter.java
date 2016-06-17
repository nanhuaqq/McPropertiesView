package cn.mucang.property.app;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.BitSet;
import java.util.List;

import cn.mucang.android.core.config.MucangConfig;
import cn.mucang.android.core.utils.UnitUtils;
import cn.mucang.property.McBasePropertiesAdapter;
import cn.mucang.property.McPropertyDataType;
import cn.mucang.property.R;
import cn.mucang.property.data.CanPeiCarEntity;
import cn.mucang.property.data.CanPeiGroup;
import cn.mucang.property.data.CanPeiRow;
import cn.mucang.property.data.CanPeiRowCell;
import cn.mucang.property.data.GetCarPropertiesResultEntity;
import cn.mucang.property.utils.DimUtils;

/**
 * Created by qinqun on 2016/6/16.
 * Email : qinqun@mucang.cn
 */
public class SerialSpecMcPropertiesAdapter extends McBasePropertiesAdapter {
    private GetCarPropertiesResultEntity getCarPropertiesResultEntity;
    private List<CanPeiGroup> sections;
    private List<CanPeiCarEntity> headerCars;
    private LayoutInflater inflater;

    private SparseArray<Integer> rowIndexMapSectionArray;
    private SparseArray<Integer> rowIndexMapRowIndexInSectionArray;
    private BitSet sectionTitlePositionsSet;
    private int totalSectionCount;
    private int totalRowCount;

    private int cellWidth ;

    private boolean isForDuibi = false;

    private Context context;

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public SerialSpecMcPropertiesAdapter(Context context, GetCarPropertiesResultEntity getCarPropertiesResultEntity) {
        this.getCarPropertiesResultEntity = getCarPropertiesResultEntity;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        if (  this.getCarPropertiesResultEntity != null ){
            this.sections = this.getCarPropertiesResultEntity.getItems();
            this.headerCars = this.getCarPropertiesResultEntity.getCars();
        }
        float scale = context.getResources().getDisplayMetrics().density;
        cellWidth =  (int)(120 * scale + 0.5F);
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
            rowIndexMapRowIndexInSectionArray.put(0,0);
            int sectionCount = getSectionCount();
            for ( int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++ ){
                int rowCountInThisSection = getRowCount(sectionIndex);
                int endRowIndex = totalRowCount + (rowCountInThisSection + 1);
                int rowIndexInSection = -1;
                for ( int startRowIndex = totalRowCount; startRowIndex < endRowIndex; startRowIndex++,rowIndexInSection ++ ){
                    rowIndexMapSectionArray.put(startRowIndex,sectionIndex);
                    rowIndexMapRowIndexInSectionArray.put(startRowIndex,rowIndexInSection);
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
        CellViewHolder viewHolder = null;
        if ( convertView == null ){
            convertView = inflater.inflate(R.layout.bj__table_cell_,parent,false);
            viewHolder = new CellViewHolder();
            viewHolder.tvParam = (TextView) convertView.findViewById(R.id.tvParam);
            viewHolder.tvGetRealPrice = (TextView) convertView.findViewById(R.id.tvGetRealPrice);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (CellViewHolder) convertView.getTag();
        }
        try {
            int padding = DimUtils.dip2px(getContext(),10);
            CanPeiRow canPeiRow = sections.get(section).getItems().get(row);
            final CanPeiRowCell canPeiRowCell = sections.get(section).getItems().get(row).getValues().get(column);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(cellWidth, ViewGroup.LayoutParams.MATCH_PARENT);
            viewHolder.tvParam.setPadding(padding, padding, padding, padding);
            convertView.setLayoutParams(layoutParams);
            viewHolder.tvParam.setGravity(Gravity.CENTER);
            //tvParam.setMaxLines(2);
            viewHolder.tvParam.setText(canPeiRowCell.getValue());

            if (canPeiRow.isDifferent()) {
                viewHolder.tvParam.setTextColor(getContext().getResources().getColor(R.color.main_color));
            } else {
                viewHolder.tvParam.setTextColor(getContext().getResources().getColor(R.color.bj_black));
            }
            viewHolder.tvParam.getPaint().setFakeBoldText(canPeiRowCell.isBold());
            if (canPeiRowCell.isBold()) {
                viewHolder.tvParam.setTextSize(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimensionPixelSize(R.dimen.cxk_cx_canpei_main_text_size_big));
            } else {
                viewHolder.tvParam.setTextSize(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimensionPixelSize(R.dimen.cxk_cx_canpei_main_text_size));
            }

            viewHolder.tvGetRealPrice.setVisibility(View.GONE);
            viewHolder.tvGetRealPrice.setOnClickListener(null);
            convertView.setOnClickListener(null);

            if (canPeiRow.getId() == 1025 && !TextUtils.isEmpty(canPeiRowCell.getValue())) {
                viewHolder.tvGetRealPrice.setVisibility(View.VISIBLE);
                viewHolder.tvGetRealPrice.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
            } else {
                viewHolder.tvGetRealPrice.setVisibility(View.GONE);
                viewHolder.tvGetRealPrice.setOnClickListener(null);
            }
        } catch (NullPointerException e) {
        }
        return convertView;
    }

    @Override
    public View getCellTitleView(int section, int row, View convertView, ViewGroup parent) {
        TextView tvCellTitleView = null;
        if ( convertView == null ){
            convertView = inflater.inflate(R.layout.bj__table_cell_title_view,parent,false);
        }
        tvCellTitleView = (TextView) convertView;
        try {
            CanPeiRow canPeiRow = sections.get(section).getItems().get(row);
            tvCellTitleView.setText(canPeiRow.getName());
        }catch (NullPointerException e){}
        return convertView;
    }

    @Override
    public View getTableHeaderView(int column, View convertView, ViewGroup parent) {
        TextView tvCarTypeName;
        TextView tvChangeCar;
        ImageButton ibtnRemove;
//        DuiBiLinearLayoutButton btnDuibi;
        if ( convertView == null ){
            if (!isForDuibi) {
                convertView = inflater.inflate(R.layout.bj__cxk_cx_canpei_car_block, null, false);
            } else {
                convertView = inflater.inflate(R.layout.bj__cxk_cx_duibi_car_block, null, false);
            }
        }

        final CanPeiCarEntity canPeiCarEntity = headerCars.get(column);
        try {
            CanPeiGroup canPeiGroup = sections.get(0);
            final CanPeiRowCell canPeiRowCell = canPeiGroup.getItems().get(0).getValues().get(column);
            CanPeiRowCell priceCanPeiRowCell = canPeiGroup.getItems().get(1).getValues().get(column);
            final String carFullName = canPeiRowCell.getValue();
            final String price = priceCanPeiRowCell.getValue();
            tvCarTypeName = (TextView) convertView.findViewById(R.id.tvCarTypeName);
            tvCarTypeName.setText(carFullName);

            if (isForDuibi) { //todo  这里的点击逻辑到底该怎么处理(是移动到 presenter 还是 fragment)

            } else {

            }
            ibtnRemove = (ImageButton) convertView.findViewById(R.id.ibtnRemove);
            ibtnRemove.setTag(canPeiRowCell.getCarId());
            ibtnRemove.setOnClickListener(onCarDeleteListener);

        }catch (NullPointerException e){}
        return convertView;
    }

    @Override
    public View getSectionHeaderView(int section, View convertView, ViewGroup parent) {
        if ( convertView == null ){
            convertView = inflater.inflate(R.layout.bj__table_section_title,parent,false);
        }
        TextView tvGroupName = ((TextView) convertView.findViewById(R.id.tvGroupName));
        try {
            CanPeiGroup canPeiGroup = sections.get(section);
            tvGroupName.setText(canPeiGroup.getName());
        }catch (NullPointerException e){}
        return convertView;
    }

    @Override
    public View getLeftCornerView(View convertView, ViewGroup parent) {
        View leftCornerView = inflater.inflate(R.layout.bj__table_leftcorner,parent,false);
        return leftCornerView;
    }

    @Override
    public View getExtraTableHeaderView(int column, View convertView, ViewGroup parent) {
        if ( convertView == null ){
            convertView = inflater.inflate(R.layout.bj__cxk_cx_duibi_car_add_new,parent,false);
        }
        return convertView;
    }

    @Override
    public View getExtraCellView(int section, int row, int column, View convertView, ViewGroup parent) {
        if ( convertView == null ){
            convertView = inflater.inflate(R.layout.bj__table_extra_cell_view,parent,false);
        }
        return convertView;
    }

    @Override
    public int getItemViewType(int section, int row, int column) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return McPropertyDataType.TYPE_TOTAL_COUNT;
    }

    private View.OnClickListener onCarDeleteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View parentView = (View) v.getParent();
            int viewType = (Integer)parentView.getTag(R.id.tag_view_type);
            int column = (Integer)parentView.getTag(R.id.tag_column);

            if ( column >= headerCars.size() ){
                return;
            }
            //首先删除carHeader
            headerCars.remove(column);
            for (CanPeiGroup canpeiGroup:sections) {
                for (CanPeiRow canpeiRow:canpeiGroup.getItems()) {
                    canpeiRow.getValues().remove(column);
                }
            }

            rowIndexMapSectionArray = null;
            rowIndexMapRowIndexInSectionArray = null;
            sectionTitlePositionsSet = null;
            totalSectionCount = 0;
            totalRowCount = 0;
            notifyDataSetChanged();
        }
    };

    static class CellViewHolder {
        TextView tvParam;
        TextView tvGetRealPrice;
    }
}
