package com.example.videocallingapp;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Firebase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.Arrays;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;


public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };





    RtcEngine rtcEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);

        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnCompleteListener(new OnCompleteListener<String>()
                {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    Log.i(TAG, token);

                    Toast.makeText(getApplicationContext(), "Token : " + token, Toast.LENGTH_SHORT).show();
                } else {
                    String error =task.getException().toString();
                    Toast.makeText(getApplicationContext(), "Token : " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });



       checkPermissions();


    }


    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        Toast.makeText(getApplicationContext(),"Permissions are Granted",Toast.LENGTH_SHORT).show();
        return true;
    }

    private void checkPermissions() {
        for (String permission : REQUESTED_PERMISSIONS) {
            if (!checkSelfPermission(permission, PERMISSION_REQ_ID))
            {
                return;
            }
            InitializeAgoraEngine();
            setupUIButtons();
        }
        // Initialize Agora only if permissions are granted

    }







    private void setupUIButtons() {
        Button startCallButton = findViewById(R.id.Start_Call_Button);
        startCallButton.setOnClickListener(v -> startVideoCall());

        Button endCallButton = findViewById(R.id.End_Call_Button);
        endCallButton.setOnClickListener(v -> endVideoCall());
    }

    private void startVideoCall() {
        rtcEngine.enableAudio();
        rtcEngine.setEnableSpeakerphone(true);
        rtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO,Constants.AUDIO_SCENARIO_CHATROOM_ENTERTAINMENT);
        rtcEngine.enableVideo();
        setupLocalVideo();
        rtcEngine.joinChannel(null, "test_channel", "Optional Data", 0);
        Toast.makeText(this, "Video call started", Toast.LENGTH_SHORT).show();
    }

    private void endVideoCall() {
        rtcEngine.leaveChannel();
        rtcEngine.stopPreview();

        // Clear the video view containers
        ((FrameLayout) findViewById(R.id.local_video_view_container)).removeAllViews();
        ((FrameLayout) findViewById(R.id.remote_video_view_container)).removeAllViews();

        Toast.makeText(this, "Video call ended", Toast.LENGTH_SHORT).show();
    }

    private void setupLocalVideo() {
        FrameLayout container = findViewById(R.id.local_video_view_container);
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);

        rtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        rtcEngine.startPreview();
    }


    // Agora event handler for handling user joins, leaves, etc.
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed)
        {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Join channel success", Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> setupRemoteVideo(uid));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> removeRemoteVideo());
        }

        @Override
        public void onError(int err)
        {
            super.onError(err);
            Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            Log.e("Agora ","Error"+err);
        }
    };

    private void setupRemoteVideo(int uid) {
        rtcEngine.enableAudio();
        rtcEngine.enableVideo();
        rtcEngine.setEnableSpeakerphone(true);
        FrameLayout container = findViewById(R.id.remote_video_view_container);
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        container.addView(surfaceView);

        rtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    private void removeRemoteVideo() {
        FrameLayout container = findViewById(R.id.remote_video_view_container);
        container.removeAllViews();
    }



    private void InitializeAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(getBaseContext(), "962a93c4d2bc4505a34ee2392144141a", mRtcEventHandler);
            Toast.makeText(this, "Agora Engine Initialized", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            Log.e("Initializtion Failed",e.getMessage().toString());
            throw new RuntimeException(e);
        }

    }

    //private void muteLocalAudio(boolean muted)
    //{
      //  rtcEngine.muteLocalAudioStream(muted);
   // }












}
