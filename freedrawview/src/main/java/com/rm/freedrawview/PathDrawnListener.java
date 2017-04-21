package com.rm.freedrawview;

import java.util.ArrayList;

/**
 * Created by Riccardo on 22/11/16.
 */

public interface PathDrawnListener {

    void onPathStart();

    void onNewPathDrawn();

    void onTouch(int touchEvent, ArrayList<android.graphics.Point> points, int paintWidth, int paintColor, int paintAlpha);

    void onUndoLast();

    void onUndoAll();
}
