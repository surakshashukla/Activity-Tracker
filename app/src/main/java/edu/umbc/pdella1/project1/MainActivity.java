package edu.umbc.pdella1.project1;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import edu.umbc.pdella1.project1.BoundedService.MyBinder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static String LOG_TAG = "MainActivity";
    private static int maxActivities = 10;
    BoundedService mBoundService;
    boolean mServiceBound = false;
    TextView textViewOut;
    String filename = "fileActivity.txt";
    long startingTime;
    private List<String> listAct = new LinkedList<String>();
    PrintTimerTask receiver;
    Button startServiceButton, stopServiceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Force Screen to be in portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        startServiceButton = (Button)findViewById(R.id.start_service);
        startServiceButton.setOnClickListener(this);

        stopServiceButton = (Button)findViewById(R.id.stop_service);
        stopServiceButton.setOnClickListener(this);


        //This is where the list of activities is displayed
        textViewOut = (TextView) findViewById(R.id.textView_fileOut);
        textViewOut.setVisibility(View.INVISIBLE);

        //Read in activity file (if it exists)
        try {
            StringBuffer stringBuffer = new StringBuffer();
            String aDataRow = "";
            String aBuffer = "";
            File root = android.os.Environment.getExternalStorageDirectory();

            File myFile = new File(root, filename);
            if (!myFile.exists()) {
                myFile.createNewFile();
            }
            else {

                FileInputStream fIn = new FileInputStream(myFile);
                BufferedReader myReader = new BufferedReader(
                        new InputStreamReader(fIn));

                while ((aDataRow = myReader.readLine()) != null) {
                    aBuffer += aDataRow + "\n";
                    listAct.add(aDataRow);
                }

                textViewOut.setText(aBuffer);
                textViewOut.setVisibility(View.VISIBLE);
                myReader.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyBinder myBinder = (BoundedService.MyBinder) service;
            mBoundService = myBinder.getService();
            mServiceBound = true;
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.start_service:
                if (!mServiceBound)
                {
                    //Force screen on so that the accelerometer can run to get data
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    //Create ability for Activity to get updates from Service
                    IntentFilter filter = new IntentFilter("GetData");
                    receiver = new PrintTimerTask();
                    registerReceiver(receiver, filter);

                    //Bind Service
                    Intent intent = new Intent(MainActivity.this, BoundedService.class);
                    startingTime = System.currentTimeMillis();
                    startService(intent);
                    bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

                    //Display start toast
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(getApplicationContext(), "Service Started", duration);
                    toast.show();

                }
                break;

            case R.id.stop_service:
                if (mServiceBound) {
                    //Unbind service
                    unbindService(mServiceConnection);
                    mServiceBound = false;

                    //Stop the service
                    Intent intent = new Intent(MainActivity.this,
                            BoundedService.class);
                    stopService(intent);
                    unregisterReceiver(receiver);

                    //Toast the user that the service has stopped
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(getApplicationContext(), "Service Stopped", duration);
                    toast.show();

                    //Release lock for keeping the screen on
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                break;
        }

    }


    /**
     * This runs when Bounded service broadcasts that it has new data (every 2 minutes)
     */
    private class PrintTimerTask extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            listAct.add(mBoundService.getTimeActivityItem());

            while (listAct.size() > maxActivities) {
                listAct.remove(0);
            }

            if (listAct.size() == 0) {
                //show the toast.
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(getApplicationContext(), "No data to display", duration);
                toast.show();
            } else
            {

                String state = Environment.getExternalStorageState();

                //Write updated list to file
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    // Can read and write the media
                    Log.d(LOG_TAG, "SD card writable");
                    File root = android.os.Environment.getExternalStorageDirectory();

                    File myFile = new File(root, filename);

                    try {
                        if (!myFile.exists()) {
                            myFile.createNewFile();
                        }
                        FileOutputStream fOut = new FileOutputStream(myFile);
                        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);


                        for (int i = 0; i < listAct.size(); i++) {
                            myOutWriter.append(listAct.get(i) + "\n");
                            Log.d(LOG_TAG, "Activity list size " + listAct.size());
                        }

                        myOutWriter.close();
                        fOut.close();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    // Can only read the media
                    Log.d(LOG_TAG, "SD card readable only");
                } else {
                    // Can't read or write
                    Log.d(LOG_TAG, "SD card not readable or writable");
                }

                //Display the updated list
                String aBuffer = "";
                for (int j = 0; j < listAct.size(); j++)
                {
                    aBuffer += listAct.get(j) + "\n";

                }
                textViewOut.setText(aBuffer);
                textViewOut.setVisibility(View.VISIBLE);

            }

        }
    }


}