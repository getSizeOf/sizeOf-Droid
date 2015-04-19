package com.sizeof.sizeof;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

import static android.view.View.resolveSize;


public class CaptureActivity extends ActionBarActivity implements SurfaceHolder.Callback {
    private Camera cam;
    private SurfaceHolder holder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //set content view AFTER ABOVE sequence (to avoid crash)
        this.setContentView(R.layout.activity_capture);
        SurfaceView view = (SurfaceView)(findViewById(R.id.surfaceView));
        this.holder = view.getHolder();//FIXME null check
        this.holder.addCallback(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_capture, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.cam = Camera.open();
        this.cam.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int error, Camera camera) {
                if (error == 100) {//media server died
                    cam.release();
                    cam = Camera.open();
                }
            }
        });
        if (this.cam == null) {
            System.out.println("Camera opening returned null! D:");
            return;//TODO
        }
        Camera.Parameters camSettings = this.cam.getParameters();
        List<Camera.Size> sizes = camSettings.getSupportedPreviewSizes();
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        Camera.Size optimal = getOptimalPreviewSize(sizes, size.x, size.y);
        camSettings.setPreviewSize(optimal.width, optimal.height);
        this.holder.setFixedSize(optimal.width, optimal.height);
        if (camSettings.isSmoothZoomSupported())
            this.cam.startSmoothZoom(0);
        else
            if (camSettings.isZoomSupported())
                camSettings.setZoom(0);//start zoomed out
            else
                System.out.println("Zoom Not Supported! D:");
        this.cam.setDisplayOrientation(90);
        this.cam.setParameters(camSettings);//apply settings
        try {
            this.cam.setPreviewDisplay(this.holder);
            this.cam.startPreview();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double)h / w;
        System.out.println(w);

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void onPause() {
        super.onPause();
        this.cam.stopPreview();
        if (this.cam != null)
            this.cam.release();
    }

    @Override
    public void surfaceCreated(final SurfaceHolder surfaceHolder) {
        try {
            this.cam.setPreviewDisplay(surfaceHolder);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder surfaceHolder) {
        // ...
    }

    @Override
    public void surfaceChanged(final SurfaceHolder surfaceHolder, final int format, final int width, final int height) {
        if (this.holder.getSurface() == null) {
            System.out.println("Null surface D:");
            return ;
        }

        try {
            this.cam.stopPreview();
            this.cam.setPreviewDisplay(this.holder);
            this.cam.startPreview();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
