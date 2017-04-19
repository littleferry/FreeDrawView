package com.rm.freedrawview;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Riccardo Moro on 9/25/2016.
 */

class Point implements Parcelable {
    float x, y;
    boolean selfDraw; // 自己画的还是别人画的

    Point() {
        x = y = -1;
    }

    private Point(Parcel in) {
        x = in.readFloat();
        y = in.readFloat();
    }

    @Override
    public String toString() {
        return "" + x + "," + y;
    }


    // Parcelable stuff
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(x);
        dest.writeFloat(y);
    }

    // Parcelable CREATOR class
    public static final Creator<Point> CREATOR = new Creator<Point>() {
        @Override
        public Point createFromParcel(Parcel in) {
            return new Point(in);
        }

        @Override
        public Point[] newArray(int size) {
            return new Point[size];
        }
    };

}
