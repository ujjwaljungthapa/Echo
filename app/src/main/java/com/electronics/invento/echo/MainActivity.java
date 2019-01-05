package com.electronics.invento.echo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    //Requesting permission to RECORD_AUDIO;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private ImageView imageView_start_echo;
    private TextView textView_record_status;

    private boolean mStartEcho = true;
    private boolean mRunProcess = true;

    private Thread m_thread;

    private AudioRecord m_record = null;
    private AudioTrack m_track = null;

    int SAMPLE_RATE = 44100;        //clearer sound
    //int BUF_SIZE = 256;             //better than 1024
    int BUF_SIZE = 512;             //better than 1024
    byte[] buffer = new byte[BUF_SIZE];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        initialize();

        imageView_start_echo.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) {
            finish();
        }
    }

    private void initialize() {
        imageView_start_echo = findViewById(R.id.imageView_main_start);
        textView_record_status = findViewById(R.id.textView_main_status);
        imageView_start_echo.setBackgroundColor(getResources().getColor(R.color.colorRecordGreen));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageView_main_start:
                if (mStartEcho) {
                    mRunProcess = true;
                    imageView_start_echo.setBackgroundColor(getResources().getColor(R.color.colorRecordRed));
                    textView_record_status.setText("Echo Started");

                    do_loopback();
                } else {
                    mRunProcess = false;
                    imageView_start_echo.setBackgroundColor(getResources().getColor(R.color.colorRecordGreen));
                    textView_record_status.setText("Echo Stopped");

                    m_thread.interrupt();
                    if (m_record != null) {
                        m_record.release();
                        m_record = null;
                    }
                    if (m_track != null) {
                        m_track.flush();
                        m_track.release();
                        m_track = null;
                    }
                }
                mStartEcho = !mStartEcho;
                break;
        }
    }

    private void do_loopback() {
        m_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                loopback();
            }
        });

        m_thread.start();
    }

    private void loopback() {
        try {
            //Using AudioRecord & AudioTrack
            int buffersize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            buffersize = Math.max(buffersize, AudioTrack.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT));

            buffersize = Math.max(buffersize, BUF_SIZE);

            if (buffersize <= BUF_SIZE) {
                buffersize = BUF_SIZE;
            }
            Log.d(TAG, "loopback: Initializing audio record and audio playing objects");
           /* m_record = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffersize);*/
            m_record = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,   //better
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffersize);

            m_track = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffersize,
                    AudioTrack.MODE_STREAM);
            //increase volumme of track
            m_track.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
            //set track rate
            m_track.setPlaybackRate(SAMPLE_RATE);
        } catch (Throwable throwable) {
            Log.d(TAG, "loopback: failed Initializing audio record and audio playing objects");
        }

        m_record.startRecording();
        Log.d(TAG, "loopback: Audio Recording started");
        m_track.play();

        Log.d(TAG, "loopback: Audio playing started");

        while (mRunProcess) {
            m_record.read(buffer, 0, BUF_SIZE);
            m_track.write(buffer, 0, buffer.length);
        }

        Log.d(TAG, "loopback: loopback exit");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mRunProcess = false;
        if (m_record != null) {
            m_record.release();
            m_record = null;
        }
        if (m_track != null) {
            m_track.flush();
            m_track.release();
            m_track = null;
        }
    }
}
