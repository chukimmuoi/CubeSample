package com.connectycube.sample.conference.utils;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntRange;

import com.connectycube.sample.conference.App;
import com.connectycube.sample.conference.R;

public class UiUtils {
    private static final int RANDOM_COLOR_START_RANGE = 0;
    private static final int RANDOM_COLOR_END_RANGE = 9;

    public static Drawable getGreyCircleDrawable() {
        return getColoredCircleDrawable(getColor(R.color.color_grey));
    }

    public static Drawable getColoredCircleDrawable(@ColorInt int color) {
        GradientDrawable drawable = (GradientDrawable) getDrawable(R.drawable.shape_circle);
        drawable.setColor(color);
        return drawable;
    }

    public static int getColor(@ColorRes int colorId) {
        return App.getInstance().getResources().getColor(colorId);
    }

    public static Drawable getDrawable(@DrawableRes int drawableId) {
        return App.getInstance().getResources().getDrawable(drawableId);
    }

    public static Drawable getColorCircleDrawable(int colorPosition) {
        return getColoredCircleDrawable(getCircleColor(colorPosition % RANDOM_COLOR_END_RANGE));
    }

    public static int getCircleColor(@IntRange(from = RANDOM_COLOR_START_RANGE, to = RANDOM_COLOR_END_RANGE)
                                             int colorPosition) {
        String colorIdName = String.format("random_color_%d", colorPosition + 1);
        int colorId = App.getInstance().getResources()
                .getIdentifier(colorIdName, "color", App.getInstance().getPackageName());

        return getColor(colorId);
    }
}
