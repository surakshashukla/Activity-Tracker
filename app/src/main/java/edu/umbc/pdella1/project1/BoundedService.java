package edu.umbc.pdella1.project1;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class BoundedService extends Service implements SensorEventListener{
    private static String LOG_TAG = "BoundService";
    private IBinder mBinder = new MyBinder();
    float lastAcclx = 0;
    float lastAccly = 0;
    float lastAcclz = 0;
    boolean shouldContinue = true;
    float xSum = 0;
    float ySum = 0;
    float zSum = 0;
    int count = 0;
    long timeInit = 0;
    Timer timer;
    float ALPHA = 0.158f; //low pass coefficient
    float BETA = 0.9843f;
    float yValSum = 0;
    float yMax = 0;
    int timeLength = 120000;
    TimerTask timerTask2;
    private float xValue, yValue, zValue;
    private String timeActivityItem;
    private List<String> listActivity = new LinkedList<String>();
    private SensorManager sensormanager_;
    private Sensor accelerometer_;
    private final int DELAY = 100;
    private Handler myHandler = new Handler();


    public BoundedService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(LOG_TAG, "in onCreate");

        //Create sensormanager
        sensormanager_ = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer_ = sensormanager_.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensormanager_.registerListener(this, accelerometer_, SensorManager.SENSOR_DELAY_NORMAL, DELAY);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(LOG_TAG, "in onBind");
        sensormanager_.registerListener(this, accelerometer_, SensorManager.SENSOR_DELAY_NORMAL, DELAY);

        //Start new timer task
        timer = new Timer();
        timerTask2 = new doAsynchTask();
        timeInit = System.currentTimeMillis();
        timer.scheduleAtFixedRate(timerTask2, timeLength, timeLength);

        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(LOG_TAG, "in onRebind");
        super.onRebind(intent);
        sensormanager_.registerListener(this, accelerometer_, SensorManager.SENSOR_DELAY_NORMAL, DELAY);

        //Start new timer task
        timer = new Timer();
        timerTask2 = new doAsynchTask();
        timeInit = System.currentTimeMillis();
        timer.scheduleAtFixedRate(timerTask2, timeLength, timeLength);


    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(LOG_TAG, "in onUnbind");
        stopTimerTask();
        sensormanager_.unregisterListener(this, accelerometer_);

        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "in onDestroy");
        sensormanager_.unregisterListener(this, accelerometer_);
        stopTimerTask();
        shouldContinue = false;

    }


    private class AcclWork implements Runnable {

        private SensorEvent event_;

        public AcclWork(SensorEvent event) {
            event_ = event;
        }

        @Override
        public void run() {

            //Always running
            //Accelerometer data
            xValue = event_.values[0];
            yValue = event_.values[1];
            zValue = event_.values[2];

            //Applying low pass filter
            float xVal = (ALPHA * xValue) + (BETA*lastAcclx); //ADC[n] = a1A[n]+b1ADC[n−1], where ADC is the filtered output data and A is the raw input data
            float yVal = ALPHA * yValue + BETA*lastAccly;
            float zVal = ALPHA * zValue + BETA*lastAcclz;

            if(yVal > yMax)
            {
                yMax = yVal;
            }
            yValSum += yVal;

            double dX = Math.abs((lastAcclx) - (xVal));
            double dY = Math.abs((lastAccly) - (yVal));
            double dZ = Math.abs((lastAcclz) - (zVal));

            xSum += dX;
            ySum += dY;
            zSum += dZ;
            count += 1;

            lastAcclx = xValue;
            lastAccly = yValue;
            lastAcclz = zValue;

        }
    }

    private class doAsynchTask extends TimerTask{

        @Override
        public void run() {
            myHandler.post(new Runnable()  {
                @SuppressWarnings("unchecked")
                public void run() {
                    try {

                        String activity = "No activity";

                        float xSumVal = xSum;
                        float ySumVal = ySum;
                        float zSumVal = zSum;
                        float countVal = count;
                        float yValSumVal = yValSum;
                        float yMaxVal = yMax;
                        long timeEndVal = System.currentTimeMillis();
                        long timeInitVal = timeInit;

                        xSum = 0;
                        ySum = 0;
                        zSum = 0;
                        count = 0;
                        yMax = 0;
                        yValSum = 0;
                        timeInit = timeEndVal;

                        float xAvg = xSumVal / countVal;
                        float yAvg = ySumVal / countVal;
                        float zAvg = zSumVal / countVal;
                        float yValAvg = yValSumVal / countVal;
                        Date intTime = new Date(timeInitVal);
                        Date endTime = new Date(timeEndVal);
                        DateFormat formatter = new SimpleDateFormat("hh:mm a");

                        String initFormat = formatter.format(intTime);
                        String endFormat = formatter.format(endTime);

                        if (xAvg < 2 && yAvg < 2 && zAvg < 2){
                            if (yMaxVal > 14) //  || yValAvg > 14)
                            {
                                activity = "Walking/Running";
                            }
                            else if (yValAvg > 8)
                            {
                                activity = "Sitting";
                            }
                            else {
                                activity = "Sleeping";
                            }
                        }
                        else if ( yMaxVal > 14 ) //|| yValAvg > 14 )
                        {
                            activity = "Walking/Running";
                        }
                        else
                        {
                            activity = "Sitting";
                        }

                        /*if ((xAvg < .20) && (yAvg < .20) && ((1 < zAvg ) && (zAvg < 1.6))) {
                            activity = "Sleeping";
                        } else if ((xAvg < .20) && (yAvg > 1) && (zAvg < 1) && (yMaxVal < 14)) {
                            activity = "Sitting";
                        } else if ((xAvg > .15) && (yAvg > 1) && (yMaxVal > 14)) {
                            activity = "Walking/Running";
                        } else {
                            activity = "Sitting";
                        }*/

                        timeActivityItem = initFormat + " - " + endFormat + " " + activity;

                        //Broadcast to MainActivity the new activity item
                        Intent intent = new Intent();
                        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        intent.setAction("GetData");
                        sendBroadcast(intent);

                    }
                    catch (Exception e) {

                    }
                }

            });
        }
    }

    public void stopTimerTask() {
        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * Main Activity can grab the new activity item from the service
     * @return A list activity item
     */
    public String getTimeActivityItem() {
        return timeActivityItem;
    }


    public class MyBinder extends Binder {
        BoundedService getService() {
            return BoundedService.this;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
        if(mySensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            myHandler.postDelayed(new AcclWork(event), 10);

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
