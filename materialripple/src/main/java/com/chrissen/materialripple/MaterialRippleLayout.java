package com.chrissen.materialripple;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Property;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.FrameLayout;

/**
 * @author Chrissen
 * @date 2018/1/19
 */

public class MaterialRippleLayout extends FrameLayout {

    private static final int  FADE_EXTRA_DELAY = 50;

    private int mRippleColor;
    private int mDefaultColor = Color.parseColor("#9E9E9E");

    private boolean mRippleInAdapter;
    private boolean mRipplePersistent;

    private View mChildView;

    private AdapterView mAdapterView;

    private Paint mPaint;
    private Point mTouchedPoint;

    private Rect bounds = new Rect();
    private Point currentCoords = new Point();
    private Point previousCoords = new Point();

    private GestureDetector mGestureDetector;

    private PerformClickEvent mPerformClickEvent;
    private PressedEvent mPressedEvent;

    private boolean eventCancelled;
    private boolean prepressed;
    private int positionInAdapter;

    private AnimatorSet mRippleAnimator;

    private float radius;
    private int mPositionInAdapter;

    private int      rippleColor;
    private boolean  rippleOverlay;
    private boolean  rippleHover;
    private int      rippleDiameter;
    private int      rippleDuration;
    private int      rippleAlpha;
    private boolean  rippleDelayClick;
    private int      rippleFadeDuration;
    private boolean  ripplePersistent;
    private Drawable rippleBackground;
    private boolean  rippleInAdapter;
    private float    rippleRoundedCorners;


    public MaterialRippleLayout(@NonNull Context context) {
        this(context,null);
    }

    public MaterialRippleLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MaterialRippleLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }

    private void init(Context context , AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs,R.styleable.MaterialRippleLayout);
        mRippleColor = ta.getColor(R.styleable.MaterialRippleLayout_ripple_color,mDefaultColor);
        ta.recycle();
    }

    @Override
    public void addView(View child) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("MaterialRippleLayout must have one child");
        }
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        child.setLayoutParams(lp);
        mChildView = child;
        super.addView(child);
    }


    /**
     * 为添加的view添加点击事件
     * @param l
     */
    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        if(mChildView == null){
            throw new IllegalStateException("MaterialRippleLayout must have a child view to handle clicks");
        }
        mChildView.setOnClickListener(l);
    }

    /**
     * 为childview添加长按事件
     * @param l
     */
    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener l) {
        if (mChildView == null) {
            throw new IllegalStateException("MaterialRippleLayout must have a child view to handle clicks");
        }
        mChildView.setOnLongClickListener(l);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !findClickableViewInChild(mChildView,(int) ev.getX(),(int) ev.getY());
    }

    private boolean findClickableViewInChild(View view , int x ,int y){
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childView = viewGroup.getChildAt(i);
                Rect rect = new Rect();
                //Hit rectangle in parent's coordinates
                childView.getHitRect(rect);
                boolean contains = rect.contains(x,y);
                if (contains) {
                    return findClickableViewInChild(childView,rect.left,y - rect.top);
                }
            }
        }else if(view != mChildView){
            return (view.isEnabled() && (view.isClickable() || view.isLongClickable() || view.isFocusableInTouchMode()));
        }
        return view.isFocusableInTouchMode();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean superOnTouchEvent = super.onTouchEvent(event);
        if (!isEnabled() || !mChildView.isEnabled()) {
            return superOnTouchEvent;
        }
        boolean isEventInBounds = bounds.contains((int) event.getX(),(int) event.getY());
        if (isEventInBounds) {
            previousCoords.set(currentCoords.x,currentCoords.y);
            currentCoords.set((int) event.getX(),(int) event.getY());
        }
        boolean gestureResult = mGestureDetector.onTouchEvent(event);
        if (gestureResult || hasPerformedLongPress) {
            return true;
        }else {
            int action = event.getActionMasked();
            switch (action){
                case MotionEvent.ACTION_UP:
                    mPerformClickEvent = new PerformClickEvent();
                    if(prepressed){
                        mChildView.setPressed(true);
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mChildView.setPressed(false);
                            }
                        }, ViewConfiguration.getPressedStateDuration());
                    }
                    if (isEventInBounds) {
                        startRipple(mPerformClickEvent);
                    }
                    if(!rippleDelayClick && isEventInBounds){
                        mPerformClickEvent.run();
                    }
                    cancelPressedEvent();
                    break;
                case MotionEvent.ACTION_DOWN:
                    setPositionInAdaper();
                    eventCancelled = false;
                    mPressedEvent = new PressedEvent(event);
                    if (isInScrollingContainer()) {
                        cancelPressedEvent();
                        prepressed = true;
                        postDelayed(mPressedEvent,ViewConfiguration.getTapTimeout());
                    }else {
                        mPressedEvent.run();
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    if (mRippleInAdapter) {
                        currentCoords.set(previousCoords.x,previousCoords.y);
                        previousCoords = new Point();
                    }
                    mChildView.onTouchEvent(event);
                    cancelPressedEvent();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!isEventInBounds) {
                        cancelPressedEvent();
                        mChildView.onTouchEvent(event);
                        eventCancelled = true;
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    private boolean isInScrollingContainer(){
        ViewParent p = getParent();
        while (p != null && p instanceof  ViewGroup){
            if(((ViewGroup)p).shouldDelayChildPressedState()){
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    private void setPositionInAdaper(){
        if (mRippleInAdapter) {
            mPositionInAdapter = findParentAdapterView().getPositionForView(MaterialRippleLayout.this);
        }
    }

    private void cancelPressedEvent(){
        if (mPressedEvent != null) {
            removeCallbacks(mPressedEvent);
            prepressed = false;
        }
    }

    private boolean hasPerformedLongPress;
    private GestureDetector.SimpleOnGestureListener longClickListener = new GestureDetector.SimpleOnGestureListener(){
        @Override
        public void onLongPress(MotionEvent e) {
            hasPerformedLongPress = mChildView.performLongClick();
            if (hasPerformedLongPress) {
                cancelPressedEvent();
            }
        }

        @Override
        public boolean onDown(MotionEvent e) {
            hasPerformedLongPress = false;
            return super.onDown(e);
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        boolean positionChanged = adapterPositionChanged();
        super.onDraw(canvas);
        if (!positionChanged) {
            Path clipPath = new Path();
            RectF rect = new RectF(0,0,canvas.getWidth(),canvas.getHeight());
            clipPath.addRoundRect(rect,rippleRoundedCorners,rippleRoundedCorners,Path.Direction.CW);
            canvas.clipPath(clipPath);
        }
        canvas.drawCircle(currentCoords.x,currentCoords.y,radius,mPaint);
    }

    private boolean adapterPositionChanged() {
        return false;
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bounds.set(0,0,w,h);
    }

    private void startRipple(final Runnable animationEndRunnable){
        if (eventCancelled) {
            return;
        }
        float endRadius = getEndRadius();
        cancelAnimations();
        mRippleAnimator = new AnimatorSet();
        mRippleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mRipplePersistent) {

                }
                if(animationEndRunnable != null && rippleDelayClick){
                    animationEndRunnable.run();
                }
                mChildView.setPressed(false);
            }
        });
        ObjectAnimator ripple = ObjectAnimator.ofFloat(this,radiusProperty,radius,endRadius);
        ripple.setDuration(rippleDuration);
        ripple.setInterpolator(new DecelerateInterpolator());
        ObjectAnimator fade = ObjectAnimator.ofInt(this,circleAlphaPropeerty,rippleAlpha,0);
        fade.setDuration(rippleFadeDuration);
        fade.setInterpolator(new AccelerateInterpolator());
        fade.setStartDelay(rippleDuration - rippleFadeDuration - FADE_EXTRA_DELAY);
        if (mRipplePersistent) {
            mRippleAnimator.play(ripple);
        }else if(getRadius() > endRadius){
            fade.setStartDelay(0);
            mRippleAnimator.play(fade);
        }else {
            mRippleAnimator.playTogether(ripple,fade);
        }
        mRippleAnimator.start();
    }

    private void cancelAnimations(){
        if (mRippleAnimator != null) {
            mRippleAnimator.cancel();
            mRippleAnimator.removeAllListeners();
        }
    }

    private float getEndRadius(){
        int width = getWidth();
        int height = getHeight();
        int halfWidth = width/2;
        int halfHeight = height/2;
        float radiusX = halfWidth > currentCoords.x ? width - currentCoords.x : currentCoords.x;
        float radiusY = halfHeight > currentCoords.y ? height - currentCoords.y : currentCoords.y;
        return (float) (Math.sqrt(Math.pow(radiusX,2) + Math.pow(radiusY,2))*1.2f);
    }


    private class PerformClickEvent implements Runnable{

        @Override
        public void run() {
            if (hasPerformedLongPress) {
                return;
            }
            if (getParent() instanceof AdapterView) {
                if (!mChildView.performClick()) {
                    clickAdapterView((AdapterView) getParent());
                }
            }else if(mRippleInAdapter){
                clickAdapterView(findParentAdapterView());
            }else {
                mChildView.performClick();
            }
        }

        private void clickAdapterView(AdapterView parent){
            int position = parent.getPositionForView(MaterialRippleLayout.this);
            long itemId = parent.getAdapter() != null?parent.getAdapter().getItemId(position):0;
            if(position != AdapterView.INVALID_POSITION){
                parent.performItemClick(MaterialRippleLayout.this,position,itemId);
            }

        }

    }

    private AdapterView findParentAdapterView(){
        if (mAdapterView != null) {
            return mAdapterView;
        }
        ViewParent current = getParent();
        while (true){
            if(current instanceof AdapterView){
                mAdapterView = (AdapterView) current;
                return mAdapterView;
            }else {
                try{
                    current = current.getParent();
                }catch (NullPointerException exception){
                    throw new RuntimeException("Could not find a parent AdapterView");
                }
            }
        }
    }


    private class PressedEvent implements Runnable{

        private final MotionEvent mEvent;

        private PressedEvent(MotionEvent event) {
            mEvent = event;
        }

        @Override
        public void run() {
            prepressed = false;
            mChildView.setLongClickable(false);
            mChildView.onTouchEvent(mEvent);
            mChildView.setPressed(true);

        }
    }

    static float dpToPx(Resources resources, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
    }


    private Property<MaterialRippleLayout,Float> radiusProperty = new Property<MaterialRippleLayout, Float>(Float.class,"radius") {
        @Override
        public Float get(MaterialRippleLayout object) {
            return object.getRadius();
        }

        @Override
        public void set(MaterialRippleLayout object, Float value) {
            object.setRadius(value);
        }
    };

    private Property<MaterialRippleLayout,Integer> circleAlphaPropeerty = new Property<MaterialRippleLayout, Integer>(Integer.class,"rippleAlpha") {
        @Override
        public Integer get(MaterialRippleLayout object) {
            return object.getRippleAlpha();
        }

        @Override
        public void set(MaterialRippleLayout object, Integer value) {
            object.setRippleAlpha(value);
        }
    };

    public static RippleBuilder build(View view){
        return new RippleBuilder(view);
    }

    private float getRadius(){
        return radius;
    }

    private void setRadius(float radius){
        this.radius = radius;
        invalidate();
    }

    public int getRippleAlpha() {
        return mPaint.getAlpha();
    }

    public void setRippleAlpha(Integer rippleAlpha) {
        mPaint.setAlpha(rippleAlpha);
        invalidate();
    }


    public int getRippleColor() {
        return rippleColor;
    }

    public void setRippleColor(int rippleColor) {
        this.rippleColor = rippleColor;
    }

    public boolean isRippleOverlay() {
        return rippleOverlay;
    }

    public void setRippleOverlay(boolean rippleOverlay) {
        this.rippleOverlay = rippleOverlay;
    }

    public boolean isRippleHover() {
        return rippleHover;
    }

    public void setRippleHover(boolean rippleHover) {
        this.rippleHover = rippleHover;
    }

    public int getRippleDiameter() {
        return rippleDiameter;
    }

    public void setRippleDiameter(int rippleDiameter) {
        this.rippleDiameter = rippleDiameter;
    }

    public int getRippleDuration() {
        return rippleDuration;
    }

    public void setRippleDuration(int rippleDuration) {
        this.rippleDuration = rippleDuration;
    }

    public void setRippleAlpha(int rippleAlpha) {
        this.rippleAlpha = (int) rippleAlpha * 255;
        mPaint.setAlpha(rippleAlpha);
        invalidate();
    }

    public boolean isRippleDelayClick() {
        return rippleDelayClick;
    }

    public void setRippleDelayClick(boolean rippleDelayClick) {
        this.rippleDelayClick = rippleDelayClick;
    }

    public int getRippleFadeDuration() {
        return rippleFadeDuration;
    }

    public void setRippleFadeDuration(int rippleFadeDuration) {
        this.rippleFadeDuration = rippleFadeDuration;
    }

    public boolean isRipplePersistent() {
        return ripplePersistent;
    }

    public void setRipplePersistent(boolean ripplePersistent) {
        this.ripplePersistent = ripplePersistent;
    }

    public Drawable getRippleBackground() {
        return rippleBackground;
    }

    public void setRippleBackground(Drawable rippleBackground) {
        this.rippleBackground = rippleBackground;
    }

    public boolean isRippleInAdapter() {
        return rippleInAdapter;
    }

    public void setRippleInAdapter(boolean rippleInAdapter) {
        this.rippleInAdapter = rippleInAdapter;
    }

    public float getRippleRoundedCorners() {
        return rippleRoundedCorners;
    }

    public void setRippleRoundedCorners(float rippleRoundedCorners) {
        this.rippleRoundedCorners = rippleRoundedCorners;
    }
}
