package com.sizeof.sizeof;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * Created by adamwalsh on 22/04/15.
 */
public class TracableView extends View {
    private Point orig, other;

    public TracableView(Context context) {
        super(context);
    }

    public TracableView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void setOrig(Point orig) {
        if (orig == null)
            this.orig = null;
        else
            this.orig = convertToRelativePoint(orig);
    }

    /*
     * Convert a point from raw x and y values (for the entire display) to
     * the x and y values that match said x and y
     * values inside this view, i.e. convert from
     * raw x,y of whole screen to x,y for this
     * view element.
     */
    private Point convertToRelativePoint(Point absPoint) {
        int[] loc = new int[2];
        this.getLocationOnScreen(loc);//get this view's location offset values
        return new Point(absPoint.x - loc[0], absPoint.y - loc[1]);
    }

    public void setOrig(int x, int y) {
        this.orig = convertToRelativePoint(new Point(x, y));
    }

    public void setOther(Point other) {
        if (other == null)
            this.other = null;
        else
            this.other = convertToRelativePoint(other);
    }

    public void setOther(int x, int y) {
        this.other = convertToRelativePoint(new Point(x, y));
    }

    public void printPoints() {
        if (this.orig != null)
            System.out.println(this.orig.toString());
        if (this.other != null)
            System.out.println(this.other.toString());
    }

    public void forceRedraw() {
        this.postInvalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.orig != null && this.other != null) {
            //Canvas canvas = this.getHolder().lockCanvas();
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(3);
            int left = Math.min(this.orig.x, this.other.x);
            int right = left == this.orig.x ? this.other.x : this.orig.x;
            int top = Math.min(this.orig.y, this.other.y);
            int bottom = top == this.orig.y ? this.other.y : this.orig.y;
            canvas.drawRect(left, top, right, bottom, paint);
            String locStr = "width: " + ((Integer)(right-left)).toString() + "\nheight: " +
                    ((Integer)(bottom-top)).toString();
            Paint textPaint = new Paint();
            textPaint.setColor(Color.CYAN);
            textPaint.setTextSize(32);
            canvas.drawText(locStr, left+1, top+1, textPaint);
            //this.getHolder().unlockCanvasAndPost(canvas);
        }
    }
}
