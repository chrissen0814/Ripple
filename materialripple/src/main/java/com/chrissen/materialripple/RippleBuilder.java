package com.chrissen.materialripple;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author Chrissen
 * @date 2018/1/19
 */

public class RippleBuilder {

    private Context mContext;
    private View mChild;

    private int mRippleColor = RippleConstant.DEFAULT_COLOR;
    private boolean mRippleOverlay = RippleConstant.DEFAULT_RIPPLE_OVERLAY;
    private boolean mRippleHover = RippleConstant.DEFAULT_HOVER;
    private float mRippleDiameter = RippleConstant.DEFAULT_DIAMETER_DP;
    private int mRippleDuration = RippleConstant.DEFAULT_DURATION;
    private float mRippleAlpha = RippleConstant.DEFAULT_ALPHA;
    private boolean mRippleDelayClick = RippleConstant.DEFAULT_DELAY_CLICK;
    private int mRippleFadeDuration = RippleConstant.DEFAULT_FADE_DURATION;
    private boolean mRipplePersistent = RippleConstant.DEFAULT_PERSISTENT;
    private int mRippleBackground = RippleConstant.DEFAULT_BACKGROUND;
    private boolean mRippleSearchAdapter = RippleConstant.DEFAULT_SEARCH_ADAPTER;
    private float mRippleRoundedCorner = RippleConstant.DEFAULT_ROUNDED_CORNERS;

    public RippleBuilder(View child) {
        mChild = child;
        mContext = child.getContext();
    }

    public RippleBuilder rippleColor(int rippleColor){
        mRippleColor = rippleColor;
        return this;
    }

    public RippleBuilder rippleOverlay(boolean overlay){
        mRippleOverlay = overlay;
        return this;
    }

    public RippleBuilder rippleHover(boolean hover){
        mRippleHover = hover;
        return this;
    }

    public RippleBuilder rippleDiameterDp(int diameterDp){
        mRippleDiameter = diameterDp;
        return this;
    }

    public RippleBuilder rippleDuration(int duration){
        mRippleDuration = duration;
        return this;
    }

    public RippleBuilder rippleAlpha(float alpha){
        mRippleAlpha = alpha;
        return this;
    }

    public RippleBuilder rippleDelayClick(boolean delayClick) {
        mRippleDelayClick = delayClick;
        return this;
    }

    public RippleBuilder rippleFadeDuration(int fadeDuration) {
        mRippleFadeDuration = fadeDuration;
        return this;
    }

    public RippleBuilder ripplePersistent(boolean persistent) {
        mRipplePersistent = persistent;
        return this;
    }

    public RippleBuilder rippleBackground(int color) {
        mRippleBackground = color;
        return this;
    }

    public RippleBuilder rippleInAdapter(boolean inAdapter) {
        mRippleSearchAdapter = inAdapter;
        return this;
    }

    public RippleBuilder rippleRoundedCorners(int radiusDp) {
        mRippleRoundedCorner = radiusDp;
        return this;
    }

    public MaterialRippleLayout create(){
        MaterialRippleLayout layout = new MaterialRippleLayout(mContext);
        layout.setRippleAlpha(mRippleColor);
        layout.setRippleColor(mRippleColor);
        layout.setRippleDelayClick(mRippleDelayClick);
        layout.setRippleDuration(mRippleDuration);
        layout.setRippleFadeDuration(mRippleFadeDuration);
        layout.setRippleHover(mRippleHover);
        layout.setRipplePersistent(mRipplePersistent);
        layout.setRippleOverlay(mRippleOverlay);
        layout.setRippleInAdapter(mRippleSearchAdapter);
        ViewGroup.LayoutParams params = mChild.getLayoutParams();
        ViewGroup parent = (ViewGroup) mChild.getParent();
        int index = 0;
        if (parent != null && parent instanceof MaterialRippleLayout) {
            throw new IllegalStateException("MaterialRippleLayout could not be created");
        }
        if (parent != null) {
            index = parent.indexOfChild(mChild);
            parent.removeView(mChild);
        }
        layout.addView(mChild,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (parent != null) {
            parent.addView(layout,index,params);
        }
        return layout;
    }


}
