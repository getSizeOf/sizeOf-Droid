package com.sizeof.sizeof;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;


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
        if (this.cam == null)
            return;//TODO
        try {
            this.cam.setPreviewDisplay(this.holder);
            this.cam.startPreview();
            System.out.println("started");
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
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
            // ...
        }
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder surfaceHolder) {
        // ...
    }

    @Override
    public void surfaceChanged(final SurfaceHolder surfaceHolder, final int format, final int width, final int height) {
        if (this.holder.getSurface() == null) {
            // ...
            return ;
        }

        try {
            this.cam.stopPreview();
            // ...
            this.cam.setPreviewDisplay(this.holder);
            this.cam.startPreview();
        }
        catch (Exception e) {
            // ...
        }
    }
}
