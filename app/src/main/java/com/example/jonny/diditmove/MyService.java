package com.example.jonny.diditmove;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.example.jonny.diditmove.MyServiceTask.ResultCallback;

import java.util.Date;

public class MyService extends Service {

    private static final String LOG_TAG = "MyService";

    // Handle to notification manager.
    private NotificationManager notificationManager;
    private int ONGOING_NOTIFICATION_ID = 1; // This cannot be 0. So 1 is a good candidate.

    //power manager

    private PowerManager.WakeLock wakeLock;
    // Motion detector thread and runnable.
    private Thread myThread;
    private MyServiceTask myTask;
    private Notification notification;

    private boolean timeRecorded;
    public  Date T0;
    public Date firstAccel;
    public Date T1;
    // Binder given to clients
    private final IBinder myBinder = new MyBinder();

    // Binder class.
    public class MyBinder extends Binder {
        MyService getService() {
            // Returns the underlying service.
            return MyService.this;
        }
    }

    public MyService() {

    }

    @Override
    public void onCreate() {

        Log.i(LOG_TAG, "Service is being created");

        // Display a notification about us starting.  We put an icon in the status bar.
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //showMyNotification(this);

        // Creates the thread running the camera service.
        myTask = new MyServiceTask(getApplicationContext());
        myThread = new Thread(myTask);
        myThread.start();

        //sensor call
        boolean senseMotion =((SensorManager) getSystemService(Context.SENSOR_SERVICE)).registerListener(
                new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        //check the x and y movement
                        if((event.values[0] < -1 || event.values[0] > 1) || (event.values[1] < -1 || event.values[1] > 1)){
                            T1 = new Date();
                            //If it has been 30 or more seconds since the app was started, note movement
                            if((T1.getTime() - T0.getTime()) / 1000 > 30 && !timeRecorded){
                                firstAccel = T1;
                                timeRecorded = true;
                                Log.i("LOG_TAG","Acceleration  changed");
                            }
                        }

                    }
                    @Override
                    //do nothing here
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
                },
                ((SensorManager)getSystemService(Context.SENSOR_SERVICE))
                        .getSensorList(Sensor.TYPE_ACCELEROMETER).get(0), SensorManager.SENSOR_DELAY_GAME);

        T0 = new Date();
        firstAccel = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG, "Service is being bound");
        // Returns the binder to this service.
        return myBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(LOG_TAG, "Received start id " + startId + ": " + intent);
        // We start the task thread.
        if (!myThread.isAlive())
        {
            Log.i(LOG_TAG, "ALIVE: " + myThread.isAlive());
            myThread.start();
        }
        //manage the phone power
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock= powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        notificationManager.cancel(ONGOING_NOTIFICATION_ID);
        Log.i(LOG_TAG, "Stopping.");
        //Stops the motion detector.
        myTask.stopProcessing();
        Log.i(LOG_TAG, "Stopped.");

        //wakeLock manager
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock= powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");

        //code to release wakeLock
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    // Interface to be able to subscribe to the bitmaps by the service.

    public void releaseResult(ServiceResult result) {
        myTask.releaseResult(result);
    }

    public void addResultCallback(ResultCallback resultCallback) {
        myTask.addResultCallback(resultCallback);
    }

    public void removeResultCallback(ResultCallback resultCallback) {
        myTask.removeResultCallback(resultCallback);
    }

    // Interface which sets recording on/off.
    public void setTaskState(boolean b) {
        myTask.setTaskState(b);
    }


    /**
     * Show a notification while this service is running.
     */
    @SuppressWarnings("deprecation")
    private void showMyNotification(Context mContext) {

        // Creates a notification.
        //Notification notification = new Notification(
          //      R.mipmap.ic_launcher,
            //    getString(R.string.my_service_started),
              //  System.currentTimeMillis());
        int icon = R.mipmap.ic_launcher;
        String ns = Context.NOTIFICATION_SERVICE;
        CharSequence tickerText = "Fall Detector";
        long when = System.currentTimeMillis();
        notification = new Notification(icon, tickerText, when);
        notificationManager = (NotificationManager) getSystemService(ns);



        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification.Builder myNoti = new Notification.Builder(mContext)

                .setContentTitle("Fall Detector")
                .setContentText("Running")
                .setContentIntent(pendingIntent);
        myNoti.build();
        Notification notification = myNoti.getNotification();

        //notificationManager.notify(1, notification);


    //    startForeground(ONGOING_NOTIFICATION_ID, notification);
    }
    //This destroys the service in main
    public void clear (){
        Date d = new Date();
        T0 = d;
        firstAccel= null;
        timeRecorded = false;
    }
   //call by the main activity
    public boolean didItMove(){
        timeRecorded = false;
        return myTask.didItMove(firstAccel);
    }


}
