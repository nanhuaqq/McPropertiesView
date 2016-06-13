package cn.mucang.property;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import cn.mucang.android.core.utils.UnitUtils;
import cn.mucang.property.utils.DimUtils;

/**
 * Created by nanhuaqq on 2016/5/12.
 */
public class McPropertiesView extends ViewGroup{
    private int currentX;
    private int currentY;

    //区域    //列    //行 组成坐标（0,0,0）（1,0,0）
    private int firstSection;
    private int firstColumn;
    private int firstRow;

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
        this.cellWidth = DimUtils.dip2px(getContext(),180);

        scrollX = 0;
        scrollY = 0;
        firstSection = 0;
        firstColumn = 0;
        firstRow = 0;

        realRowCount = 0;
        rowHeightsAsignIndex = 0;

        if ( this.adapter != null ){
            for (int i = 1; i < adapter.getSectionCount(); i++) {
                //从section 1开始，0是tableheader区域需要特殊处理
                //每一行的rowCount = adapter.getRowCount(i) + 1; 1表示SectionHeaderView
                realRowCount += adapter.getRowCount(i) + 1;
            }
            // realRowCount += 1;  头部的特殊处理 --> 就是一行
            realRowCount += 1;
        }

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
        if ( rowHeightsAsignIndex >= realRowCount - 1 ){ //越界保护
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
                    }else{
                        //measure cellView(从0开始)
                        final View cellView = adapter.getCellView(i,j,k,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_CELL),this);
                        cellHeight = Math.max(cellHeight,onlyMeasureChild(cellView));
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
            View leftCornerView = adapter.getLeftCornerView(recycler.getRecycledView(McPropertyDataType.TYPE_SHOW_ALL_OR_DIFF),this);
            leftCornerView.measure(MeasureSpec.makeMeasureSpec(cellWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(rowHeights[0],MeasureSpec.EXACTLY));
            addView(leftCornerView,0);
            leftCornerView.layout(0,0,cellWidth,rowHeights[0]);
            //HeaderView
            left = cellWidth - scrollX % cellWidth;
            for ( int columnIndex = firstColumn; columnIndex < columnCount && left < width; columnIndex++ ){
                View headerView = adapter.getTableHeaderView(columnIndex,recycler.getRecycledView(McPropertyDataType.TYPE_CAR_HEADER),this);
                right = left + cellWidth;
                addView(headerView,0);
                headerView.layout(left,0,right,rowHeights[0]);
                left = right;
            }

            /**
             * layout title
             */
            top = rowHeights[0] - ( (scrollY - getArraySum(rowHeights,firstRow)) % rowHeights[firstRow] );
            for ( int rowIndex = firstRow; rowIndex < adapter.getTotalRowCount() && top < height; rowIndex++ ){
                int sectionIndex = adapter.getSectionIndex(rowIndex);
                bottom = top + rowHeights[rowIndex];
                if ( adapter.isSectionTitle(rowIndex) ){ //如果是sectionTitle
                    View sectionTitleView = adapter.getSectionHeaderView(sectionIndex,recycler.getRecycledView(McPropertyDataType.TYPE_GROUP_TITLE),this);
                    addView(sectionTitleView,0);
                    sectionTitleView.layout(0,top,width,bottom);
                }else{ //cellTitle
                    int rowIndexInSection = adapter.getRowIndexInSection(rowIndex);
                    View cellTitleView = adapter.getCellTitleView(sectionIndex,rowIndexInSection,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_TITLE),this);
                    addView(cellTitleView,0);
                    cellTitleView.layout(0,top,cellWidth,bottom);

                    // layout cellView
                    left = cellWidth - scrollX % cellWidth;
                    for ( int columnIndex = firstColumn; columnIndex < columnCount && left < width; columnIndex++ ){
                        View cellView = adapter.getCellView(sectionIndex,rowIndexInSection,columnIndex,recycler.getRecycledView(McPropertyDataType.TYPE_PROPERTY_CELL),this);
                        right = left + cellWidth;
                        addView(cellView);
                        cellView.layout(left,top,right,rowHeights[0]);
                        left = right;
                    }
                }
                top = bottom;
            }
        }
    }

    private int getArraySum(int[] array,int endIndex){
        if ( array == null || array.length <= endIndex ){
            return 0;
        }
        int sum = 0;
        for ( int i = 0 ; i < endIndex; i++ ){
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
