package cn.mucang.property;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

import cn.mucang.property.utils.DimUtils;

/**
 * Created by nanhuaqq on 2016/5/12.
 */
public class McPropertiesView extends ViewGroup{

    View leftCornerView;

    private int currentX;
    private int currentY;

    //区域    //列    //行 组成坐标（0,0,0）（1,0,0）
    private int firstSection;
    private int firstColumn;
    private int firstRow;
    private int lastRow;
    private int lastColumn;
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

        this.setWillNotDraw(false);
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
        this.cellWidth = DimUtils.dip2px(getContext(),120);

        scrollX = 0;
        scrollY = 0;
        firstSection = 0;
        firstColumn = 0;
        firstRow = 1;

        realRowCount = adapter.getTotalRowCount();
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
        for (int i = -1; i < adapter.getColumnCount(); i++) {

            if ( i == -1 ){  // leftCornerView
                final View leftCornerView = adapter.getLeftCornerView(recycler.getRecycledView(McPropertyDataType.TYPE_SHOW_ALL_OR_DIFF),this);
                recycler.addRecycledView(leftCornerView,McPropertyDataType.TYPE_SHOW_ALL_OR_DIFF);
                headerHeight = Math.max(headerHeight,onlyMeasureChild(leftCornerView));
            }else{   // tableTitleView
                final View tableHeaderView = adapter.getTableHeaderView(i,recycler.getRecycledView(McPropertyDataType.TYPE_CAR_HEADER),this);
                recycler.addRecycledView(tableHeaderView,McPropertyDataType.TYPE_CAR_HEADER);
                headerHeight = Math.max(headerHeight,onlyMeasureChild(tableHeaderView));
            }

        }
        asignRowHeights(headerHeight);

        //随后分section measure
        for (int i = 0; i < adapter.getSectionCount() ; i++) {
            //首先measure sectionTitle
            final View sectionTitleView = adapter.getSectionHeaderView(i,recycler.getRecycledView(McPropertyDataType.TYPE_GROUP_TITLE),this);
            int sectionViewHeight = 0;
            sectionViewHeight = Math.max(sectionViewHeight,onlyMeasureChild(sectionTitleView));
            recycler.addRecycledView(sectionTitleView,McPropertyDataType.TYPE_GROUP_TITLE);
            asignRowHeights(sectionViewHeight);

            // 随后测量 section中的每一行
            for (int j = 0; j < adapter.getRowCount(i); j++){
                int cellHeight = 0;
                for ( int k = -1; k < adapter.getColumnCount(); k++ ){
                    if ( k == -1 ){ //measure cellTitleView(-1)
                        final View cellTitleView = adapter.getCellTitleView(i,j,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_TITLE),this);
                        cellHeight = Math.max( cellHeight,onlyMeasureChild(cellTitleView));
                        recycler.addRecycledView(cellTitleView,McPropertyDataType.TYPE_PROPERTY_TITLE);
                    }else{
                        //measure cellView(从0开始)
                        final View cellView = adapter.getCellView(i,j,k,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_CELL),this);
                        cellHeight = Math.max(cellHeight,onlyMeasureChild(cellView));
                        recycler.addRecycledView(cellView,McPropertyDataType.TYPE_PROPERTY_CELL);
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
        addViewInLayout(child, 0, params, true);

        child.measure(MeasureSpec.EXACTLY | cellWidth, MeasureSpec.UNSPECIFIED);
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

    private void scrollBounds() {
        scrollX = scrollBoundsX(scrollX);
        scrollY = scrollBoundsY(scrollY);
    }

    private int scrollBoundsX(int desiredScroll) {
        if (desiredScroll == 0) {
            // no op
        } else if (desiredScroll < 0) {
            desiredScroll = 0;
        } else {
            desiredScroll = Math.min(desiredScroll,adapter.getColumnCount()*cellWidth + cellWidth - width);
        }
        return desiredScroll;
    }

    private int scrollBoundsY(int desiredScroll){
        if (desiredScroll == 0) {
            // no op
        } else if (desiredScroll < 0) {
            desiredScroll = 0;
        } else {
            desiredScroll = Math.min(desiredScroll, getArraySum(rowHeights,0,adapter.getTotalRowCount())-height);
        }
        return desiredScroll;
    }

    private int getHeaderBottom(){
        int headerBottom;
        headerBottom = leftCornerView.getBottom();
        return headerBottom;
    }

    private int calculateFirstRowBottom(){
        int firstRowBottom;
        if ( adapter.isSectionTitle(firstRow) && !sectionTitleViews.isEmpty() ){
            View sectionTitleView = sectionTitleViews.get(0);
            firstRowBottom = sectionTitleView.getBottom();
        }else {
            View cellTitleView = cellTitleViews.get(0);
            firstRowBottom = cellTitleView.getBottom();
        }
        return  firstRowBottom;
    }

    private int calculateFirstRowTop(){
        int firstRowTop;
        if ( adapter.isSectionTitle(firstRow) && !sectionTitleViews.isEmpty() ){
            View sectionTitleView = sectionTitleViews.get(0);
            firstRowTop = sectionTitleView.getTop();
        }else {
            View cellTitleView = cellTitleViews.get(0);
            firstRowTop = cellTitleView.getTop();
        }
        return  firstRowTop;
    }

    private int calculateLastRowTop(){
        View sectionTitleView,cellTitleView;
        int sectionViewRowIndex = 0,cellTitleViewRowIndex = 0;
        int sectionViewTop = 0,cellTitleViewTop = 0;
        if ( sectionTitleViews.size() >= 1 ){
            sectionTitleView = sectionTitleViews.get(sectionTitleViews.size()-1);
            if ( sectionTitleView != null ){
                sectionViewRowIndex = (Integer) sectionTitleView.getTag(R.id.tag_row);
                sectionViewTop = sectionTitleView.getTop();
            }
        }
        if ( cellTitleViews.size() >= 1 ){
            cellTitleView = cellTitleViews.get(cellTitleViews.size() -1);
            if ( cellTitleView != null ){
                cellTitleViewRowIndex = (Integer)cellTitleView.getTag(R.id.tag_row);
                cellTitleViewTop = cellTitleView.getTop();
            }
        }

        return sectionViewRowIndex > cellTitleViewRowIndex ? sectionViewTop : cellTitleViewTop;
    }

    private int calculateLastRowBottom(){
        View sectionTitleView,cellTitleView;
        int sectionViewRowIndex = 0,cellTitleViewRowIndex = 0;
        int sectionViewBottom = 0,cellTitleViewBottom = 0;
        if ( sectionTitleViews.size() >= 1 ){
            sectionTitleView = sectionTitleViews.get(sectionTitleViews.size()-1);
            if ( sectionTitleView != null ){
                sectionViewRowIndex = (Integer) sectionTitleView.getTag(R.id.tag_row);
                sectionViewBottom = sectionTitleView.getBottom();
            }
        }
        if ( cellTitleViews.size() >= 1 ){
            cellTitleView = cellTitleViews.get(cellTitleViews.size() -1);
            if ( cellTitleView != null ){
                cellTitleViewRowIndex = (Integer)cellTitleView.getTag(R.id.tag_row);
                cellTitleViewBottom = cellTitleView.getBottom();
            }
        }

        return sectionViewRowIndex > cellTitleViewRowIndex ? sectionViewBottom : cellTitleViewBottom;
    }

    @Override
    public void scrollBy(int x, int y) {
        scrollX += x;
        scrollY += y;

        if ( needRelayout ){
            return;
        }

        scrollBounds();
        int deltaScrollX = scrollX - firstColumn * cellWidth;
        if ( x == 0 ){
            // 不做任何操作
        } else if ( x > 0 ){ //滑动时 view的回收与添加（）
            while ( cellWidth < deltaScrollX ){
                removeLeft();
                deltaScrollX -= cellWidth;
                firstColumn++;
            }
            while ( getFilledWidth(deltaScrollX) < width ){
                addRight();
            }
        } else {  // view的回收与添加 （从右到左）

            while (  getFilledWidth(deltaScrollX) - cellWidth > width ){
               removeRight();
            }

            while ( deltaScrollX < 0 ){ //都是思想实验啊 shit
                addLeft();
                firstColumn--;
                deltaScrollX = scrollX - firstColumn * cellWidth;
            }

        }

        int deltaScrollY = scrollY - getArraySum(rowHeights,1,firstRow);
        if ( y == 0 ){

        } else if ( y > 0 ){
            if ( calculateFirstRowBottom() < getHeaderBottom() ){
                Log.e("qinqunc","remove=>");
                removeTop();
                firstRow++;
            }
            if ( calculateLastRowBottom() < height ){
                Log.e("qinqunc","add=>");
                addBottom();
            }
        } else {
            if ( calculateLastRowTop() > height ){
                removeBottom();
            }
            if ( calculateFirstRowTop() > getHeaderBottom() ){
                addTop();
                firstRow = Math.max(1,firstRow);
                firstRow--;
            }
        }

        this.removeView(leftCornerView);
        this.addView(leftCornerView);

        Log.e("qinqun","header size=>"+headerViews.size());
        Log.e("qinqun","view list size=>"+cellViews.size());
        repositionViews();
    }

    private void repositionViews(){
        int left, top, right, bottom, i;

        //leftCornerView
        leftCornerView.layout(0,0,cellWidth,rowHeights[0]);
        bindViewTags(leftCornerView,McPropertyDataType.TYPE_SHOW_ALL_OR_DIFF,-1,0,0);

        //HeaderView
        left = cellWidth - scrollX % cellWidth;
        for ( View headerView:headerViews){
            right = left + cellWidth;
            headerView.layout(left,0,right,rowHeights[0]);
            left = right;
        }

        /**
         * layout title
         */
        int deltaScrollY = scrollY - getArraySum(rowHeights,1,firstRow);
        top = rowHeights[0] - deltaScrollY;
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
                View sectionTitleView = sectionTitleViews.get(sectionPosition);
                sectionTitleView.layout(0,top,width,bottom);
                sectionPosition++;
            }else{ //cellTitle // TODO: 2016/6/14 空指针
                if ( cellPosition >= cellTitleViews.size() ){
                    continue;
                }
                View cellTitleView = cellTitleViews.get(cellPosition);
                cellTitleView.layout(0,top,cellWidth,bottom);

                // layout cellView
                left = cellWidth - scrollX % cellWidth;
                List<View> viewList = cellViews.get(cellPosition);
                for ( View cellView : viewList ){
                    right = left + cellWidth;
                    cellView.layout(left,top,right,bottom);
                    left = right;
                }
                cellPosition ++;
            }
            top = bottom;
        }
        invalidate();
    }

    private void removeLeftOrRight(int position) {
        removeView(headerViews.remove(position));
        for (List<View> list : cellViews) {
            removeView(list.remove(position));
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
    }

    /**
     * 移除右边的views
     */
    private void removeRight(){
        removeLeftOrRight(headerViews.size()-1);
    }

    /**
     * 移除左边的views
     */
    private void removeLeft(){
        removeLeftOrRight(0);
    }

    /**
     * 从右边添加views
     */
    private void addRight(){
        final int size = headerViews.size();
        addLeftOrRight(firstColumn+size,size);
        addHeaderLeftOrRight(firstColumn+size,size);
    }

    /**
     * 从左边添加views
     */
    private void addLeft(){
        addLeftOrRight(firstColumn-1,0);
        addHeaderLeftOrRight(firstColumn-1,0);
    }

    private void addHeaderLeftOrRight(int column,int index){
        View headerView = adapter.getTableHeaderView(column,recycler.getRecycledView(McPropertyDataType.TYPE_GROUP_TITLE),this);
        addView(headerView);
        headerViews.add(index,headerView);
        bindViewTags(headerView,McPropertyDataType.TYPE_GROUP_TITLE,-1,0,column);
    }

    private void addLeftOrRight(int column,int index){
        int addRowCount = cellTitleViews.size() + sectionTitleViews.size();
        int realRowIndex;
        int cellPostion = 0;
        for ( int rowIndex = 0; rowIndex <  addRowCount; rowIndex++ ){
            //todo firstRow = 4 或 356等会造成数组越界
            realRowIndex = firstRow + rowIndex;
            int currentSection = adapter.getSectionIndex(realRowIndex);
            if ( currentSection == -1 ){
                continue;
            }
            if ( adapter.isSectionTitle(realRowIndex) ){ //如果是sectionTitle 不做处理
            }else{ //cellView
                if ( cellPostion >= cellViews.size() ){
                    continue;
                }
                int rowIndexInSection = adapter.getRowIndexInSection(realRowIndex);
                List<View> viewList = cellViews.get(cellPostion);
                View cellView = adapter.getCellView(currentSection,rowIndexInSection,column,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_CELL),this);
                addView(cellView,0);
                viewList.add(index,cellView);
                bindViewTags(cellView,McPropertyDataType.TYPE_PROPERTY_CELL,currentSection,realRowIndex,column);
                cellPostion++;
            }
        }
    }

    private void addCellTitleTopAndBottom(int rowIndex,int index){
        int sectionIndex = adapter.getSectionIndex(rowIndex);
        int rowIndexInSection = adapter.getRowIndexInSection(rowIndex);
        View cellTitleView = adapter.getCellTitleView(sectionIndex,rowIndexInSection,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_TITLE),this);
        addView(cellTitleView);
        cellTitleViews.add(index,cellTitleView);
        bindViewTags(cellTitleView,McPropertyDataType.TYPE_PROPERTY_TITLE,sectionIndex,rowIndex,-1);
    }

    private void addCellTopAndBottom(int rowIndex, int index) {
        int sectionIndex = adapter.getSectionIndex(rowIndex);
        int rowIndexInSection = adapter.getRowIndexInSection(rowIndex);
        int columnCount = headerViews.size();
        int realColumnIndex;
        List<View> viewList = new ArrayList<>();
        for ( int columnIndex = 0; columnIndex < columnCount; columnIndex++ ){
            realColumnIndex = firstColumn + columnIndex;
            View cellView = adapter.getCellView(sectionIndex,rowIndexInSection,realColumnIndex,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_CELL),this);
            addView(cellView,0);
            viewList.add(cellView);
            bindViewTags(cellView,McPropertyDataType.TYPE_PROPERTY_CELL,sectionIndex,rowIndex,realColumnIndex);
        }
        cellViews.add(index,viewList);
    }

    private void addSectionTitleTopAndBottom(int rowIndex,int index){
        int sectionIndex = adapter.getSectionIndex(rowIndex);
        View sectionTitleView = adapter.getSectionHeaderView(sectionIndex,recycler.getRecycledView(McPropertyDataType.TYPE_GROUP_TITLE),this);
        addView(sectionTitleView,0);
        sectionTitleViews.add(index,sectionTitleView);
        bindViewTags(sectionTitleView,McPropertyDataType.TYPE_GROUP_TITLE,sectionIndex,rowIndex,0);
    }

    /**
     * 从最底下添加views
     */
    private void addBottom(){
        lastRow = firstRow + cellTitleViews.size() + sectionTitleViews.size() - 1;
        if ( lastRow >= rowHeights.length - 1 ){
            return;
        }
        if ( adapter.isSectionTitle(lastRow+1) ){
            addSectionTitleTopAndBottom(lastRow+1,sectionTitleViews.size());
        }else{
            addCellTopAndBottom(lastRow+1,cellTitleViews.size());
            addCellTitleTopAndBottom(lastRow+1,cellTitleViews.size());
        }
    }

    /**
     * 从最顶添加views
     */
    private void addTop(){
        if ( firstRow < 2 ){
            return;
        }
        if ( adapter.isSectionTitle( firstRow -1 ) ){
            //todo -1的时候需要处理
            addSectionTitleTopAndBottom(firstRow-1,0);
        }else{
            //todo -1的时候需要处理
            addCellTopAndBottom(firstRow-1,0);
            addCellTitleTopAndBottom(firstRow-1,0);
        }
    }

    /**
     * 得到已经填充的宽度
     * @return
     */
    private int getFilledWidth(int deltaScrollX){
        return ( headerViews.size() + 1 ) * cellWidth - deltaScrollX;
    }

    /**
     * 得到已经填充的高度
     * @return
     */
    private int getFilledHeight(int start,int deltaScrollY) {
        return rowHeights[0] + getArraySum(rowHeights,start,start+cellTitleViews.size()+sectionTitleViews.size())  - deltaScrollY;
    }

    public int getActualScrollX() {
        return 0;
    }

    public int getActualScrollY() {
        return 0;
    }


    private int getMaxScrollX() {
        return 0;
    }

    private int getMaxScrollY() {
        return 0;
    }

    private void reset() {
        removeAllViews();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureRowHeights();

        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(widthSize, heightSize);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if ( !needRelayout && !changed ){
            return;
        }
        needRelayout = false;
        reset();

        if ( adapter != null ){
            width = r - l;
            height = b - t;
            int left,top,right,bottom;
            int columnCount = adapter.getColumnCount();

            //需要考虑分割线
            /**
             * 首先layout tableHeaderView
             */
            //leftCornerView
            leftCornerView = adapter.getLeftCornerView(recycler.getRecycledView(McPropertyDataType.TYPE_SHOW_ALL_OR_DIFF),this);
            leftCornerView.measure(MeasureSpec.makeMeasureSpec(cellWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(rowHeights[0],MeasureSpec.EXACTLY));
            addView(leftCornerView,0);
            leftCornerView.layout(0,0,cellWidth,rowHeights[0]);
            bindViewTags(leftCornerView,McPropertyDataType.TYPE_SHOW_ALL_OR_DIFF,-1,0,0);
            //HeaderView
            left = cellWidth - scrollX % cellWidth;
            for ( int columnIndex = firstColumn; columnIndex < columnCount && left < width; columnIndex++ ){
                View headerView = adapter.getTableHeaderView(columnIndex,recycler.getRecycledView(McPropertyDataType.TYPE_CAR_HEADER),this);
                right = left + cellWidth;
                addView(headerView,0);
                headerViews.add(headerView);
                headerView.layout(left,0,right,rowHeights[0]);
                bindViewTags(headerView,McPropertyDataType.TYPE_CAR_HEADER,-1,0,columnIndex);
                left = right;
            }

            /**
             * layout title
             */
            top = rowHeights[0] - scrollY % rowHeights[firstRow] ;
            for ( int rowIndex = firstRow; rowIndex < adapter.getTotalRowCount() && top < height; rowIndex++ ){
                int sectionIndex = adapter.getSectionIndex(rowIndex);
                if ( sectionIndex == -1 ){ //说明是 tableHeader
                    continue;
                }
                bottom = top + rowHeights[rowIndex];
                if ( adapter.isSectionTitle(rowIndex) ){ //如果是sectionTitle
                    View sectionTitleView = adapter.getSectionHeaderView(sectionIndex,recycler.getRecycledView(McPropertyDataType.TYPE_GROUP_TITLE),this);
                    addView(sectionTitleView,0);
                    sectionTitleViews.add(sectionTitleView);
                    sectionTitleView.layout(0,top,width,bottom);
                    bindViewTags(sectionTitleView,McPropertyDataType.TYPE_GROUP_TITLE,sectionIndex,rowIndex,0);
                }else{ //cellTitle
                    int rowIndexInSection = adapter.getRowIndexInSection(rowIndex);
                    View cellTitleView = adapter.getCellTitleView(sectionIndex,rowIndexInSection,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_TITLE),this);
                    addView(cellTitleView);
                    cellTitleViews.add(cellTitleView);
                    cellTitleView.layout(0,top,cellWidth,bottom);
                    bindViewTags(cellTitleView,McPropertyDataType.TYPE_PROPERTY_TITLE,sectionIndex,rowIndex,-1);

                    // layout cellView
                    left = cellWidth - scrollX % cellWidth;
                    List<View> viewList = new ArrayList<>();
                    for ( int columnIndex = firstColumn; columnIndex < columnCount && left < width; columnIndex++ ){
                        View cellView = adapter.getCellView(sectionIndex,rowIndexInSection,columnIndex,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_CELL),this);
                        right = left + cellWidth;
                        addView(cellView,0);
                        viewList.add(cellView);
                        cellView.layout(left,top,right,bottom);
                        bindViewTags(cellView,McPropertyDataType.TYPE_PROPERTY_CELL,sectionIndex,rowIndex,columnIndex);
                        left = right;
                    }
                    cellViews.add(viewList);
                }
                top = bottom;
            }
        }
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        final int viewType = (Integer) view.getTag(R.id.tag_view_type);
        recycler.addRecycledView(view,viewType);
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

    private int getArraySum(int[] array, int endIndex){
        if ( array == null || array.length <= endIndex ){
            return 0;
        }
        int sum = 0;
        for ( int i = 0 ; i < endIndex; i++ ){
            sum += array[i];
        }
        return sum;
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

    // http://stackoverflow.com/a/6219382/842697
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
}
