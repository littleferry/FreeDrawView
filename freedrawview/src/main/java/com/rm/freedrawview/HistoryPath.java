package com.rm.freedrawview;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * Created by Riccardo Moro on 9/27/2016.
 */

class HistoryPath implements Parcelable {
    private SerializablePath path;
    private SerializablePaint paint;
    private float originX, originY;
    private boolean isSelfDraw;
    private ArrayList<Point> points;

    HistoryPath(@NonNull SerializablePaint paint, ArrayList<Point> points, boolean isSelfDraw) {
        this.paint = paint;
        this.isSelfDraw = isSelfDraw;
        this.points = new ArrayList<>();
        if (points.size() > 0) {
            Point point;
            point = points.get(0);
            this.originX = point.x;
            this.originY = point.y;
            this.points.addAll(points);
            resetPath();
        }
    }

    private HistoryPath(Parcel in) {
        originX = in.readFloat();
        originY = in.readFloat();
        isSelfDraw = in.readByte() != 0;
    }

    public void addPoint(ArrayList<Point> points) {
        if (points.size() > 0) {
            this.points.addAll(points);
            resetPath();
        }
    }

    private void resetPath() {
        path = new SerializablePath();
        boolean first = true;
        ArrayList<Point> mPoints = points;
        for (int i = 0; i < mPoints.size(); i++) {
            Point point = points.get(i);
            if (first) {
                path.moveTo(point.x, point.y);
                first = false;
            } else {
                path.lineTo(point.x, point.y);
            }
        }
    }

    public SerializablePath getPath() {
        return path;
    }

    public void setPath(SerializablePath path) {
        this.path = path;
    }

    public SerializablePaint getPaint() {
        return paint;
    }

    public void setPaint(SerializablePaint paint) {
        this.paint = paint;
    }

    public boolean isPoint() {
        return FreeDrawHelper.isAPoint(points);
    }

    public boolean isSelfDraw() {
        return isSelfDraw;
    }

    public void setSelfDraw(boolean selfDraw) {
        isSelfDraw = selfDraw;
    }

    public float getOriginX() {
        return originX;
    }

    public void setOriginX(float originX) {
        this.originX = originX;
    }

    public float getOriginY() {
        return originY;
    }

    public void setOriginY(float originY) {
        this.originY = originY;
    }

    // Parcelable stuff
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(path);
        dest.writeSerializable(paint);
        dest.writeFloat(originX);
        dest.writeFloat(originY);
        dest.writeByte((byte) (isSelfDraw ? 1 : 0));
    }

    // Parcelable CREATOR class
    public static final Creator<HistoryPath> CREATOR = new Creator<HistoryPath>() {
        @Override
        public HistoryPath createFromParcel(Parcel in) {
            return new HistoryPath(in);
        }

        @Override
        public HistoryPath[] newArray(int size) {
            return new HistoryPath[size];
        }
    };
}
