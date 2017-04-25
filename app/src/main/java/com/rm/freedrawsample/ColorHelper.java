package com.rm.freedrawsample;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

/**
 * Created by Riccardo Moro on 10/23/2016.
 */

public class ColorHelper {
    private static TypedArray colors;

    public static TypedArray getColors(@NonNull Context context) {
        if (colors == null) {
            colors = context.getResources().obtainTypedArray(R.array.material_colors);
        }
        return colors;
    }

    public static void recycleColors() {
        if (colors != null) {
            colors.recycle();
            colors = null;
        }
    }

    @ColorInt
    public static int getRandomColorIndex() {
        int index = (int) (Math.random() * colors.length());
        return index;
    }

    public static int getColor(int position) {
        return colors.getColor(position, Color.BLACK);
    }
}
