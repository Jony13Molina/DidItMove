package com.example.jonny.diditmove;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import static android.os.Looper.getMainLooper;


public class MainActivity extends Activity implements
        com.example.jonny.diditmove.MyServiceTask.ResultCallback, View.OnClickListener  {

    public static final int DISPLAY_NUMBER = 10;
    private Handler mUiHandler;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Rect mSurfaceSize;

    Button clearButton;
    Button exitButton;
    private static final String LOG_TAG = "MainActivity";

    // Service connection variables.
    private boolean serviceBound;
    private MyService myService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUiHandler = new Handler(getMainLooper(), new UiCallback());
        serviceBound = false;
        // Prevents the screen from dimming and going to sleep.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        clearButton = findViewById(R.id.ClearB);
        clearButton.setOnClickListener(this);
        exitButton = findViewById(R.id.FinishB);
        exitButton.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Starts the service, so that the service will only stop when explicitly stopped.
        Intent intent = new Intent(this, MyService.class);
        startService(intent);
        bindMyService();

        TextView myMessage = (TextView) findViewById(R.id.number_view);
        //If it moved, update textview
        if (myService != null) {
            if (myService.didItMove()) {
                Log.i(LOG_TAG, "CHECK AND MOVED");
                myMessage.setText("The phone was moved!");
            } else {
                Log.i(LOG_TAG, "CHECK AND STATIONARY");
                myMessage.setText("The phone was not moved");
            }
        }
    }

    private void bindMyService() {
        // We are ready to show images, and we should start getting the bitmaps
        // from the motion detection service.
        // Binds to the service.
        Log.i(LOG_TAG, "Starting the service");
        Intent intent = new Intent(this, MyService.class);
        Log.i("LOG_TAG", "Trying to bind");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    // Service connection code.
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
            // We have bound to the camera service.
            MyService.MyBinder binder = (MyService.MyBinder) serviceBinder;
            myService = binder.getService();
            serviceBound = true;
            // Let's connect the callbacks.
            Log.i("MyService", "Bound succeeded, adding the callback");
            myService.addResultCallback(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            serviceBound = false;
        }
    };

    @Override
    protected void onPause() {
        if (serviceBound) {
            if (myService != null) {
                myService.removeResultCallback(this);
            }
            Log.i("MyService", "Unbinding");
            unbindService(serviceConnection);
            serviceBound = false;
            // If we like, stops the service.
        }
        super.onPause();
    }

    /**
     * This function is called from the service thread.  To process this, we need
     * to create a message for a handler in the UI thread.
     */
    @Override
    public void onResultReady(ServiceResult result) {
        if (result != null) {
            Log.i(LOG_TAG, "Preparing a message for " + result.intValue);
        } else {
            Log.e(LOG_TAG, "Received an empty result!");
        }
        mUiHandler.obtainMessage(DISPLAY_NUMBER, result).sendToTarget();
    }
//method to handle buttons
    @Override
    public void onClick(View v)
    {
        switch(v.getId())
        {
            case R.id.FinishB:
                if (serviceBound) {
                    if (myService != null) {
                        myService.removeResultCallback(this);
                    }
                    //Unbinds our service
                    Log.i("MyService", "Unbinding");
                    unbindService(serviceConnection);
                    serviceBound = false;

                    //this stops pur service
                    Log.i(LOG_TAG, "Stopping.");
                    Intent intent = new Intent(this, MyService.class);
                    stopService(intent);
                    Log.i(LOG_TAG, "Stopped.");
                    finish();
                }
                break;
            case R.id.ClearB:
                myService.clear();
                break;
        }
    }

    /**
     * This Handler callback gets the message generated above.
     * It is used to display the integer on the screen.
     */
    private class UiCallback implements Handler.Callback {
        @SuppressLint("SetTextI18n")
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == DISPLAY_NUMBER) {
                // Gets the result.
                ServiceResult result = (ServiceResult) message.obj;
                // Displays it.
                if (result != null) {
                    Log.i(LOG_TAG, "Displaying: " + result.intValue);
                    TextView tv = (TextView) findViewById(R.id.number_view);
                    tv.setText(Boolean.toString(result.intValue));
                 if(myService.didItMove())
                    {
                        tv.setText("Phone Was Moved");
                    }
                    else{
                        tv.setText("Phone Did Not Moved");
                    }
                    if (serviceBound && myService != null) {
                        Log.i(LOG_TAG, "Releasing result holder for " + result.intValue);
                        myService.releaseResult(result);
                    }
                } else {
                    Log.e(LOG_TAG, "Error: received empty message!");
                }
            }
            return true;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
}
