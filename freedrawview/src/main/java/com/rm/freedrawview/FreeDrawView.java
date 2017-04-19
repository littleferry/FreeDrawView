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
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by Riccardo Moro on 9/10/2016.
 */
public class FreeDrawView extends View implements View.OnTouchListener {
    private static final String TAG = FreeDrawView.class.getSimpleName();

    private static final float DEFAULT_STROKE_WIDTH = 4;
    private static final int DEFAULT_COLOR = Color.BLACK;
    private static final int DEFAULT_ALPHA = 255;

    private ResizeBehaviour mResizeBehaviour;
    private ArrayList<HistoryPath> mPaths = new ArrayList<>();
    private ArrayList<HistoryPath> mCanceledPaths = new ArrayList<>();

    // 自己画的
    private SerializablePaint mCurrentPaint;
    @ColorInt
    private int mPaintColor = DEFAULT_COLOR;
    @IntRange(from = 0, to = 255)
    private int mPaintAlpha = DEFAULT_ALPHA;
    private float mPaintWidth = DEFAULT_STROKE_WIDTH;
    private boolean mFinishPath = false;

    private int mLastDimensionW = -1;
    private int mLastDimensionH = -1;

    // Needed to draw points
    private Paint mFillPaint;

    private PathDrawnListener mPathDrawnListener;
    private PathRedoUndoCountChangeListener mPathRedoUndoCountChangeListener;
    private HistoryPath[] mDrawPath = new HistoryPath[2];

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
        mFinishPath = true;

        invalidate();

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
     * Get the current color with the current alpha
     */
    @ColorInt
    public int getPaintColorWithAlpha() {
        return mCurrentPaint.getColor();
    }


    /**
     * Set the paint width in px
     *
     * @param widthPx The new weight in px, must be > 0
     */
    private void setPaintWidthPx(@FloatRange(from = 0) float widthPx) {
        if (widthPx > 0) {
            mFinishPath = true;

            invalidate();

            mCurrentPaint.setStrokeWidth(widthPx);
        }
    }

    /**
     * Set the paint width in dp
     *
     * @param dp The new weight in dp, must be > 0
     */
    public void setPaintWidthDp(float dp) {
        mPaintWidth = dp;
        setPaintWidthPx(FreeDrawHelper.convertDpToPixels(dp));
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

        // Finish current path and redraw, so that the new setting is applied only to the next path
        mFinishPath = true;
        invalidate();

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


    /**
     * Set what to do when the view is resized (on rotation if its dimensions are not fixed)
     * {@link ResizeBehaviour}
     */
    public void setResizeBehaviour(ResizeBehaviour newBehaviour) {
        mResizeBehaviour = newBehaviour;
    }

    /**
     * Get the current behaviour on view resize
     */
    public ResizeBehaviour getResizeBehaviour() {
        return mResizeBehaviour;
    }


    /**
     * Cancel the last drawn segment
     */
    public void undoLast() {
        if (mPaths.size() > 0) {
            for (int i = mPaths.size() - 1; i >= 0; i--) {
                HistoryPath path = mPaths.get(i);
                if (path.isSelfDraw()) {
                    // End current path
                    mFinishPath = true;
                    invalidate();

                    // Cancel the last one and redraw
                    mCanceledPaths.add(path);
                    mPaths.remove(i);
                    invalidate();

                    notifyRedoUndoCountChanged();

                    mPathDrawnListener.onUndoLast();
                    break;
                }
            }
        }
    }

    /**
     * Cancel the other last drawn segment
     */
    public void undoOtherLast() {
        for (int i = mPaths.size() - 1; i >= 0; i--) {
            HistoryPath path = mPaths.get(i);
            if (!path.isSelfDraw()) {
                // End current path
                mFinishPath = true;
                invalidate();
                // Cancel the last one and redraw
                mPaths.remove(i);
                invalidate();
                break;
            }
        }
    }

    /**
     * Re-add the first removed path and redraw
     */
    public void redoLast() {

        if (mCanceledPaths.size() > 0) {
            mPaths.add(mCanceledPaths.get(mCanceledPaths.size() - 1));
            mCanceledPaths.remove(mCanceledPaths.size() - 1);
            invalidate();

            notifyRedoUndoCountChanged();
        }
    }

    /**
     * Remove all the paths and redraw (can be undone with {@link #redoLast()})
     */
    public void undoAll() {
        boolean isRemove = false;
        ArrayList<HistoryPath> path = new ArrayList<>();
        for (int i = mPaths.size() - 1; i >= 0; i--) {
            if (mPaths.get(i).isSelfDraw()) {
                path.add(mPaths.get(i));
                mPaths.remove(i);
                isRemove = true;
            }
        }
        if (isRemove) {
            mCanceledPaths.addAll(path);
            invalidate();

            notifyRedoUndoCountChanged();
            mPathDrawnListener.onUndoAll();
        }
    }

    /**
     * Remove all the paths and redraw (can be undone with {@link #redoLast()})
     */
    public void undoOtherAll() {
        boolean isRemove = false;
        for (int i = mPaths.size() - 1; i >= 0; i--) {
            if (!mPaths.get(i).isSelfDraw()) {
                mPaths.remove(i);
                isRemove = true;
            }
        }
        if (isRemove) {
            invalidate();
        }
    }

    /**
     * Re-add all the removed paths and redraw
     */
    public void redoAll() {

        if (mCanceledPaths.size() > 0) {
            mPaths.addAll(mCanceledPaths);
            mCanceledPaths.clear();
            invalidate();

            notifyRedoUndoCountChanged();
        }
    }

    /**
     * Get how many undo operations are available
     */
    public int getUndoCount() {
        int count = 0;
        for (int i = 0; i < mPaths.size(); i++) {
            if (mPaths.get(i).isSelfDraw()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get how many redo operations are available
     */
    public int getRedoCount() {
        return mCanceledPaths.size();
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

    /**
     * Create a Bitmap with the content drawn inside the view
     */
    public void getDrawScreenshot(@NonNull final DrawCreatorListener listener) {
        new TakeScreenShotAsyncTask(listener).execute();
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
            mPathRedoUndoCountChangeListener.onRedoCountChanged(getRedoCount());
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
        for (int i = 0; i < mPaths.size(); i++) {
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
    private void addPoint(int action, ArrayList<android.graphics.Point> points, float x, float y) {
        //if (lastAction != action || lastPoint == null || lastPoint.x != x || lastPoint.y != y) {
        android.graphics.Point point;
        point = new android.graphics.Point();
        point.x = (int) x;
        point.y = (int) y;
        points.add(point);
//            if (action == MotionEvent.ACTION_MOVE) {
//                lastPoint = point;
//            } else {
//                lastPoint = null;
//            }
//            lastAction = action;
//        } else {
//            Log.i(TAG, "point is same " + x + "," + y);
//        }
    }

    android.graphics.Point lastPoint;
    int lastAction;

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        ArrayList<android.graphics.Point> points = new ArrayList<>();
        if ((motionEvent.getAction() != MotionEvent.ACTION_UP) &&
                (motionEvent.getAction() != MotionEvent.ACTION_CANCEL)) {
            for (int i = 0; i < motionEvent.getHistorySize(); i++) {
                addPoint(motionEvent.getAction(), points, motionEvent.getHistoricalX(i), motionEvent.getHistoricalY(i));
            }
            addPoint(motionEvent.getAction(), points, motionEvent.getX(), motionEvent.getY());
            if (points.size() > 0) {
                mPathDrawnListener.onTouch(motionEvent.getAction(), points, (int) mPaintWidth,
                        getPaintColor(), getPaintAlpha());
            }
        } else {
            mPathDrawnListener.onTouch(motionEvent.getAction(), points, (int) mPaintWidth,
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
                (mPaths.size() == 0 && mCanceledPaths.size() == 0)) {
            return;
        }

        if (mResizeBehaviour == ResizeBehaviour.CLEAR) {// If clear, clear all and return
            mPaths.clear();
            mCanceledPaths.clear();
            return;
        } else if (mResizeBehaviour == ResizeBehaviour.CROP) {
            xMultiplyFactor = yMultiplyFactor = 1;
        }

        // Adapt drawn paths
        for (HistoryPath historyPath : mPaths) {

            multiplySinglePath(historyPath, xMultiplyFactor, yMultiplyFactor);
        }

        // Adapt canceled paths
        for (HistoryPath historyPath : mCanceledPaths) {

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

    public interface DrawCreatorListener {
        void onDrawCreated(Bitmap draw);

        void onDrawCreationError();
    }


    class TakeScreenShotAsyncTask extends AsyncTask<Void, Void, Void> {
        private int mWidth, mHeight;
        private Canvas mCanvas;
        private Bitmap mBitmap;
        private DrawCreatorListener mListener;

        public TakeScreenShotAsyncTask(@NonNull DrawCreatorListener listener) {
            mListener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mWidth = getWidth();
            mHeight = getHeight();
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                mBitmap = Bitmap.createBitmap(
                        mWidth, mHeight, Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(mBitmap);
            } catch (Exception e) {
                e.printStackTrace();
                cancel(true);
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            if (mListener != null) {
                mListener.onDrawCreationError();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            draw(mCanvas);

            if (mListener != null) {
                mListener.onDrawCreated(mBitmap);
            }
        }
    }

    public synchronized void onTouch(int selfDraw, int touchEvent, ArrayList<android.graphics.Point> points, int paintWidth,
                                     int paintColor, int paintAlpha, int width, int height) {
        Log.d(TAG, "onTouch() called begin with: selfDraw = [" + selfDraw + "], touchEvent = [" + touchEvent + "]");

        int w = getWidth();
        int h = getHeight();
        // 先把点保存到数组
        ArrayList<Point> pointList = new ArrayList<>();
        if (points.size() > 0) {
            for (int i = 0; i < points.size(); i++) {
                Point point = new Point();
                point.selfDraw = (selfDraw == 1);
                // 转换一下缩放坐标
                point.x = (points.get(i).x * w) / width;
                point.y = (points.get(i).y * h) / height;
                pointList.add(point);
            }
        }
        if (selfDraw == 1) {
            mCurrentPaint.setColor(mPaintColor);
            mCurrentPaint.setAlpha(mPaintAlpha);
            mCurrentPaint.setStrokeWidth(FreeDrawHelper.convertDpToPixels(mPaintWidth));
        } else {
            mCurrentPaint.setColor(paintColor);
            mCurrentPaint.setAlpha(paintAlpha);
            mCurrentPaint.setStrokeWidth(FreeDrawHelper.convertDpToPixels(paintWidth));
        }
        if (selfDraw == 1 && touchEvent == MotionEvent.ACTION_DOWN) {
            notifyPathStart();
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }

        if (touchEvent == MotionEvent.ACTION_DOWN) {
            mDrawPath[selfDraw] = null;
            addPoint(selfDraw, pointList);
        } else if (touchEvent == MotionEvent.ACTION_MOVE) {
            addPoint(selfDraw, pointList);
        } else if (touchEvent == MotionEvent.ACTION_UP) {
            if (pointList.size() > 0) {
                addPoint(selfDraw, pointList);
                mDrawPath[selfDraw] = null;
            }
        }
        invalidate();

        Log.d(TAG, "onTouch() called end with: selfDraw = [" + selfDraw + "], touchEvent = [" + touchEvent + "]");
    }

    private void addPoint(int selfDraw, ArrayList<Point> points) {
        if (mDrawPath[selfDraw] == null) {
            mDrawPath[selfDraw] = new HistoryPath(new SerializablePaint(mCurrentPaint), points, selfDraw == 1);
            mPaths.add(mDrawPath[selfDraw]);
            if (selfDraw == 1) {
                notifyPathDrawn();
                notifyRedoUndoCountChanged();
            }
        } else {
            mDrawPath[selfDraw].addPoint(points);
        }
    }
}
