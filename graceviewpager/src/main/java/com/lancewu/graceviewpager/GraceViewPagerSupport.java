package com.lancewu.graceviewpager;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * Created by wrs on 2018/8/8.
 * <br/>
 * 为ViewPager提供额外的支持和Bug修复；
 * <ol>
 * <li>支持ViewPager尺寸变化以及Page尺寸变化后能够正确定位，特别是在一屏显示多item的场景下发生了padding变化：
 * {@link #supportLayoutChange(ViewPager)}</li>
 * <li>因为调用ViewPager.setPageMargin()会出现滚动位置错误问题，所以提供修复的替代调用：
 * {@link #setPageMargin(ViewPager, int)}</li>
 * </ol>
 */
public final class GraceViewPagerSupport {

    /**
     * 使ViewPager支持尺寸变化后，能够正确定位。
     * 例如：在一屏显示多item的场景下发生了ViewPager的width变化、padding变化后，不进行重新定位滚动位置会导致显示位置错误。
     * <br/>
     * <font color=red>注意：ViewPager初始化后调用一次即可（原理是添加View.OnLayoutChangeListener），不需要重复调用</font>
     *
     * @param viewPager 需要添加支持的viewPager
     */
    public static void supportLayoutChange(@NonNull ViewPager viewPager) {
        viewPager.addOnLayoutChangeListener(new ViewPagerLayoutChangeListener(viewPager));
    }

    /**
     * ViewPager.recomputeScrollPosition()方法源码有Bug，计算的scrollX值有误，导致动态去调用setPageMargin()后，
     * 滚动位置有问题。<br/>
     * 直接调用该方法替代{@link ViewPager#setPageMargin(int)}，可以修正滚动位置错误问题。
     *
     * @param viewPager  ViewPager
     * @param pageMargin pageMargin
     */
    public static void setPageMargin(@NonNull ViewPager viewPager, int pageMargin) {
        int oldPageMargin = viewPager.getPageMargin();
        if (pageMargin == oldPageMargin) {
            return;
        }
        int childWidth = viewPager.getMeasuredWidth() - viewPager.getPaddingLeft() - viewPager.getPaddingRight();
        if (childWidth == 0) {
            viewPager.setPageMargin(pageMargin);
        } else {
            /* setPageMargin()调用会进行重新计算item的offset和重新layout，该offset已经考虑了pageMargin，所以
             * 直接取出调用前的scrollX值进行定位即可
             */
            int oldScrollX = viewPager.getScrollX();
            viewPager.setPageMargin(pageMargin);
            viewPager.scrollTo(oldScrollX, viewPager.getScrollY());
        }
    }

    /**
     * 布局变化监听
     */
    private static final class ViewPagerLayoutChangeListener implements View.OnLayoutChangeListener {

        private ViewPager mViewPager;
        private int mLastChildWidth;

        ViewPagerLayoutChangeListener(ViewPager viewPager) {
            mViewPager = viewPager;
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                                   int oldTop, int oldRight, int oldBottom) {
            int childWidth = right - left - v.getPaddingLeft() - v.getPaddingRight();
            if (childWidth == 0) {
                return;
            }
            if (mLastChildWidth == 0) {
                mLastChildWidth = childWidth;
                return;
            }
            if (mLastChildWidth == childWidth) {
                return;
            }
            /*
             * 问题：page宽度变化后，layout会正确放置child位置，但是scrollX值仍然是旧值，导致绘制位置偏差；
             * 同时，经过数据刷新后scrollX=0不代表定位到第一个页面，取决于最左边child的位置，所以该值有可能是负值；
             * 解决方案：根据旧值获取页面偏移，根据页面偏移计算新的scrollX位置
             */
            recomputeScrollPosition(mViewPager, mViewPager.getScrollX(), childWidth, mLastChildWidth);
            mLastChildWidth = childWidth;
        }

        /**
         * 重新计算滚动位置
         *
         * @param viewPager     ViewPager
         * @param scrollX       当前滚动位置
         * @param childWidth    新的item宽度
         * @param oldChildWidth 旧的item宽度
         */
        private static void recomputeScrollPosition(ViewPager viewPager, int scrollX,
                                                    int childWidth, int oldChildWidth) {
            float pageOffset = (float) scrollX / oldChildWidth;
            int newOffsetPixels = (int) (pageOffset * childWidth);
            viewPager.scrollTo(newOffsetPixels, viewPager.getScrollY());
        }

    }

}