 package com.example.jonny.diditmove;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.sql.Time;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by jonny on 11/28/2017.
 */
    public class MyServiceTask implements Runnable {


        public static final String LOG_TAG = "MyService";
        private boolean running;
        private Context context;
        MyService theService;


        //most of this are variables that were declared in order to make
        // the service task communication
        private Set<ResultCallback> resultCallbacks = Collections.synchronizedSet(
                new HashSet<ResultCallback>());
        private ConcurrentLinkedQueue<ServiceResult> freeResults =
                new ConcurrentLinkedQueue<ServiceResult>();


        @Override
        public void run()
        {
            running = true;
            while (running)
            {
                // Sleep a tiny bit.
                try
                {
                Thread.sleep(1000);
                } catch (Exception e)
                {
                e.getLocalizedMessage();
            }
            //Set Acceleration
                Boolean answer = false;
            // Sends it to the UI thread in MainActivity (if MainActivity
            // is running).
                notifyResultCallback(answer);
        }
    }
        //did it move function

        public boolean didItMove(Date T1Accel)
        {
            boolean moved = false;
            Date currTime = new Date();
            //synchronizes our thread
            synchronized (this)
            {
                //check to see if phone was moved during the time span
                //if true return true
                if( T1Accel!= null && (currTime.getTime()- T1Accel.getTime()/100)>30)
                {
                  moved = true;
                }
                return moved;
            }

        }
        public MyServiceTask(Context _context)
        {
            context = _context;
            // Put here what to do at creation.
        }



        public void addResultCallback(ResultCallback resultCallback)
        {
            Log.i(LOG_TAG, "Adding result callback");
            resultCallbacks.add(resultCallback);
        }

        public void removeResultCallback(ResultCallback resultCallback)
        {
            Log.i(LOG_TAG, "Removing result callback");
            // We remove the callback...
            resultCallbacks.remove(resultCallback);
            // ...and we clear the list of results.
            // Note that this works because, even though mResultCallbacks is a synchronized set,
            // its cardinality should always be 0 or 1 -- never more than that.
            // We have one viewer only.
            // We clear the buffer, because some result may never be returned to the
            // free buffer, so using a new set upon reattachment is important to avoid
            // leaks.
            freeResults.clear();
        }

        // Creates result bitmaps if they are needed.
        private void createResultsBuffer()
        {
            // I create some results to talk to the callback, so we can reuse these instead of creating new ones.
            // The list is synchronized, because integers are filled in the service thread,
            // and returned to the free pool from the UI thread.
            freeResults.clear();
            for (int i = 0; i < 10; i++)
            {
                freeResults.offer(new ServiceResult());
            }
        }

        // This is called by the UI thread to return a result to the free pool.
        public void releaseResult(ServiceResult r)
        {
            Log.i(LOG_TAG, "Freeing result holder for " + r.intValue);
            freeResults.offer(r);
        }

        public void stopProcessing()
        {
            running = false;
        }

        public void setTaskState(boolean b)
        {
            // Do something with b.
        }

        /**
         * Call this function to return the integer i to the activity.
         * @param
         */
        private void notifyResultCallback(boolean answer)
        {
            if (!resultCallbacks.isEmpty())
            {
                // If we have no free result holders in the buffer, then we need to create them.
                if (freeResults.isEmpty())
                {
                    createResultsBuffer();
                }
                ServiceResult result = freeResults.poll();
                // If we got a null result, we have no more space in the buffer,
                // and we simply drop the integer, rather than sending it back.
                if (result != null) {
                    result.intValue = answer;
                    for (ResultCallback resultCallback : resultCallbacks) {
                        Log.i(LOG_TAG, "calling resultCallback for " + result.intValue);
                        resultCallback.onResultReady(result);
                    }
                }
            }
        }
        public interface ResultCallback
        {
            void onResultReady(ServiceResult result);

        }


    }



