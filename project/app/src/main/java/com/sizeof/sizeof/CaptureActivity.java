package com.sizeof.sizeof;

import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class CaptureActivity extends ActionBarActivity implements SurfaceHolder.Callback {
    private final double[] DROID_MAXX_INF_FOCUS_DISTS = {1.218732, 2.043917, 6.329597};
    private Camera cam;
    private SurfaceHolder holder;
    private Point displaySize;
    private OutputStreamWriter osw;
    private FileOutputStream fOut;

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
        this.displaySize = new Point();
        this.getWindowManager().getDefaultDisplay().getSize(this.displaySize);
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
        try {
            if (this.fOut == null)
                this.fOut = openFileOutput("DROID_MAXX.txt", MODE_PRIVATE);
            if (this.osw == null)
                this.osw = new OutputStreamWriter(this.fOut);
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
        this.cam = Camera.open();
        this.cam.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int error, Camera camera) {
                if (error == 100) {//media server died
                    cam.release();
                    cam = Camera.open();//re-open
                }
            }
        });
        if (this.cam == null) {
            System.out.println("Camera opening returned null! D:");
            return;//TODO
        }
        Camera.Parameters camSettings = this.cam.getParameters();
        List<Camera.Size> sizes = camSettings.getSupportedPreviewSizes();
        Camera.Size optimal = getOptimalPreviewSize(sizes, this.displaySize.x, this.displaySize.y);
        camSettings.setPreviewSize(optimal.width, optimal.height);
        camSettings.setFocusAreas(new ArrayList<Camera.Area>());//no focus areas to start
        this.holder.setFixedSize(optimal.width, optimal.height);
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
        this.cleanUpFileOutput();
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
        //do nothing
    }

    @Override
    public void surfaceChanged(final SurfaceHolder surfaceHolder, final int format, final int width, final int height) {
        if (this.holder.getSurface() == null) {
            System.out.println("Null surface D:");
            return;
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
//            if (this.cam.getParameters().getFocusMode())
            Camera.Parameters camSettings = this.cam.getParameters();
            camSettings.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            int x = (int) event.getX();
            int y = (int) event.getY();
            int width = 125;//250px X 250px focus area
            int left = (int)(((double)x / this.displaySize.x) * 2000 - 1000) - width;//scale to [-1000,1000]
            int top = (int)(((double)y / this.displaySize.y) * 2000 - 1000) - width;//see SDK 21 Camera.setFocusAreas
            int right = left + 2 * width;
            int bottom = top + 2 * width;
            Camera.Area focusArea = new Camera.Area(new Rect(left, top, right, bottom), 1000);
            ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
            focusAreas.add(focusArea);
            camSettings.setFocusAreas(focusAreas);
            this.cam.setParameters(camSettings);
            this.cam.autoFocus(null);//apply focus area
            float[] floats = new float[3];
            this.cam.getParameters().getFocusDistances(floats);
            if (!floats.equals(DROID_MAXX_INF_FOCUS_DISTS))//not inf focus distances
                this.writeToOutput(floats[0] + ", "+floats[1] + ", "+floats[2]);//simple readable CSV
            else
                System.out.println("infinity or not focused");
        }
        return true;
    }

    private void writeToOutput(String message) {
        try {
            this.osw.write(message);
        }
        catch(IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void cleanUpFileOutput() {
        try {
            if (this.osw != null) {
                this.osw.flush();
                this.osw.close();
            }
            if (this.fOut != null) {
                this.fOut.flush();
                this.fOut.close();
            }
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
        finally {
            this.osw = null;//reset to null to mark as
            this.fOut = null;//closed for re-open next onResume()
        }
    }
}
