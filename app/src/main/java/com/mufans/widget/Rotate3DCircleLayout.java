package com.mufans.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by liujun on 16-1-12.
 * 实现子view绕y轴旋转的容器
 */
public class Rotate3DCircleLayout extends ViewGroup {

    /**
     * 状态
     */
    private static final int STATUS_RESET = 0;
    private static final int STATUS_MOVE = 1;

    /**
     * 调整缩放倍率的参数
     */
    private static final int SCALE_DISTANCE_FACOTR = 2;

    /**
     * 当前状态
     */
    private int status;
    /**
     * 手指放下的坐标
     */
    private int downX, downY;
    /**
     * 上次移动的坐标
     */
    private int lastY, lastX;

    /**
     * 旋转的半径
     */
    private int radius;

    /**
     * 累计旋转的角度
     */
    private int accAngle;
    /**
     * 根据 childcount等分的角度
     */
    private int spliteAngle;

    /**
     * 缩放参数
     */
    private float factor;

    private int touchSlop;

    private List<View> viewList = new LinkedList<>();

    private OnItemClickListener listener;

    public Rotate3DCircleLayout(Context context) {
        super(context);
        init(context);
    }

    public Rotate3DCircleLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Rotate3DCircleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        for (int i = 0; i < getChildCount(); i++) {
            int childHeight = getChildAt(i).getMeasuredHeight();
            if (height < childHeight) {
                height = childHeight;
            }
        }
        setMeasuredDimension(width, height);
        Log.d("Rotate", width + "," + height);
        radius = getMeasuredWidth() / 4;
        if (getChildCount() > 0) {
            factor = calculatorScaleFactor(radius, getChildAt(0).getMeasuredWidth());
        }
    }

    /**
     * 计算缩放参数，保证2个圆旋转到最前位置并且平行时能够相切
     *
     * @param radius
     * @param childWidth
     * @return
     */
    private float calculatorScaleFactor(float radius, int childWidth) {
        int halfSpliteAngle = spliteAngle / 2;
        float x = (float) (radius * Math.cos(angleToRadians(-1 * halfSpliteAngle)));
        float distance = radius - x;
        float ratio = (2 * radius - distance / 2) / (2 * radius);
        factor = (float) (2 * Math.sin(angleToRadians(halfSpliteAngle)) * radius / (childWidth * ratio));
        return factor;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int childCount = getChildCount();
        float ratioTmp = 0;
        int left = 0;
        int top = 0;
        for (int i = 0; i < childCount; i++) {
            View child = viewList.get(i);
            float radains = angleToRadians(i * spliteAngle + accAngle); //child当前的弧度
            int centerX = getMeasuredWidth() / 2 + (int) (Math.sin(radains) * radius + 0.5f); //每个圆的圆心的 x坐标
            float x = (float) (radius * Math.cos(radains));
            float distance = radius - x; //每个圆相对于屏幕z轴的距离
            float ratio = (2 * radius - distance / SCALE_DISTANCE_FACOTR) * factor / (2 * radius);
            child.setScaleX(ratio);
            child.setScaleY(ratio);
            if (ratioTmp < ratio) {
                ratioTmp = ratio;
                bringChildToFront(child);//将最前的view层级切换到最前
            }
            left = centerX - child.getMeasuredWidth() / 2;
            top = height / 2 - child.getMeasuredHeight() / 2;
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.setTag(i);
            child.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    autoRotateToCurrent(v);
                    if (listener != null) {
                        listener.onItemClick((Integer) v.getTag());
                    }
                }
            });
            viewList.add(child);
        }
        spliteAngle = (360 / getChildCount());

    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        int event = ev.getAction();
        if (event == MotionEvent.ACTION_DOWN) {
            downX = x;
            downY = y;
        } else if (event == MotionEvent.ACTION_MOVE) {
            if (status == STATUS_RESET) {
                if (Math.abs(x - downX) > touchSlop) {
                    status = STATUS_MOVE;
                    lastX = x;
                    lastY = y;
                    return true;
                }
            }
        } else if (event == MotionEvent.ACTION_UP
                ) {
            downX = -1;
            downY = -1;
            lastX = -1;
            lastY = -1;
            status = STATUS_RESET;
        }
        lastX = x;
        lastY = y;
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        int event = ev.getAction();
        if (event == MotionEvent.ACTION_DOWN) {
            downX = x;
            downY = y;
        } else if (event == MotionEvent.ACTION_MOVE) {
            if (status == STATUS_RESET) {
                if (Math.abs(x - downX) > touchSlop) {
                    status = STATUS_MOVE;
                }
            } else {
                rotate(x - lastX);
            }
        } else if (event == MotionEvent.ACTION_UP) {
            autoRotate();
            downX = -1;
            downY = -1;
            lastX = -1;
            lastY = -1;
            status = STATUS_RESET;
        }
        lastX = x;
        lastY = y;
        return true;
    }


    /**
     * 旋转, 将横向移动距离转换为y轴旋转的角度，调用requestLayout调整位置
     *
     * @param deltaX 手指横向滑动距离
     */
    private void rotate(int deltaX) {
        accAngle += deltaX * 1.0f / radius * 90;
        requestLayout();
    }

    /**
     * 角度转弧度
     *
     * @param angle
     * @return
     */
    private static float angleToRadians(int angle) {
        float radians = (float) (angle * 1.0f / 180 * Math.PI);
        return radians;
    }

    /**
     * 动画滚动
     *
     * @param from 开始角度
     * @param to   结束角度
     */
    private void animRotate(int from, int to) {
        if (from == to) {
            return;
        }
        ValueAnimator valueAnimator = ValueAnimator.ofInt(from, to);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int angle = (int) animation.getAnimatedValue();
                accAngle = angle;
                //当动画完成时，重新调整viewList顺序，并重置accAngle
                if (animation.getAnimatedFraction() == 1) {
                    int retainCount = accAngle / spliteAngle % viewList.size(); //根据当前的旋转角度，计算出调整的次数，符号代表方向
                    int count = Math.abs(retainCount);//调整次序的次数
                    //调整次序
                    while (count > 0) {
                        count--;
                        if (retainCount > 0) {
                            View last = viewList.remove(viewList.size() - 1);
                            viewList.add(0, last);
                        } else {
                            View first = viewList.remove(0);
                            viewList.add(first);
                        }
                    }
                    accAngle = 0;
                }
                requestLayout();
            }
        });
        valueAnimator.setDuration(300);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.start();
    }

    /**
     * 自动旋转，调整位置
     */
    private void autoRotate() {
        final int retainAngle = accAngle % spliteAngle;
        int from = accAngle;
        int to = from;
        if (Math.abs(retainAngle) > spliteAngle / 2) {
            if (accAngle > 0) {
                to += spliteAngle - retainAngle;
            } else {
                to -= spliteAngle + retainAngle;
            }
        } else {
            to -= retainAngle;
        }

        Log.e("Rotate", "from:" + from + ",to:" + to);

        animRotate(from, to);

    }

    /**
     * 自动滚动到最前端
     *
     * @param view
     */
    public void autoRotateToCurrent(View view) {
        int index = viewList.indexOf(view);
        int angle = getChildAngle(index) % 360;
        int rotateAngle = 0;
        if (angle > 0) {
            if (angle > 180) {
                rotateAngle = 360 - angle;
            } else {
                rotateAngle = -1 * angle;
            }
        } else {
            if (angle < -180) {
                rotateAngle = 360 + angle;
            } else {
                rotateAngle = -1 * angle;
            }
        }
        int from = accAngle;
        int to = rotateAngle + from;
        animRotate(from, to);
    }

    /**
     * 获取child的角度
     *
     * @param index viewlist的索引
     * @return
     */
    private int getChildAngle(int index) {
        return index * spliteAngle + accAngle;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.listener = onItemClickListener;
    }

    /**
     * 点击事件回调
     */
    public static interface OnItemClickListener {
        void onItemClick(int pos);
    }
}
