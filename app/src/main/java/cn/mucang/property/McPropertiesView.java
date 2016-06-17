package cn.mucang.property;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.IllegalFormatCodePointException;
import java.util.List;

import cn.mucang.android.core.config.MucangConfig;
import cn.mucang.android.core.utils.UnitUtils;

/**
 * Created by qinqun on 2016/6/16.
 * Email : qinqun@mucang.cn
 */
public class McPropertiesView extends ViewGroup{

    View leftCornerView;

    private int currentX;
    private int currentY;

    //区域    //列    //行 组成坐标（0,0,0）（1,0,0）
    private int firstColumn;
    private int firstRow;
    private int lastRow;
    private List<View> headerViews;
    private List<View> cellTitleViews;
    private List<View> sectionTitleViews;
    private List<List<View>> cellViews;

    /**
     * 控件的宽度 （layout）
     */
    private int width;
    /**
     * 控件的高度 (layout)
     */
    private int height;

    private McPropertiesAdapter adapter;

    private int scrollX;
    private int scrollY;
    private int maxScrollX;
    private int maxScrollY;
    private int deltaX;
    private int deltaY;
    private int firstCellLeft = 0;
    private int lastCellRight = 0;
    private int firstCellTop = 0;
    private int lastCellBottom = 0;

    private boolean needRelayout;

    private McPropertiesChildrenRecycler recycler;
    private McPropertiesAdapterDataSetObserver observer;


    private final int minimumVelocity;
    private final int maximumVelocity;
    private final Flinger flinger;
    private VelocityTracker velocityTracker;
    private int touchSlop;

    //单元格的宽度
    private int cellWidth;
    //分隔线的 粗细
    private int dividerSize = 1;

    /**
     * 真实的行数目 = 1（顶部） + section数目 + 多个section中行数和
     */
    int realRowCount;

    int realColumnCount;
    /**
     * 每一行的高度
     */
    int [] rowHeights;
    /**
     * rowHeights赋值指针
     */
    int rowHeightsAsignIndex = 0;
    /**
     * 是否通过measure得到每行的高度
     */
    private boolean isRowHeightsInited = false;

    /**
     * sticky借鉴stickyScroll的做法 一个currentStickView一个approachingView
     */
    private View currentHeader;
    private float headerOffset;

    /********************************* 自定义属性 *****************************************/
    private boolean isSupportExtraHeader = false;
    private boolean isDividerEnable = true;

    public McPropertiesView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.needRelayout = true;
        this.flinger = new Flinger(context);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        this.touchSlop = configuration.getScaledTouchSlop();
        this.minimumVelocity = configuration.getScaledMinimumFlingVelocity();
        this.maximumVelocity = configuration.getScaledMaximumFlingVelocity();

        headerViews = new ArrayList<>();
        sectionTitleViews = new ArrayList<>();
        cellTitleViews = new ArrayList<>();
        cellViews = new ArrayList<>();

        init(attrs);

        this.setWillNotDraw(false);
    }

    private void init(AttributeSet attr){
        if ( attr != null ){
            TypedArray a = getContext().obtainStyledAttributes(attr,R.styleable.McPropertiesView);
            if ( a!= null ){
                isSupportExtraHeader = a.getBoolean(R.styleable.McPropertiesView_isSupportExtraHeader,false);
                isDividerEnable = a.getBoolean(R.styleable.McPropertiesView_isDividerEnable,true);
                dividerSize = (int) a.getDimension(R.styleable.McPropertiesView_pvDividerSize,2f);
                a.recycle();
            }
        }
    }

    public McPropertiesAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(McPropertiesAdapter adapter) {
        if ( this.adapter != null ){
            this.adapter.unregisterDataSetObserver(observer);
        }
        this.adapter = adapter;
        observer = new McPropertiesAdapterDataSetObserver();
        this.adapter.registerDataSetObserver(observer);

        this.recycler = new McPropertiesChildrenRecycler(adapter.getViewTypeCount());
        float scale = getContext().getResources().getDisplayMetrics().density;
        this.cellWidth = (int)(120 * scale + 0.5F);

        scrollX = 0;
        scrollY = 0;
        maxScrollX = 0;
        maxScrollY = 0;
        firstColumn = 0;
        firstRow = 1;

        realRowCount = adapter.getTotalRowCount();
        realColumnCount = adapter.getColumnCount();
        if ( isSupportExtraHeader ){
            realColumnCount += 1;
        }
        rowHeightsAsignIndex = 0;

        if ( rowHeights != null ){
            rowHeights = null;
        }
        rowHeights = new int[realRowCount];

        needRelayout = true;
        requestLayout();
    }

    /**
     * 为 rowHeights赋值
     * @param height
     */
    private void asignRowHeights(int height){
        if ( rowHeights == null ){ //rowHeights未初始化
            return;
        }
        if ( rowHeightsAsignIndex >= realRowCount ){ //越界保护
            return;
        }
        rowHeights[rowHeightsAsignIndex] = height;
        rowHeightsAsignIndex++;
    }

    /**
     * 第一次需要 计算出 每一行的高度
     */
    private void measureRowHeights(){
        if ( isRowHeightsInited ){
            return;
        }
        isRowHeightsInited = true;
        //首先measure header
        int headerHeight = 0;
        for (int i = -1; i < realColumnCount; i++) {

            if ( i == -1 ){  // leftCornerView
                leftCornerView = adapter.getLeftCornerView(recycler.getRecycledView(McPropertyDataType.TYPE_SHOW_ALL_OR_DIFF),this);
                recycler.addRecycledView(leftCornerView,McPropertyDataType.TYPE_SHOW_ALL_OR_DIFF);
                headerHeight = Math.max(headerHeight,onlyMeasureChild(leftCornerView));
            }else{   // tableTitleView
                if ( isSupportExtraHeader && i == realColumnCount -1 ){ //如果支持 额外的 header （最后一个）
                    final View tableExtraHeaderView = adapter.getExtraTableHeaderView(i,recycler.getRecycledView(McPropertyDataType.TYPE_CAR_HEADER_EXTRA),this);
                    recycler.addRecycledView(tableExtraHeaderView,McPropertyDataType.TYPE_CAR_HEADER_EXTRA);
                    headerHeight = Math.max(headerHeight,onlyMeasureChild(tableExtraHeaderView));
                }else {
                    final View tableHeaderView = adapter.getTableHeaderView(i,recycler.getRecycledView(McPropertyDataType.TYPE_CAR_HEADER),this);
                    recycler.addRecycledView(tableHeaderView,McPropertyDataType.TYPE_CAR_HEADER);
                    headerHeight = Math.max(headerHeight,onlyMeasureChild(tableHeaderView));
                }
            }

        }
        asignRowHeights(headerHeight);

        //随后分section measure
        for (int i = 0; i < adapter.getSectionCount() ; i++) {
            //首先measure sectionTitle
            final View sectionTitleView = adapter.getSectionHeaderView(i,recycler.getRecycledView(McPropertyDataType.TYPE_GROUP_TITLE),this);
            int sectionViewHeight = 0;
            sectionTitleView.measure(MeasureSpec.EXACTLY | width,MeasureSpec.UNSPECIFIED);
            sectionViewHeight = sectionTitleView.getMeasuredHeight();
            recycler.addRecycledView(sectionTitleView,McPropertyDataType.TYPE_GROUP_TITLE);
            asignRowHeights(sectionViewHeight);

            // 随后测量 section中的每一行
            for (int j = 0; j < adapter.getRowCount(i); j++){
                int cellHeight = 0;
                for ( int k = -1; k < realColumnCount; k++ ){
                    if ( k == -1 ){ //measure cellTitleView(-1)
                        final View cellTitleView = adapter.getCellTitleView(i,j,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_TITLE),this);
                        cellHeight = Math.max( cellHeight,onlyMeasureChild(cellTitleView));
                        recycler.addRecycledView(cellTitleView,McPropertyDataType.TYPE_PROPERTY_TITLE);
                    }else{
                        //measure cellView(从0开始)
                        if ( isSupportExtraHeader && k == realColumnCount - 1 ){ //如果支持 额外header（最后一个）
                            final View extraCellView  = adapter.getExtraCellView(i,j,k,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_CELL_EXTRA),this);
                            cellHeight = Math.max(cellHeight,onlyMeasureChild(extraCellView));
                            recycler.addRecycledView(extraCellView,McPropertyDataType.TYPE_PROPERTY_CELL_EXTRA);
                        }else{
                            final View cellView = adapter.getCellView(i,j,k,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_CELL),this);
                            cellHeight = Math.max(cellHeight,onlyMeasureChild(cellView));
                            recycler.addRecycledView(cellView,McPropertyDataType.TYPE_PROPERTY_CELL);
                        }
                    }
                }
                asignRowHeights(cellHeight);
            }
        }
    }

    /**
     * 只测量child
     * @param child
     */
    private int onlyMeasureChild(final View child) {
        LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
        child.setDrawingCacheEnabled(true);
//        addViewInLayout(child, 0, params, true);

        child.measure(MeasureSpec.EXACTLY | cellWidth, MeasureSpec.UNSPECIFIED);
        return child.getMeasuredHeight();
    }

    private int exactlyMeasureChild(final View child,int height){
        child.setDrawingCacheEnabled(true);
        child.measure(MeasureSpec.EXACTLY | cellWidth, MeasureSpec.EXACTLY | height);
        return child.getMeasuredHeight();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean intercept = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                currentX = (int) event.getRawX();
                currentY = (int) event.getRawY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int x2 = Math.abs(currentX - (int) event.getRawX());
                int y2 = Math.abs(currentY - (int) event.getRawY());
                if (x2 > touchSlop || y2 > touchSlop) {
                    intercept = true;
                }
                break;
            }
        }
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (velocityTracker == null) { // If we do not have velocity tracker
            velocityTracker = VelocityTracker.obtain(); // then get one
        }
        velocityTracker.addMovement(event); // add this movement to it

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (!flinger.isFinished()) { // If scrolling, then stop now
                    flinger.forceFinished();
                }
                currentX = (int) event.getRawX();
                currentY = (int) event.getRawY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int x2 = (int) event.getRawX();
                final int y2 = (int) event.getRawY();
                final int diffX = currentX - x2;
                final int diffY = currentY - y2;
                currentX = x2;
                currentY = y2;

                scrollBy(diffX, diffY);
                break;
            }
            case MotionEvent.ACTION_UP: {
                final VelocityTracker velocityTracker = this.velocityTracker;
                velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
                int velocityX = (int) velocityTracker.getXVelocity();
                int velocityY = (int) velocityTracker.getYVelocity();

                if (Math.abs(velocityX) > minimumVelocity || Math.abs(velocityY) > minimumVelocity) {
                    flinger.start(getActualScrollX(), getActualScrollY(), velocityX, velocityY, getMaxScrollX(), getMaxScrollY());
                } else {
                    if (this.velocityTracker != null) { // If the velocity less than threshold
                        this.velocityTracker.recycle(); // recycle the tracker
                        this.velocityTracker = null;
                    }
                }
                break;
            }
        }
        return true;
    }

    private void correctDeltaXAndScrollX(){
        int sumScrollX = scrollX + deltaX;
        if ( sumScrollX > maxScrollX ){
            deltaX = deltaX - (sumScrollX - maxScrollX);
            scrollX = maxScrollX;
        }else {
            scrollX += deltaX;
        }
    }

    private void correctDeltaYAndScrollY(){
        int sumScrollY = scrollY + deltaY;
        if ( sumScrollY > maxScrollY ){
            deltaY = deltaY - (sumScrollY - maxScrollY);
            scrollY = maxScrollY;
        }else {
            scrollY += deltaY;
        }
    }

    public int getActualScrollX() {
        return scrollX;
    }

    public int getActualScrollY() {
        return scrollY;
    }


    private int getMaxScrollX() {
        return maxScrollX;
    }

    private int getMaxScrollY() {
        return maxScrollY;
    }

    /**
     * 计算 X轴 最大 滑动距离
     * @return
     */
    private int calculateMaxScrollX(){
        int sumCellWidth = cellWidth * adapter.getColumnCount();
        int sumDividerWidth = dividerSize * ( adapter.getColumnCount() -1 );
        int sumScreenWidth = width - cellWidth;
        int maxScrollX = sumCellWidth + sumDividerWidth - sumScreenWidth;
        return maxScrollX;
    }

    /**
     * 计算 Y轴 最大的 滑动距离
     * @return
     */
    private int calculateMaxScrollY(){
        int sumCellAndSectionTitleHeight = getArraySum(rowHeights,1,rowHeights.length);
        int sumDividerHeight = dividerSize * ( adapter.getTotalRowCount() - 2 );
        int sumScreenHeight = height - rowHeights[0];
        int maxScrollY = sumCellAndSectionTitleHeight + sumDividerHeight - sumScreenHeight;
        return maxScrollY;
    }


    @Override
    public void scrollBy(int x, int y) {
        positionViews(x,y);
        awakenScrollBars();
    }

    private void positionViews(int deltaX,int deltaY){
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        correctDeltaXAndScrollX();
        correctDeltaYAndScrollY();
        int left = cellWidth,top = 0,right = 0,bottom = 0;
        //todo 算法改变
        //leftCornerView
        if ( leftCornerView == null ){
            leftCornerView = adapter.getLeftCornerView(recycler.getRecycledView(McPropertyDataType.TYPE_SHOW_ALL_OR_DIFF),this);
        }
        if ( leftCornerView.getParent() != null ){
            this.removeView(leftCornerView);
        }
        addView(leftCornerView,0);
        exactlyMeasureChild(leftCornerView,rowHeights[0]);
        leftCornerView.layout(0,0,cellWidth,rowHeights[0]);
        bindViewTags(leftCornerView,McPropertyDataType.TYPE_SHOW_ALL_OR_DIFF,-1,0,0);

        //layoutHeader 首先layout已经添加到视图层次中的view
        left = firstCellLeft - deltaX;
        for ( View headerView : headerViews ){
            right = left + cellWidth;
            exactlyMeasureChild(headerView,rowHeights[0]);
            headerView.layout(left,0,right,rowHeights[0]);
            left = right + dividerSize;
        }

        //layout sectionTitle cellTitle cell
        top = firstCellTop - scrollY;
        int rowCount = cellTitleViews.size() + sectionTitleViews.size();
        int sectionPosition = 0;
        int cellPosition = 0;
        for ( int rowIndexx = 0; rowIndexx < rowCount; rowIndexx++ ){
            int realRowIndex = firstRow + rowIndexx;
            int sectionIndex = adapter.getSectionIndex(realRowIndex);
            if ( sectionIndex == -1 ){ //说明是 tableHeader
                continue;
            }
            bottom = top + rowHeights[realRowIndex];
            if ( adapter.isSectionTitle(realRowIndex) ){ //如果是sectionTitle
                if ( sectionTitleViews.isEmpty() || sectionPosition >= sectionTitleViews.size() ){
                    continue;
                }
                View sectionTitleView = sectionTitleViews.get(sectionPosition);
                sectionTitleView.measure(MeasureSpec.EXACTLY | width,rowHeights[rowIndexx]);
                sectionTitleView.layout(0,top,width,bottom);
                sectionPosition++;
            }else{ //cellTitle
                if ( cellPosition >= cellTitleViews.size() ){
                    continue;
                }
                View cellTitleView = cellTitleViews.get(cellPosition);
                exactlyMeasureChild(cellTitleView,rowHeights[realRowIndex]);
                cellTitleView.layout(0,top,cellWidth,bottom);

                // layout cellView
                left = firstCellLeft + deltaX;
                List<View> viewList = cellViews.get(cellPosition);
                for ( View cellView : viewList ){
                    right = left + cellWidth;
                    exactlyMeasureChild(cellView,rowHeights[realRowIndex]);
                    cellView.layout(left,top,right,bottom);
                    left = right + dividerSize;
                }
                cellPosition ++;
            }
            top = bottom + dividerSize;
        }

        //重新计算 firstCellLeft lastCellRight firstCellTop lastCellBottom;
        firstCellLeft -= deltaX;
        firstCellTop -= deltaY;
        lastCellRight = right;
        lastCellBottom = bottom;

        //根据边界 添加view
        while ( firstCellLeft > cellWidth ){ //从左边添加
            addLeft();
        }

        while ( lastCellRight < width ){ //从右边添加
            addRight();
        }

        while ( firstCellTop > rowHeights[0] ){ // 从上面添加
            addTop();
        }

        while ( lastCellBottom < height ){ //从下面添加
            addBottom();
        }

        //根据边界 回收view
        while ( firstCellTop < -rowHeights[firstRow] ){ //从上面移除
            removeTop();
        }

        while ( lastCellBottom - rowHeights[lastRow] > height ){ //从下面移除
            removeBottom();
        }

        while ( firstCellLeft <= 0 ){ // 从左边移除
            removeLeft();
        }

        while ( lastCellRight > width + cellWidth ){ //从右边移除
            removeRight();
        }

        invalidate();
    }

    public static final int UNKONW_INDEX = -100;
    private int currentSection = 0;
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if ( adapter == null ){
            return;
        }
        int sectionIndex = adapter.getSectionIndex(firstRow);
        if ( sectionIndex == -1 ){
            sectionIndex = 0;
        }
        currentHeader = adapter.getSectionHeaderView(sectionIndex,recycler.getRecycledView(McPropertyDataType.TYPE_GROUP_TITLE),this);
        bindViewTags(currentHeader,McPropertyDataType.TYPE_GROUP_TITLE,sectionIndex,UNKONW_INDEX,0);

        if (adapter == null || currentHeader == null)
            return;


//        currentHeader.setDrawingCacheEnabled(true);
        currentHeader.measure(MeasureSpec.EXACTLY | width,MeasureSpec.UNSPECIFIED);
        currentHeader.layout(0,0,width,currentHeader.getMeasuredHeight());

        int saveCount = canvas.save();
        canvas.translate(0, rowHeights[0]);
        canvas.clipRect(0, 0, getWidth(), currentHeader.getMeasuredHeight()); // needed
        // for
        // <
        // HONEYCOMB
        currentHeader.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    private void removeLeftOrRight(int position) {
        if ( position < headerViews.size() ){
            removeView(headerViews.remove(position));
        }
        for (List<View> list : cellViews) {
            if ( position < list.size() ){
                removeView(list.remove(position));
            }
        }
    }

    private void removeCellTitleAndCellTopOrBottom(int position) {
        removeView(cellTitleViews.remove(position));
        List<View> remove = cellViews.remove(position);
        for (View view : remove) {
            removeView(view);
        }
    }

    private void removeSectionTitleTopOrBottom(int position){
        if ( position >= sectionTitleViews.size() ){
            return;
        }
        removeView(sectionTitleViews.remove(position));
    }

    /**
     * 移除顶部的views
     * 分俩种情况讨论（1 sectionTitle 2 cellView）
     */
    private void removeTop(){
        if ( adapter.isSectionTitle(firstRow) ){
            removeSectionTitleTopOrBottom(0);
        }else{
            removeCellTitleAndCellTopOrBottom(0);
        }
        firstCellTop = firstCellTop - rowHeights[firstRow] - dividerSize;
        firstRow ++;
    }

    /**
     * 移除底部views
     * 分俩种情况讨论 （1 sectionTitle 2 cellView）
     */
    private void removeBottom(){
        lastRow = firstRow + cellTitleViews.size() + sectionTitleViews.size();
        if ( adapter.isSectionTitle(lastRow) ){ // 如果是sectionTitle
            if ( sectionTitleViews.isEmpty() ){
                return;
            }
            removeSectionTitleTopOrBottom(sectionTitleViews.size()-1);
        }else{
            if ( cellTitleViews.isEmpty() ){
                return;
            }
            removeCellTitleAndCellTopOrBottom(cellTitleViews.size()-1);
        }

        lastCellBottom = lastCellBottom - rowHeights[lastRow] - dividerSize;
        lastRow --;
    }

    /**
     * 移除右边的views
     */
    private void removeRight(){
        removeLeftOrRight(headerViews.size()-1);
        lastCellRight = lastCellRight - cellWidth - dividerSize;
    }

    /**
     * 移除左边的views
     */
    private void removeLeft(){
        removeLeftOrRight(0);
        firstCellLeft = firstCellLeft + cellWidth + dividerSize;
    }

    /**
     * 从右边添加views
     */
    private void addRight(){
        final int size = headerViews.size();
        addLeftOrRight(firstColumn+size,size,DIRECTION_RIGHT);
        addHeaderLeftOrRight(firstColumn+size,size,DIRECTION_RIGHT);
        lastCellRight = lastCellRight + cellWidth + dividerSize;
    }

    /**
     * 从左边添加views
     */
    private void addLeft(){
        firstColumn--;
        addLeftOrRight(firstColumn,0,DIRECTION_LEFT);
        addHeaderLeftOrRight(firstColumn,0,DIRECTION_LEFT);
        firstCellLeft = firstCellLeft - cellWidth - dividerSize;
    }

    private void addHeaderLeftOrRight(int column,int index,int direction){
        View headerView = null;

        int temLeft = 0 ;
        if ( direction ==  DIRECTION_LEFT ){
            temLeft = firstCellLeft - cellWidth - dividerSize;
        }else if ( direction == DIRECTION_RIGHT ){
            temLeft = lastCellRight + dividerSize;
        }

        if ( isSupportExtraHeader && column == adapter.getColumnCount()  ){
            headerView = adapter.getExtraTableHeaderView(column,recycler.getRecycledView(McPropertyDataType.TYPE_CAR_HEADER_EXTRA),this);
            bindViewTags(headerView,McPropertyDataType.TYPE_CAR_HEADER_EXTRA,-1,0,column);
        }else{
            headerView = adapter.getTableHeaderView(column,recycler.getRecycledView(McPropertyDataType.TYPE_CAR_HEADER),this);
            bindViewTags(headerView,McPropertyDataType.TYPE_CAR_HEADER,-1,0,column);
        }
        addView(headerView);
        exactlyMeasureChild(headerView,rowHeights[0]);
        headerView.layout(temLeft,0,temLeft+cellWidth,+rowHeights[0]);
        headerViews.add(index,headerView);
    }

    private void addLeftOrRight(int column,int index,int direction){
        int addRowCount = cellTitleViews.size() + sectionTitleViews.size();
        int realRowIndex;
        int cellPostion = 0;
        int tempLeft = 0;
        int tempBottom;
        if ( direction == DIRECTION_LEFT ){
            tempLeft = firstCellLeft - cellWidth - dividerSize;
        }else if ( direction == DIRECTION_RIGHT ){
            tempLeft = lastCellRight + dividerSize;
        }
        int tempRight = tempLeft + cellWidth;
        int tempTop = firstCellTop;
        for ( int rowIndex = 0; rowIndex <  addRowCount; rowIndex++ ){
            realRowIndex = firstRow + rowIndex;
            int currentSection = adapter.getSectionIndex(realRowIndex);
            if ( currentSection == -1 ){
                continue;
            }
            tempBottom = tempTop + rowHeights[realRowIndex];
            if ( adapter.isSectionTitle(realRowIndex) ){ //如果是sectionTitle 不做处理

            }else{ //cellView
                if ( cellPostion >= cellViews.size() ){
                    continue;
                }
                int rowIndexInSection = adapter.getRowIndexInSection(realRowIndex);
                List<View> viewList = cellViews.get(cellPostion);
                View cellOrExtraView = null;
                if ( isSupportExtraHeader && column == adapter.getColumnCount() ){
                    cellOrExtraView = adapter.getExtraCellView(currentSection,rowIndexInSection,column,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_CELL_EXTRA),this);
                    bindViewTags(cellOrExtraView,McPropertyDataType.TYPE_PROPERTY_CELL_EXTRA,currentSection,realRowIndex,column);
                }else {
                    cellOrExtraView =  adapter.getCellView(currentSection,rowIndexInSection,column,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_CELL),this);
                    bindViewTags(cellOrExtraView,McPropertyDataType.TYPE_PROPERTY_CELL,currentSection,realRowIndex,column);
                }
                addView(cellOrExtraView,0);
                exactlyMeasureChild(cellOrExtraView,rowHeights[realRowIndex]);
                cellOrExtraView.layout(tempLeft,tempTop,tempRight,tempBottom);

                viewList.add(index,cellOrExtraView);
                cellPostion++;
            }

            tempTop = tempBottom + dividerSize;
        }
    }

    private void addCellTitleTopAndBottom(int rowIndex,int index,int direction){
        int tempTop = 0;
        if ( direction == DIRECTION_BOTTOM ){
            tempTop = lastCellBottom + dividerSize;
        }else if ( direction == DIRECTION_TOP ){
            tempTop = firstCellTop - rowHeights[rowIndex] - dividerSize;
        }
        int sectionIndex = adapter.getSectionIndex(rowIndex);
        int rowIndexInSection = adapter.getRowIndexInSection(rowIndex);
        View cellTitleView = adapter.getCellTitleView(sectionIndex,rowIndexInSection,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_TITLE),this);
        addView(cellTitleView);
        exactlyMeasureChild(cellTitleView,rowHeights[rowIndex]);
        cellTitleView.layout(0,tempTop,cellWidth,tempTop+rowHeights[rowIndex]);
        cellTitleViews.add(index,cellTitleView);
        bindViewTags(cellTitleView,McPropertyDataType.TYPE_PROPERTY_TITLE,sectionIndex,rowIndex,-1);
    }

    private void addCellTopAndBottom(int rowIndex, int index,int direction) {
        int sectionIndex = adapter.getSectionIndex(rowIndex);
        int rowIndexInSection = adapter.getRowIndexInSection(rowIndex);
        int columnCount = headerViews.size();
        int realColumnIndex;

        int tempTop = 0;
        int tempBottom = 0;
        if ( direction == DIRECTION_BOTTOM ){
            tempTop = lastCellBottom + dividerSize;
        }else if ( direction == DIRECTION_TOP ){
            tempTop = firstCellTop - rowHeights[rowIndex] - dividerSize;
        }
        tempBottom = tempTop + rowHeights[rowIndex];
        int tempLeft = firstCellLeft;
        int right = 0;
        List<View> viewList = new ArrayList<>();
        for ( int columnIndex = 0; columnIndex < columnCount; columnIndex++ ){
            realColumnIndex = firstColumn + columnIndex;
            right = tempLeft + cellWidth;
            View cellOrExtraView = null;
            if ( isSupportExtraHeader && realColumnIndex == adapter.getColumnCount()  ){
                cellOrExtraView = adapter.getExtraCellView(sectionIndex,rowIndexInSection,realColumnIndex,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_CELL_EXTRA),this);
                bindViewTags(cellOrExtraView,McPropertyDataType.TYPE_PROPERTY_CELL_EXTRA,sectionIndex,rowIndex,realColumnIndex);
            }else{
                cellOrExtraView =  adapter.getCellView(sectionIndex,rowIndexInSection,realColumnIndex,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_CELL),this);
                bindViewTags(cellOrExtraView,McPropertyDataType.TYPE_PROPERTY_CELL,sectionIndex,rowIndex,realColumnIndex);
            }
            addView(cellOrExtraView,0);
            viewList.add(cellOrExtraView);
            exactlyMeasureChild(cellOrExtraView,rowHeights[rowIndex]);
            cellOrExtraView.layout(tempLeft,tempTop,right,tempBottom);

            tempLeft = right + dividerSize;
        }
        cellViews.add(index,viewList);
    }

    public static final int DIRECTION_TOP = 1;
    public static final int DIRECTION_BOTTOM = 2;
    public static final int DIRECTION_LEFT = 3;
    public static final int DIRECTION_RIGHT = 4;
    private void addSectionTitleTopAndBottom(int rowIndex,int index,int direction){
        int tempTop = 0;
        if ( direction == DIRECTION_BOTTOM ){
            tempTop = lastCellBottom + dividerSize;
        }else if ( direction == DIRECTION_TOP ){
            tempTop = firstCellTop - rowHeights[rowIndex] - dividerSize;
        }
        int sectionIndex = adapter.getSectionIndex(rowIndex);
        View sectionTitleView = adapter.getSectionHeaderView(sectionIndex,recycler.getRecycledView(McPropertyDataType.TYPE_GROUP_TITLE),this);
        addView(sectionTitleView,0);
        sectionTitleView.measure(MeasureSpec.EXACTLY | width,rowHeights[rowIndex]);
        sectionTitleView.layout(0,tempTop,width,tempTop + rowHeights[rowIndex]);
        sectionTitleViews.add(index,sectionTitleView);
        bindViewTags(sectionTitleView,McPropertyDataType.TYPE_GROUP_TITLE,sectionIndex,rowIndex,0);
    }

    /**
     * 从最底下添加views
     */
    private void addBottom(){
        lastRow = firstRow + cellTitleViews.size() + sectionTitleViews.size() ;
        if ( lastRow >= rowHeights.length ){
            return;
        }
        if ( adapter.isSectionTitle(lastRow) ){
            addSectionTitleTopAndBottom(lastRow,sectionTitleViews.size(),DIRECTION_BOTTOM);
        }else{
            addCellTopAndBottom(lastRow,cellTitleViews.size(),DIRECTION_BOTTOM);
            addCellTitleTopAndBottom(lastRow,cellTitleViews.size(),DIRECTION_BOTTOM);
        }

        int tempTop = lastCellBottom + dividerSize;
        lastCellBottom = tempTop + rowHeights[lastRow];
        //不用 lastRow ++
        lastRow = firstRow + cellTitleViews.size() + sectionTitleViews.size() ;
    }

    /**
     * 从最顶添加views
     */
    private void addTop(){
        if ( firstRow < 2 ){
            return;
        }
        firstRow--;
        if ( adapter.isSectionTitle( firstRow ) ){
            addSectionTitleTopAndBottom(firstRow,0,DIRECTION_TOP);
        }else{
            addCellTopAndBottom(firstRow,0,DIRECTION_TOP);
            addCellTitleTopAndBottom(firstRow,0,DIRECTION_TOP);
        }
        int tempTop = firstCellTop - rowHeights[firstRow] - dividerSize;
        firstCellTop = tempTop;
    }

    private void reset() {
        if ( !needRelayout ){
            return;
        }
        isRowHeightsInited = false;
        //todo scrollBounds？
        realRowCount = adapter.getTotalRowCount();
        realColumnCount = adapter.getColumnCount();
        if ( isSupportExtraHeader ){
            realColumnCount += 1;
        }
        rowHeightsAsignIndex = 0;
        if ( rowHeights != null ){
            rowHeights = null;
        }
        rowHeights = new int[realRowCount];
        leftCornerView = null;
        headerViews.clear();
        cellTitleViews.clear();
        cellViews.clear();
        sectionTitleViews.clear();
        removeAllViews();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);
        width = getMeasuredWidth();
        height = getMeasuredHeight();
        if ( adapter != null ){
            reset();
            measureRowHeights();
            maxScrollX = calculateMaxScrollX();
            maxScrollY = calculateMaxScrollY();
            firstCellLeft = cellWidth;
            lastCellRight = width;
            firstCellTop = rowHeights[0];
        }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if ( adapter == null ){
            return;
        }
        if ( !needRelayout && !changed ){
            return;
        }
        needRelayout = false;
        if ( adapter != null ){
            positionViews(0,0);
        }
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        final int viewType = (Integer) view.getTag(R.id.tag_view_type);
        if ( viewType != 0 ){
            recycler.addRecycledView(view,viewType);
            Log.e("qinqun","test");
        }
    }

    /**
     * 绑定view的一些信息（方便removeView的时候使用）
     * @param view
     * @param viewType
     * @param section
     * @param row
     * @param column
     */
    private void bindViewTags(View view,int viewType,int section,int row,int column){
        if ( view == null ){
            return;
        }
        view.setTag(R.id.tag_view_type,viewType);
        view.setTag(R.id.tag_section,section);
        view.setTag(R.id.tag_row,row);
        view.setTag(R.id.tag_column,column);
    }

    private int getArraySum(int[] array, int startIndex,int endIndex){
        if ( array == null || array.length < endIndex ){
            return 0;
        }
        int sum = 0;
        for ( int i = startIndex; i< endIndex; i++ ){
            sum += array[i];
        }
        return sum;
    }

    private View getCachedView(int viewType){
        return recycler.getRecycledView(viewType);
    }

    private class McPropertiesAdapterDataSetObserver extends DataSetObserver {

        @Override
        public void onChanged() {
            needRelayout = true;
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            // Do nothing
        }
    }

    public int getFirstColumn() {
        return firstColumn;
    }

    public void smoothScrollToColumn(int targetColumn){
        int deltaScrollX = scrollX - firstColumn * cellWidth;
        if ( targetColumn == firstColumn ){
            smoothScrollByOffset( - deltaScrollX );
        }else{
            smoothScrollByOffset( cellWidth + cellWidth - deltaScrollX );
        }
    }

    private void smoothScrollByOffset(int offset){
        if (!flinger.isFinished()) { // If scrolling, then stop now
            flinger.forceFinished();
        }
        flinger.smoothScroll(offset);
    }

    private class Flinger implements Runnable {
        private final Scroller scroller;

        private int lastX = 0;
        private int lastY = 0;

        Flinger(Context context) {
            scroller = new Scroller(context);
        }

        void start(int initX, int initY, int initialVelocityX, int initialVelocityY, int maxX, int maxY) {
            scroller.fling(initX, initY, initialVelocityX, initialVelocityY, 0, maxX, 0, maxY);

            lastX = initX;
            lastY = initY;
            post(this);
        }

        public void smoothScroll(int offset){
            scroller.startScroll(scrollX,scrollY,offset,0);
        }

        public void run() {
            if (scroller.isFinished()) {
                return;
            }

            boolean more = scroller.computeScrollOffset();
            int x = scroller.getCurrX();
            int y = scroller.getCurrY();
            int diffX = lastX - x;
            int diffY = lastY - y;
            if (diffX != 0 || diffY != 0) {
                scrollBy(diffX, diffY);
                lastX = x;
                lastY = y;
            }

            if (more) {
                post(this);
            }
        }

        boolean isFinished() {
            return scroller.isFinished();
        }

        void forceFinished() {
            if (!scroller.isFinished()) {
                scroller.forceFinished(true);
            }
        }
    }


    /******************************************************************************
	 * scrollbar实现 相关函数
	 ******************************************************************************/

    @Override
    protected int computeHorizontalScrollExtent() {
        final float tableSize = width - cellWidth;
        final float contentSize = realColumnCount * cellWidth;
        final float percentageOfVisibleView = tableSize / contentSize;

        return Math.round(percentageOfVisibleView * tableSize);
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        final float maxScrollX = getMaxScrollX();
        final float percentageOfViewScrolled = getActualScrollX() / maxScrollX;
        final int maxHorizontalScrollOffset = width - cellWidth - computeHorizontalScrollExtent();

        return cellWidth + Math.round(percentageOfViewScrolled * maxHorizontalScrollOffset);
    }


    @Override
    protected int computeHorizontalScrollRange() {
        return width;
    }


    @Override
    protected int computeVerticalScrollExtent() {
        final float tableSize = height - rowHeights[0];
        final float contentSize = getArraySum(rowHeights,1,adapter.getTotalRowCount());
        final float percentageOfVisibleView = tableSize / contentSize;

        return Math.round(percentageOfVisibleView * tableSize);
    }


    @Override
    protected int computeVerticalScrollOffset() {
        final float maxScrollY = getMaxScrollY();
        final float percentageOfViewScrolled = getActualScrollY() / maxScrollY;
        final int maxHorizontalScrollOffset = height - rowHeights[0] - computeVerticalScrollExtent();

        return rowHeights[0] + Math.round(percentageOfViewScrolled * maxHorizontalScrollOffset);
    }


    @Override
    protected int computeVerticalScrollRange() {
        return height;
    }
}
