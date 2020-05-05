package com.bytedance.videoplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author 12937
 * VideoPlayer-基础版
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private VideoView videoView;
    private SeekBar seekBar;
    private Button buttonPlay;
    private Button buttonPause;
    private Button buttonOpen;
    private TextView textViewTime;
    private TextView textViewCurrentPosition;

    private String videoPath = "";
    private int playProgress = 0;
    private boolean playFlag = false;

    // 新开线程刷新进度条
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (videoView.isPlaying()) {
                int current = videoView.getCurrentPosition();
                seekBar.setProgress(current);
            }
            handler.postDelayed(runnable, 500);
        }
    };

    private String[] mPermissionsArrays = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    private final static int REQUEST_PERMISSION = 123;

    private boolean checkPermissionAllGranted(String[] permissions) {
        // 6.0以下不需要
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            Toast.makeText(this, "授权成功！", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            videoPath = savedInstanceState.getString("videoPath");
            playProgress = savedInstanceState.getInt("playProgress");
            playFlag = savedInstanceState.getBoolean("playFlag");
        }

        if (!checkPermissionAllGranted(mPermissionsArrays)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(mPermissionsArrays, REQUEST_PERMISSION);
            }
        }

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

            // 去掉标题栏
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
            // 全屏、隐藏状态栏
            if (Build.VERSION.SDK_INT < 19) {
                View v = this.getWindow().getDecorView();
                v.setSystemUiVisibility(View.GONE);
            } else {
                View decorView = getWindow().getDecorView();
                int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);
            }
        }

        setContentView(R.layout.activity_main);
        videoView = findViewById(R.id.videoView);
        textViewTime = findViewById(R.id.textViewTime);
        buttonOpen = findViewById(R.id.buttonOpen);
        seekBar = findViewById(R.id.seekBar);
        buttonPlay = findViewById(R.id.buttonPlay);
        buttonPause = findViewById(R.id.buttonPause);
        textViewCurrentPosition = findViewById(R.id.textViewCurrentPosition);

        buttonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
            }
        });

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

            // 修改背景为黑色
            LinearLayout mainLayout = findViewById(R.id.mainLayout);
            mainLayout.setBackgroundColor(Color.parseColor("#000000"));
            // 添加控制进度条
            MediaController mctrl = new MediaController(this);
            videoView.setMediaController(mctrl);
            // 扩展至全屏且居中
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            videoView.setLayoutParams(layoutParams);
            if (!"".equals(videoPath)) {
                videoView.setVideoPath(videoPath);
            }
        } else if (MainActivity.this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            RelativeLayout relativeLayout = findViewById(R.id.videoPlay);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );

            int m = ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()));
            layoutParams.setMargins(m, m, m, 0);
            layoutParams.setMarginStart(m);
            layoutParams.setMarginEnd(m);
            relativeLayout.setLayoutParams(layoutParams);

            if (!"".equals(videoPath)) {
                videoView.setVideoPath(videoPath);
            }
        }

        videoView.requestFocus();

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                textViewTime.setText(time(videoView.getDuration()));
                seekBar.setEnabled(true);
                seekBar.setMax(videoView.getDuration());
                videoView.seekTo(playProgress);
                if (MainActivity.this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    seekBar.setProgress(playProgress);
                    textViewCurrentPosition.setText(time(playProgress));
                    mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                        @Override
                        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                            int videoWidth = mp.getVideoWidth();
                            int videoHeight = mp.getVideoHeight();

                            int viewWidth = videoView.getWidth();
                            int viewHeight = videoView.getHeight();

                            float max = Math.max((float) videoWidth / (float) viewWidth, (float) videoHeight / (float) viewHeight);

                            videoWidth = (int) Math.ceil((float) videoWidth / max);
                            videoHeight = (int) Math.ceil((float) videoHeight / max);

                            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(videoWidth, videoHeight);
                            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                            videoView.setLayoutParams(layoutParams);
                        }
                    });
                }
                if (playFlag) {
                    play();
                    buttonPlay.setEnabled(false);
                    buttonPause.setEnabled(true);
                } else {
                    buttonPlay.setEnabled(true);
                    buttonPause.setEnabled(false);
                }
            }
        });

        // 播放出错
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(MainActivity.this, "播放出错", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        seekBar.setEnabled(false);
        // 为进度条添加进度更改事件
        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        buttonPlay.setEnabled(false);
        buttonPause.setEnabled(false);
        buttonPlay.setOnClickListener(this);
        buttonPause.setOnClickListener(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                videoPath = uri.getPath();
                videoView.setVideoPath(uri.getPath());
                playProgress = 0;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonPlay:
                play();
                buttonPlay.setEnabled(false);
                buttonPause.setEnabled(true);
                Toast.makeText(MainActivity.this, "开始播放", Toast.LENGTH_SHORT).show();
                break;
            case R.id.buttonPause:
                if (videoView.isPlaying()) {
                    videoView.pause();
                    buttonPlay.setEnabled(true);
                    buttonPause.setEnabled(false);
                    Toast.makeText(MainActivity.this, "暂停播放", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            if (videoView.isPlaying()) {
                videoView.seekTo(progress);
            }
            handler.postDelayed(runnable, 0);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            handler.removeCallbacks(runnable);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            textViewCurrentPosition.setText(time(progress));
            if (!videoView.isPlaying()) {
                videoView.seekTo(progress);
            }
        }
    };

    protected void play() {
        handler.postDelayed(runnable, 0);
        videoView.start();
    }

    protected String time(long millionSeconds) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millionSeconds - TimeZone.getDefault().getRawOffset());
        return simpleDateFormat.format(c.getTime());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("videoPath", videoPath);
        savedInstanceState.putInt("playProgress", playProgress);
        savedInstanceState.putBoolean("playFlag", playFlag);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        playProgress = videoView.getCurrentPosition();
        playFlag = videoView.isPlaying();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }
}
