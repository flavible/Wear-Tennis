package com.example.george.tennisapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

public class MainActivityWatch extends Activity implements GoogleApiClient.ConnectionCallbacks,SensorEventListener {

    private static final String START_ACTIVITY = "/start_activity";
    private static final String WEAR_MESSAGE_PATH = "/message";

    private GoogleApiClient mApiClient;

    private ArrayAdapter<String> mAdapter;

    private ListView mListView;
    private EditText mEditText;
    private Button mSendButton;

    private float lastX, lastY, lastZ;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float deltaXMax = 0;
    private float deltaYMax = 0;
    private float deltaZMax = 0;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;

    private float vibrateThreshold = 0;
    public Double prevAccel = 0.0;
    public Boolean pause = true;
    public Boolean peak = false;
    public Boolean end = false;
    public Boolean flag = false;
    public Handler handler;
    public ArrayList accelArray = new ArrayList<Double>();
    public Vibrator v;

    private final Runnable processSensors = new Runnable() {
        @Override
        public void run() {
            // Do work with the sensor values.

            flag = true;
            // The Runnable is posted to run again here:
            handler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity_watch);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler = new Handler();
        handler.post(processSensors);
        init();
        initGoogleApiClient();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            vibrateThreshold = accelerometer.getMaximumRange() / 2;
        } else {
            // fai! we dont have an accelerometer!
        }
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause();
            }
        });
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .build();

        mApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mApiClient.disconnect();
    }

    private void init() {
        mListView = (ListView) findViewById( R.id.list_view );
        mEditText = (EditText) findViewById( R.id.input );
        mSendButton = (Button) findViewById( R.id.btn_send );

        mAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_list_item_1 );
        mListView.setAdapter(mAdapter);

    }

    private void sendMessage( final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }

                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        mEditText.setText( "" );
                    }
                });
            }
        }).start();
    }

    @Override
    public void onConnected(Bundle bundle) {
        sendMessage(START_ACTIVITY, "");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    //onResume() register the accelerometer for listening the events
    protected void onResume() {
        Log.e("resume", "resume");
        flag = false;
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        handler.post(processSensors);
        super.onResume();
        pause = true;


    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        Log.e("pause", "pause");
        sensorManager.unregisterListener(this);
        System.exit(0);
        pause = true;

        super.onPause();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
    Log.e("here",pause.toString());
    if(flag) {
        if (!pause && pause != null) {
        flag = false;
        // get the change of the x,y,z values of the accelerometer
        Double accel = Math.sqrt((event.values[0] * event.values[0]) + (event.values[1] * event.values[1]) + (event.values[2] * event.values[2]));

        // if the change is below 2, it is just plain noise
        if (accel - prevAccel > 0.5) {
            deltaX = event.values[0];
            deltaY = event.values[1];
            deltaZ = event.values[2];


                if (peak == true) {
                    if (accel < 3) {
                        end = true;

                    }
                }
                if (accel > 15 && !peak) {
                    peak = true;
                }
                if (!end) {

                    sendCurrentValues();

                } else {
                    pause();
                    sendFinish();
                    accelArray.clear();
                }
            }
        }

    }

    }




    // display the current x,y,z accelerometer values
    public void sendFinish() {
        String text = "finished";
        mAdapter.add(text);
        mAdapter.notifyDataSetChanged();

        sendMessage(WEAR_MESSAGE_PATH, text);
    }
    public void sendCurrentValues() {
        String text = "x:"+String.valueOf(deltaX)+","+"y:"+String.valueOf(deltaY)+","+"z:"+String.valueOf(deltaZ)+","+"time:"+String.valueOf(System.currentTimeMillis());
        mAdapter.add(text);
        mAdapter.notifyDataSetChanged();

        sendMessage(WEAR_MESSAGE_PATH, text);
    }

    public void pause(){
        if(pause == true){
            pause = false;
            peak = false;
            end = false;
            flag = false;
            mSendButton.setText("Swing Now!");
            mSendButton.setBackgroundColor(Color.parseColor("#55d510"));
        }else{
            pause = true;
            flag = false;
            mSendButton.setText("Start");
            mSendButton.setBackgroundColor(Color.parseColor("#3656f8"));
        }
    }
    // display the max x,y,z accelerometer values
}