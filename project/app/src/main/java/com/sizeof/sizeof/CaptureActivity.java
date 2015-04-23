package com.sizeof.sizeof;

import android.content.Intent;
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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CaptureActivity extends ActionBarActivity implements SurfaceHolder.Callback {
    private final double[] DROID_MAXX_INF_FOCUS_DISTS = {1.218732, 2.043917, 6.329597};
    private Camera cam;
    private SurfaceView surfaceView;
    private TracableView tracableView;
    private Point displaySize;
    private FileWriter fileWriter;
    private File file;
    private Button capture_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        //calibration data output file
        this.file = new File(this.getExternalFilesDir(null).getAbsolutePath() + "/dataset.csv");
        int i = 0;
        while(this.file.exists())
            this.file = new File(this.getExternalFilesDir(null).getAbsolutePath() + "/dataset_" +
                    i++ + ".csv");//increment i after creating File obj
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //set content view AFTER ABOVE sequence (to avoid crash)
        this.setContentView(R.layout.activity_capture);
        final Button button = (Button)findViewById(R.id.capture_data);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeFocalData();
            }
        });
        capture_image = (Button) findViewById(R.id.capture_image);
        capture_image.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (cam != null)
                    capture();
            }
        });
        this.tracableView = (TracableView)findViewById(R.id.drawView);
        this.tracableView.setWillNotDraw(false);
        this.surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        SurfaceHolder surfHolder = this.surfaceView.getHolder();
        surfHolder.addCallback(this);
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
            if (this.fileWriter == null)
                this.fileWriter = new FileWriter(this.file);
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
        Camera.Size max = null;
        for (Camera.Size size : this.cam.getParameters().getSupportedPictureSizes())
            if (max == null)
                max = size;
            else if (max.height * max.width < size.height * size.width)
                max = size;
        Camera.Parameters camSettings = this.cam.getParameters();
        camSettings.setPictureSize(max.width, max.height);
        List<Camera.Size> sizes = camSettings.getSupportedPreviewSizes();
        Camera.Size optimal = getOptimalPreviewSize(sizes, this.displaySize.x, this.displaySize.y);
        camSettings.setPreviewSize(optimal.width, optimal.height);
        camSettings.setFocusAreas(new ArrayList<Camera.Area>());//reset focus areas
        this.surfaceView.getHolder().setFixedSize(optimal.width, optimal.height);
        this.cam.setDisplayOrientation(90);//portrait
        this.cam.setParameters(camSettings);//apply settings
        try {
            this.cam.setPreviewDisplay(this.surfaceView.getHolder());
            this.cam.startPreview();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double)h / w;

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
        if (this.cam != null) {
            this.cam.stopPreview();
            this.cam.release();
        }
        this.cleanUpFileOutput();
    }

    @Override
    public void surfaceCreated(final SurfaceHolder surfaceHolder) {
        try {
            this.cam.stopPreview();
            this.cam.setPreviewDisplay(this.surfaceView.getHolder());
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
        if (this.surfaceView == null) {
            System.out.println("Null surface D:");
            return;
        }
        try {
            this.cam.stopPreview();
            this.cam.setPreviewDisplay(this.surfaceView.getHolder());
            this.cam.startPreview();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {//focus on tapped area, clean away selection box
            this.tracableView.setOrig(null);
            this.tracableView.setOther(null);
            if (this.cam != null) {
                Camera.Parameters camSettings = this.cam.getParameters();
                camSettings.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                int x = (int) event.getRawX();
                int y = (int) event.getRawY();
                int width = 125;//250px X 250px focus area
                int left = (int) (((double) x / this.displaySize.x) * 2000 - 1000) - width;//scale to [-1000,1000]
                int top = (int) (((double) y / this.displaySize.y) * 2000 - 1000) - width;//see SDK 21 Camera.setFocusAreas
                int right = left + 2 * width;
                int bottom = top + 2 * width;
                Camera.Area focusArea = new Camera.Area(new Rect(left, top, right, bottom), 1000);
                ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
                focusAreas.add(focusArea);
                camSettings.setFocusAreas(focusAreas);
                try {
                    this.cam.setParameters(camSettings);
                }
                catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                }
                this.cam.autoFocus(null);//apply focus area
            }
        }
        else if (action == MotionEvent.ACTION_MOVE) {//draw selection box
            if (!this.tracableView.isOrigSet()) {
                this.tracableView.setOrig((int)event.getRawX(), (int)event.getRawY());
                this.tracableView.setOther(null);
            }
            else {
                this.tracableView.setOther((int) event.getRawX(), (int) event.getRawY());
            }
        }
        else if (action == MotionEvent.ACTION_UP) {//calculate size and report to user

        }
        this.tracableView.forceRedraw();
        return true;
    }

    private void capture() {
        this.cam.takePicture(null, null, null, new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Toast.makeText(getApplicationContext(), "Picture Taken",
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.putExtra("image_arr", data);
                setResult(RESULT_OK, intent);
                cam.stopPreview();
                if (cam != null) {
                    cam.release();
                    cam = null;
                }
            }
        });
    }

    private void writeFocalData() {
        float[] floats = new float[3];
        this.cam.getParameters().getFocusDistances(floats);
        if (!floats.equals(DROID_MAXX_INF_FOCUS_DISTS)) {//not inf focus distances
            Toast.makeText(getApplicationContext(), "Data Point Recorded", Toast.LENGTH_SHORT).show();
            this.writeToOutput(floats[0] + "," + floats[1] + "," + floats[2]);//simple readable CSV
        }
        else
            Toast.makeText(getApplicationContext(), "Object Not In Range / Out of Focus",
                    Toast.LENGTH_SHORT).show();
    }

    private void writeToOutput(String message) {
        try {
            this.fileWriter.write(message);
            this.fileWriter.write('\n');
            this.fileWriter.flush();
        }
        catch(IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void cleanUpFileOutput() {
        try {
            if (this.fileWriter != null)
                this.fileWriter.close();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
        finally {
            this.fileWriter = null;//reset to null to mark as closed
        }
    }
}
