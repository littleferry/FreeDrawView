package com.rm.freedrawview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;

public class FreeDrawView extends View implements View.OnTouchListener {
    private static final String TAG = FreeDrawView.class.getSimpleName();

    private static final float DEFAULT_STROKE_WIDTH = 10;
    private static final int DEFAULT_COLOR = Color.BLACK;
    private static final int DEFAULT_ALPHA = 255;

    private ResizeBehaviour mResizeBehaviour;
    private ArrayList<HistoryPath> mPaths = new ArrayList<>();

    private SerializablePaint mCurrentPaint;
    @ColorInt
    private int mPaintColor = DEFAULT_COLOR;
    @IntRange(from = 0, to = 255)
    private int mPaintAlpha = DEFAULT_ALPHA;
    private float mPaintWidthPx = DEFAULT_STROKE_WIDTH;

    private int mLastDimensionW = -1;
    private int mLastDimensionH = -1;

    // Needed to draw points
    private Paint mFillPaint;

    private PathDrawnListener mPathDrawnListener;
    private PathRedoUndoCountChangeListener mPathRedoUndoCountChangeListener;
    private HashMap<String, HistoryPath> mDrawPathArray = new HashMap<>();
    private String deviceId;

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    private HistoryPath getHistoryPath(String uid) {
        return mDrawPathArray.get(uid);
    }

    public FreeDrawView(Context context) {
        this(context, null);
    }

    public FreeDrawView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FreeDrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOnTouchListener(this);

        TypedArray a = null;
        try {

            a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.FreeDrawView,
                    defStyleAttr, 0);

            initPaints(a);
        } finally {
            if (a != null) {
                a.recycle();
            }
        }
    }

    /**
     * Set the paint color
     *
     * @param color The now color to be applied to the
     */
    public void setPaintColor(@ColorInt int color) {
        mPaintColor = color;
        mCurrentPaint.setColor(mPaintColor);
        mCurrentPaint.setAlpha(mPaintAlpha);// Restore the previous alpha
    }

    /**
     * Get the current paint color without it's alpha
     */
    @ColorInt
    public int getPaintColor() {
        return mPaintColor;
    }

    /**
     * Set the paint width in px
     *
     * @param widthPx The new weight in px, must be > 0
     */
    public void setPaintWidthPx(@FloatRange(from = 0) float widthPx) {
        if (widthPx > 0) {
            mPaintWidthPx = widthPx;
            mCurrentPaint.setStrokeWidth(widthPx);
        }
    }

    /**
     * {@link #getPaintWith(boolean)}
     */
    @FloatRange(from = 0)
    public float getPaintWidth() {
        return getPaintWith(false);
    }

    /**
     * Get the current paint with in dp or pixel
     */
    @FloatRange(from = 0)
    public float getPaintWith(boolean inDp) {
        if (inDp) {
            return FreeDrawHelper.convertPixelsToDp(mCurrentPaint.getStrokeWidth());
        } else {
            return mCurrentPaint.getStrokeWidth();
        }
    }


    /**
     * Set the paint opacity, must be between 0 and 1
     *
     * @param alpha The alpha to apply to the paint
     */
    public void setPaintAlpha(@IntRange(from = 0, to = 255) int alpha) {
        // Finish current path, so that the new setting is applied only to the next path
        mPaintAlpha = alpha;
        mCurrentPaint.setAlpha(mPaintAlpha);
    }

    /**
     * Get the current paint alpha
     */
    @IntRange(from = 0, to = 255)
    public int getPaintAlpha() {
        return mPaintAlpha;
    }

    public void undoLast() {
        undoLast(deviceId);
    }

    /**
     * Cancel the last drawn segment
     */
    public void undoLast(String uid) {
        if (mPaths.size() > 0) {
            for (int i = mPaths.size() - 1; i >= 0; i--) {
                HistoryPath path = mPaths.get(i);
                if (path.getUserId().equals(uid)) {
                    // Cancel the last one
                    mPaths.remove(i);
                    invalidate();
                    if (deviceId.equals(uid)) {
                        notifyRedoUndoCountChanged();
                        mPathDrawnListener.onUndoLast();
                    }
                    break;
                }
            }
            if (!playbacking) {
                frameIndex = mPaths.size();
            }
        }
    }

    /**
     * Remove all the paths
     */
    public void undoAll() {
        undoAll(deviceId);
    }

    /**
     * Remove all the paths
     */
    public void undoAll(String uid) {
        boolean isRemove = false;
        for (int i = mPaths.size() - 1; i >= 0; i--) {
            if (mPaths.get(i).equalsUid(uid)) {
                mPaths.remove(i);
                isRemove = true;
            }
        }
        Log.d(TAG, "undoAll() called with: uid = [" + uid + "] " + mPaths.size());
        if (!playbacking) {
            frameIndex = mPaths.size();
        }
        if (isRemove) {
            invalidate();
            if (deviceId.equals(uid)) {
                notifyRedoUndoCountChanged();
                mPathDrawnListener.onUndoAll();
            }
        }
    }

    /**
     * Get how many undo operations are available
     */
    public int getUndoCount() {
        int count = 0;
        for (int i = 0; i < mPaths.size(); i++) {
            if (mPaths.get(i).equalsUid(deviceId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Set a path drawn listener, will be called every time a new path is drawn
     */
    public void setOnPathDrawnListener(PathDrawnListener listener) {
        mPathDrawnListener = listener;
    }

    /**
     * Remove the path drawn listener
     */
    public void removePathDrawnListener() {
        mPathDrawnListener = null;
    }

    /**
     * Set a redo-undo count change listener, this will be called every time undo or redo count
     * changes
     */
    public void setPathRedoUndoCountChangeListener(PathRedoUndoCountChangeListener listener) {
        mPathRedoUndoCountChangeListener = listener;
    }

    /**
     * Remove the redo-undo count listener
     */
    public void removePathRedoUndoCountChangeListener() {
        mPathRedoUndoCountChangeListener = null;
    }

    // Internal methods
    private void notifyPathStart() {
        if (mPathDrawnListener != null) {
            mPathDrawnListener.onPathStart();
        }
    }

    private void notifyPathDrawn() {
        if (mPathDrawnListener != null) {
            mPathDrawnListener.onNewPathDrawn();
        }
    }

    private void notifyRedoUndoCountChanged() {
        if (mPathRedoUndoCountChangeListener != null) {
            mPathRedoUndoCountChangeListener.onUndoCountChanged(getUndoCount());
        }
    }

    private void initPaints(TypedArray a) {
        mCurrentPaint = new SerializablePaint(Paint.ANTI_ALIAS_FLAG);

        mCurrentPaint.setColor(a != null ? a.getColor(R.styleable.FreeDrawView_paintColor,
                mPaintColor) : mPaintColor);
        mCurrentPaint.setAlpha(a != null ?
                a.getInt(R.styleable.FreeDrawView_paintAlpha, mPaintAlpha)
                : mPaintAlpha);
        mCurrentPaint.setStrokeWidth(a != null ?
                a.getDimensionPixelSize(R.styleable.FreeDrawView_paintWidth,
                        (int) FreeDrawHelper.convertDpToPixels(DEFAULT_STROKE_WIDTH))
                : FreeDrawHelper.convertDpToPixels(DEFAULT_STROKE_WIDTH));

        mCurrentPaint.setStrokeJoin(Paint.Join.ROUND);
        mCurrentPaint.setStrokeCap(Paint.Cap.ROUND);
        mCurrentPaint.setPathEffect(new ComposePathEffect(
                new CornerPathEffect(100f),
                new CornerPathEffect(100f)));
        mCurrentPaint.setStyle(Paint.Style.STROKE);

        if (a != null) {
            int resizeBehaviour = a.getInt(R.styleable.FreeDrawView_resizeBehaviour, -1);
            mResizeBehaviour =
                    resizeBehaviour == 0 ? ResizeBehaviour.CLEAR :
                            resizeBehaviour == 1 ? ResizeBehaviour.FIT_XY :
                                    resizeBehaviour == 2 ? ResizeBehaviour.CROP :
                                            ResizeBehaviour.CROP;
        }

        initFillPaint();
    }

    private void initFillPaint() {
        mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFillPaint.setStyle(Paint.Style.FILL);
    }

    private void setupFillPaint(Paint from) {
        mFillPaint.setColor(from.getColor());
        mFillPaint.setAlpha(from.getAlpha());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw() called begin paths: " + mPaths.size());
        for (int i = 0; i < Math.min(frameIndex + 1, mPaths.size()); i++) {
            HistoryPath currentPath = mPaths.get(i);
            // If the path is just a single point, draw as a point
            if (currentPath.isPoint()) {
                setupFillPaint(currentPath.getPaint());
                canvas.drawCircle(currentPath.getOriginX(), currentPath.getOriginY(),
                        currentPath.getPaint().getStrokeWidth() / 2, mFillPaint);
            } else {// Else draw the complete path
                canvas.drawPath(currentPath.getPath(), currentPath.getPaint());
            }
        }
        Log.d(TAG, "onDraw() called end");
    }

    // Move去重
    private void addPoint(ArrayList<android.graphics.Point> points, float x, float y) {
        android.graphics.Point point;
        point = new android.graphics.Point();
        point.x = (int) x;
        point.y = (int) y;
        points.add(point);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        ArrayList<android.graphics.Point> points = new ArrayList<>();
        int paintWidth = (int) mPaintWidthPx;
        if ((motionEvent.getAction() != MotionEvent.ACTION_UP) &&
                (motionEvent.getAction() != MotionEvent.ACTION_CANCEL)) {
            for (int i = 0; i < motionEvent.getHistorySize(); i++) {
                addPoint(points, motionEvent.getHistoricalX(i), motionEvent.getHistoricalY(i));
            }
            addPoint(points, motionEvent.getX(), motionEvent.getY());
            if (points.size() > 0) {
                mPathDrawnListener.onTouch(motionEvent.getAction(), points, paintWidth,
                        getPaintColor(), getPaintAlpha());
            }
        } else {
            mPathDrawnListener.onTouch(motionEvent.getAction(), points, paintWidth,
                    getPaintColor(), getPaintAlpha());
        }

        // onTouch(true, motionEvent.getAction(), points, (int) mPaintWidth, getPaintColor(), getPaintAlpha());
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float xMultiplyFactor = 1;
        float yMultiplyFactor = 1;


        if (mLastDimensionW == -1) {
            mLastDimensionW = w;
        }

        if (mLastDimensionH == -1) {
            mLastDimensionH = h;
        }

        if (w >= 0 && w != oldw && w != mLastDimensionW) {
            xMultiplyFactor = (float) w / mLastDimensionW;
            mLastDimensionW = w;
        }

        if (h >= 0 && h != oldh && h != mLastDimensionH) {
            yMultiplyFactor = (float) h / mLastDimensionH;
            mLastDimensionH = h;
        }

        multiplyPathsAndPoints(xMultiplyFactor, yMultiplyFactor);
    }

    // Translate all the paths, used every time that this view size is changed
    @SuppressWarnings("SuspiciousNameCombination")
    private void multiplyPathsAndPoints(float xMultiplyFactor, float yMultiplyFactor) {

        // If both factors == 1 or <= 0 or no paths/points to apply things, just return
        if ((xMultiplyFactor == 1 && yMultiplyFactor == 1)
                || (xMultiplyFactor <= 0 || yMultiplyFactor <= 0) ||
                (mPaths.size() == 0)) {
            return;
        }

        if (mResizeBehaviour == ResizeBehaviour.CLEAR) {// If clear, clear all and return
            mPaths.clear();
            return;
        } else if (mResizeBehaviour == ResizeBehaviour.CROP) {
            xMultiplyFactor = yMultiplyFactor = 1;
        }

        // Adapt drawn paths
        for (HistoryPath historyPath : mPaths) {

            multiplySinglePath(historyPath, xMultiplyFactor, yMultiplyFactor);
        }
    }

    private void multiplySinglePath(HistoryPath historyPath,
                                    float xMultiplyFactor, float yMultiplyFactor) {

        // If it's a point, just multiply it's origins
        if (historyPath.isPoint()) {
            historyPath.setOriginX(historyPath.getOriginX() * xMultiplyFactor);
            historyPath.setOriginY(historyPath.getOriginY() * yMultiplyFactor);
        } else {

            // Doing this because of android, which has a problem with
            // multiple path transformations
            SerializablePath scaledPath = new SerializablePath();
            scaledPath.addPath(historyPath.getPath(),
                    new TranslateMatrix(xMultiplyFactor, yMultiplyFactor));
            historyPath.getPath().close();
            historyPath.setPath(scaledPath);
        }
    }

    private boolean playbacking;
    public boolean isPlaybacking() {
        return playbacking;
    }

    private int frameIndex = 0;
    /**
     * 回放
     */
    public void startPlayback() {
        if (!playbacking) {
            playbacking = true;
            frameIndex = -1;
            invalidate();
            handler.sendEmptyMessageDelayed(0, 1000);
        }
    }

    /**
     * 停止回放
     */
    public void stopPlayback() {
        if (playbacking) {
            playbacking = false;
            handler.removeMessages(0);
            frameIndex = mPaths.size();
            invalidate();
        }
    }

    Handler handler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            if (playbacking) {
                frameIndex ++;
                long delayTime = 300;
                if (frameIndex >= mPaths.size()) {
                    frameIndex = -1;
                    delayTime = 1000;
                }
                invalidate();
                handler.sendEmptyMessageDelayed(0, delayTime);
            }
        }
    };

    public void onTouch(String uid, int touchEvent, ArrayList<android.graphics.Point> points, int paintWidth,
                                     int paintColor, int paintAlpha, int width, int height) {
        int w = getWidth();
        int h = getHeight();
        // 先把点保存到数组
        ArrayList<Point> pointList = new ArrayList<>();
        if (points.size() > 0) {
            for (int i = 0; i < points.size(); i++) {
                Point point = new Point();
                // 转换一下缩放坐标
                point.x = (points.get(i).x * w) / width;
                point.y = (points.get(i).y * h) / height;
                pointList.add(point);
            }
        }
        mCurrentPaint.setColor(paintColor);
        mCurrentPaint.setAlpha(paintAlpha);
        mCurrentPaint.setStrokeWidth(paintWidth * w / width);
        boolean selfDraw = isSelfDraw(uid);
        if (selfDraw && touchEvent == MotionEvent.ACTION_DOWN) {
            notifyPathStart();
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }

        if (touchEvent == MotionEvent.ACTION_DOWN) {
            mDrawPathArray.put(uid, null);
            addPoint(selfDraw, uid, pointList);
        } else if (touchEvent == MotionEvent.ACTION_MOVE) {
            // mDrawPathArray.put(uid, null);
            addPoint(selfDraw, uid, pointList);
        } else if (touchEvent == MotionEvent.ACTION_UP) {
            if (pointList.size() > 0) {
                addPoint(selfDraw, uid, pointList);
            }
            mDrawPathArray.put(uid, null);
        }
        invalidate();

        Log.d(TAG, "onTouch() called end with: selfDraw = [" + selfDraw + "], touchEvent = [" + touchEvent + "]");
    }

    private boolean isSelfDraw(String uid) {
        if (TextUtils.isEmpty(uid)) {
            return false;
        }
        if (TextUtils.isEmpty(deviceId)) {
            return false;
        }
        return uid.equals(deviceId);
    }

    private void addPoint(boolean selfDraw, String uid, ArrayList<Point> points) {
        HistoryPath hp = getHistoryPath(uid);
        if (hp == null) {
            hp = new HistoryPath(new SerializablePaint(mCurrentPaint), points, uid);
            mPaths.add(hp);
            if (!playbacking) {
                frameIndex = mPaths.size();
            }
            mDrawPathArray.put(uid, hp);
            if (selfDraw) {
                notifyPathDrawn();
                notifyRedoUndoCountChanged();
            }
        } else {
            hp.addPoint(points);
        }
    }
}
