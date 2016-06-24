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
import android.widget.RadioButton;
import android.widget.TextView;


import java.util.BitSet;
import java.util.List;

import cn.mucang.android.core.utils.LogUtils;
import cn.mucang.android.core.utils.UnitUtils;
import cn.mucang.property.DuiBiBase;
import cn.mucang.property.McBasePropertiesAdapter;
import cn.mucang.property.McPropertyDataType;
import cn.mucang.property.R;
import cn.mucang.property.data.CanPeiCarEntity;
import cn.mucang.property.data.CanPeiGroup;
import cn.mucang.property.data.CanPeiRow;
import cn.mucang.property.data.CanPeiRowCell;
import cn.mucang.property.data.GetCarPropertiesResultEntity;

/**
 * Created by qinqun on 2016/6/16.
 * Email : qinqun@mucang.cn
 */
public class SerialSpecMcPropertiesAdapter extends McBasePropertiesAdapter {
    public static final String TAG = "SerialSpecMcPropertiesAdapter";

    private GetCarPropertiesResultEntity carPropertiesResultEntity;
    private List<CanPeiGroup> sections;
    private List<CanPeiCarEntity> headerCars;
    private List<CanPeiRowCell> carInfosRowCells;
    private LayoutInflater inflater;

    private SparseArray<Integer> rowIndexMapSectionArray;
    private SparseArray<Integer> rowIndexMapRowIndexInSectionArray;
    private BitSet sectionTitlePositionsSet;
    private int totalSectionCount;
    private int totalRowCount;

//    private int cellWidth = PublicConstant.WIDTH_PIXELS / 3;

    private boolean isForDuibi = false;

    private Context context;

    private McPropertiesClickListener mcPropertiesClickListener;

    private boolean isShowAll = true;

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public SerialSpecMcPropertiesAdapter(Context context, GetCarPropertiesResultEntity getCarPropertiesResultEntity) {
        this.carPropertiesResultEntity = getCarPropertiesResultEntity;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        initData();
    }

    private void initData(){
        if (  this.carPropertiesResultEntity != null ){
            try {
                this.sections = this.carPropertiesResultEntity.getItems();
                this.headerCars = this.carPropertiesResultEntity.getCars();
                this.carInfosRowCells = this.carPropertiesResultEntity.getItems().get(0).getItems().get(0).getValues();
            }catch (NullPointerException e){
                LogUtils.d(TAG,""+e.getMessage());
            }
        }
    }

    public boolean isShowAll() {
        return isShowAll;
    }

    public void setShowAll(boolean showAll) {
        isShowAll = showAll;
    }

    public McPropertiesClickListener getMcPropertiesClickListener() {
        return mcPropertiesClickListener;
    }

    public void setMcPropertiesClickListener(McPropertiesClickListener mcPropertiesClickListener) {
        this.mcPropertiesClickListener = mcPropertiesClickListener;
    }

    public GetCarPropertiesResultEntity getCarPropertiesResultEntity() {
        return carPropertiesResultEntity;
    }

    public void setCarPropertiesResultEntity(GetCarPropertiesResultEntity carPropertiesResultEntity) {
        this.carPropertiesResultEntity = carPropertiesResultEntity;
        initData();
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
            int padding = UnitUtils.dip2px(10);
            CanPeiRow canPeiRow = sections.get(section).getItems().get(row);
            final CanPeiRowCell canPeiRowCell = sections.get(section).getItems().get(row).getValues().get(column);
//            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(cellWidth, ViewGroup.LayoutParams.MATCH_PARENT);
            viewHolder.tvParam.setPadding(padding, padding, padding, padding);
//            convertView.setLayoutParams(layoutParams);
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
                        if (!isForDuibi) {
//                            StatisticsUtil.onEvent(getContext(), "车系参配点击询底价");
                        } else {
//                            StatisticsUtil.onEvent(getContext(), "对比详情点击询底价");
                        }
//                        Intent intent = new Intent(getContext(), XunDiJiaOrYueShiJiaActivity.class);
//                        intent.putExtra("cartypeId", canPeiRowCell.getCarId());
//                        intent.putExtra("orderType", OrderType.GET_PRICE);
//                        if (!isForDuibi) {
//                            intent.putExtra("entrance", "车系参配");
//                        } else {
//                            intent.putExtra("entrance", "对比详情");
//                        }
//                        getContext().startActivity(intent);
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
        CellHeaderViewHolder viewHolder = null;
        if ( convertView == null ){
            viewHolder = new CellHeaderViewHolder();
            if (!isForDuibi) {
                convertView = inflater.inflate(R.layout.bj__cxk_cx_canpei_car_block, null, false);
//                viewHolder.btnDuibi = (DuiBiLinearLayoutButton) convertView.findViewById(R.id.btnDuibi);
//                viewHolder.btnDuibi.setTag(R.id.tag_click_type,McPropertyDataType.CLICK_TYPE_DUIBI);
            } else {
                convertView = inflater.inflate(R.layout.bj__cxk_cx_duibi_car_block, null, false);
                viewHolder.tvChangeCar = (TextView) convertView.findViewById(R.id.tvChangeCar);
                viewHolder.tvChangeCar.setTag(R.id.tag_click_type, McPropertyDataType.CLICK_TYPE_CHANGECAR);
            }
            viewHolder.tvCarTypeName = (TextView) convertView.findViewById(R.id.tvCarTypeName);
            viewHolder.ibtnRemove = (ImageButton) convertView.findViewById(R.id.ibtnRemove);

            viewHolder.tvCarTypeName.setTag(R.id.tag_click_type,McPropertyDataType.CLICK_TYPE_CARNAME);
            viewHolder.ibtnRemove.setTag(R.id.tag_click_type,McPropertyDataType.CLICK_TYPE_REMOVE);
            bindViewColumn(column,viewHolder.tvChangeCar,viewHolder.tvCarTypeName,viewHolder.ibtnRemove);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (CellHeaderViewHolder) convertView.getTag();
        }

        try {
            CanPeiGroup canPeiGroup = sections.get(0);
            final CanPeiRowCell canPeiRowCell = canPeiGroup.getItems().get(0).getValues().get(column);
            CanPeiRowCell priceCanPeiRowCell = canPeiGroup.getItems().get(1).getValues().get(column);
            final String carFullName = canPeiRowCell.getValue();
            final String price = priceCanPeiRowCell.getValue();
            viewHolder.tvCarTypeName.setText(carFullName);
            viewHolder.tvCarTypeName.setOnClickListener(onClickListener);
            viewHolder.ibtnRemove.setOnClickListener(onClickListener);
            if (isForDuibi) { //todo  这里的点击逻辑到底该怎么处理(是移动到 presenter 还是 fragment)
                viewHolder.tvChangeCar.setOnClickListener(onClickListener);
            } else {
//                viewHolder.btnDuibi.setOnClickListener(onClickListener);
            }

        }catch (NullPointerException e){}
        return convertView;
    }

    private void bindViewColumn(int column,View... views){
        if ( views == null ){
            return;
        }
        for (View view:views) {
            if ( view != null ){
                view.setTag(R.id.tag_column,column);
            }
        }
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
        LeftViewHolder leftViewHolder = null;
        if ( convertView == null ){
            leftViewHolder = new LeftViewHolder();
            convertView = inflater.inflate(R.layout.bj__table_leftcorner,parent,false);
            leftViewHolder.btnShowAllParam = (RadioButton) convertView.findViewById(R.id.btnShowAllParam);
            leftViewHolder.btnShowDiffentParam = (RadioButton) convertView.findViewById(R.id.btnShowDiffentParam);
            leftViewHolder.btnShowAllParam.setTag(R.id.tag_click_type,McPropertyDataType.CLICK_TYPE_SHOW_ALL);
            leftViewHolder.btnShowDiffentParam.setTag(R.id.tag_click_type,McPropertyDataType.CLICK_TYPE_SHOW_DIFF);
            bindViewColumn(-1,leftViewHolder.btnShowAllParam,leftViewHolder.btnShowDiffentParam);
            convertView.setTag(leftViewHolder);
        }else {
            leftViewHolder = (LeftViewHolder) convertView.getTag();
        }

        leftViewHolder.btnShowAllParam.setOnClickListener(onClickListener);
        leftViewHolder.btnShowDiffentParam.setOnClickListener(onClickListener);
        return convertView;
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

    @Override
    public void reset() {
        rowIndexMapSectionArray = null;
        rowIndexMapRowIndexInSectionArray = null;
        sectionTitlePositionsSet = null;
        totalSectionCount = 0;
        totalRowCount = 0;
    }

    public interface McPropertiesClickListener extends DuiBiBase.OnDuiBiClickListener{
        void onCarTypeNameClick(int serialId, int carId);
        void onChangeCarClick(String brandId, int serialId, int oldCarId);
        void onAddDuiBi(View v);
        void onRemoveDuiBi(View v);
        void onShowAllOrDiff(boolean isShowAll);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int clickType = (Integer) v.getTag(R.id.tag_click_type);
            int column = (Integer) v.getTag(R.id.tag_column);

            if ( clickType == McPropertyDataType.CLICK_TYPE_CARNAME ){
                if ( mcPropertiesClickListener != null ){
                    final CanPeiCarEntity canPeiCarEntity = headerCars.get(column);
                    final CanPeiRowCell canPeiRowCell = carInfosRowCells.get(column);
                    mcPropertiesClickListener.onCarTypeNameClick(canPeiCarEntity.getSerialId(),canPeiRowCell.getCarId());
                }
            }else if ( clickType == McPropertyDataType.CLICK_TYPE_CHANGECAR ){
                if ( mcPropertiesClickListener != null ){
                    final CanPeiCarEntity canPeiCarEntity = headerCars.get(column);
                    final CanPeiRowCell canPeiRowCell = carInfosRowCells.get(column);
                    mcPropertiesClickListener.onChangeCarClick(canPeiCarEntity.getBrandId(),canPeiCarEntity.getSerialId(),canPeiRowCell.getCarId());
                }
            }else if ( clickType == McPropertyDataType.CLICK_TYPE_DUIBI ){
                //todo:对比的处理暂时不做
            }else if ( clickType == McPropertyDataType.CLICK_TYPE_REMOVE ){
                removeByColumn(column);
            }else if ( clickType == McPropertyDataType.CLICK_TYPE_SHOW_ALL ){
                if ( mcPropertiesClickListener != null ){
                    RadioButton radioButton = (RadioButton) v;
                    radioButton.toggle();
                    mcPropertiesClickListener.onShowAllOrDiff(true);
                }
            }else if ( clickType == McPropertyDataType.CLICK_TYPE_SHOW_DIFF ){
                if ( mcPropertiesClickListener != null ){
                    RadioButton radioButton = (RadioButton) v;
                    radioButton.toggle();
                    mcPropertiesClickListener.onShowAllOrDiff(false);
                }
            }
        }
    };

    private void removeByColumn(int column){
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

        reset();
        notifyDataSetChanged();
    }

    static class LeftViewHolder {
        RadioButton btnShowAllParam, btnShowDiffentParam;
    }

    static class CellViewHolder {
        TextView tvParam;
        TextView tvGetRealPrice;
    }

    static class CellHeaderViewHolder{
        TextView tvCarTypeName;
        TextView tvChangeCar;
        ImageButton ibtnRemove;
//        DuiBiLinearLayoutButton btnDuibi;
    }


}
