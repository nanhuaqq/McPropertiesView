package cn.mucang.property;

import android.view.View;

import java.util.Stack;

/**
 * Created by nanhuaqq on 2016/5/12.
 */
public class McPropertiesChildrenRecycler {
    private Stack<View>[] views;

    public McPropertiesChildrenRecycler(int size) {
        views = new Stack[size];
        for (int i = 0; i < size ; i++) {
            views[i] = new Stack<>();
        }
    }

    public  void addRecycledView(View view,int type){
        views[type].push(view);
    }

    public View getRecycledView(int viewType){
        try{
            return views[viewType].pop();
        } catch (java.util.EmptyStackException e){
            return null;
        }
    }
}
