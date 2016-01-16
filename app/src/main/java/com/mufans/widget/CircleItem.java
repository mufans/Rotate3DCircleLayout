package com.mufans.widget;

import android.view.View;

/**
 * Created by liujun on 16-1-16.
 */
public class CircleItem implements Comparable<CircleItem> {

    public View view;
    public float distance; //离屏幕的距离

    @Override
    public int compareTo(CircleItem another) {
        //根据离屏幕的距离倒序排序
        return distance - another.distance < 0 ? 1 : -1;
    }

}
