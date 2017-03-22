package com.cainwong.metronome.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.cainwong.metronome.App;
import com.cainwong.metronome.R;
import com.cainwong.metronome.RotateControlView;
import com.cainwong.metronome.core.Metronome;
import com.cainwong.metronome.services.AudioService;
import com.jakewharton.rxbinding.view.RxView;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    public static final long MIN_DELAY = 215;
    public static final long MAX_DELAY = 3000;
    public static final int ROUNDTO_VALUE = 10;

    @Inject
    Metronome metronome;

    @Inject
    @Named("mainThread")
    Scheduler mainThreadScheduler;

    @Inject
    @Named("immediate")
    Scheduler intervalScheduler;

    @Inject
    @Named("newThread")
    Scheduler newThreadScheduler;

    Subscription delaySubscription;
    Subscription playStateSubscription;
    @Bind(R.id.rotate)
    RotateControlView rotate;
    @Bind(R.id.beat_left_ib)
    ImageButton beatLeftIb;
    @Bind(R.id.beat_tempo_tv)
    TextView beatTempoTv;
    @Bind(R.id.beat_right_ib)
    ImageButton beatRightIb;
    @Bind(R.id.beat_less_ib)
    ImageButton beatLessIb;
    @Bind(R.id.beat_plus_ib)
    ImageButton beatPlusIb;
    @Bind(R.id.beat_start_ib)
    ImageButton beatStartIb;
    @Bind(R.id.beat_switch)
    TextView beatSwitch;
    @Bind(R.id.beat_bpm_et)
    EditText beatBpmEt;
    private int mBpm = 120;
    private int mX = 4;
    private int mY = 4;
    private int mMaxValue = 400;       //最大值
    private int mMinValue = 60;       //最小值
    private BeatModel[] mBeatArray = new BeatModel[]{new BeatModel(2, 2), new BeatModel(3, 4), new BeatModel(4, 4), new BeatModel(4, 8), new BeatModel(6, 8)};
    private int curentBeatDex = 2;

    //Activity最外层的Layout视图
    private View activityRootView;
    //屏幕高度
    private int screenHeight = 0;
    //软件盘弹起后所占高度阀值
    private int keyHeight = 0;

    @Override
    protected void onResume() {
        super.onResume();

//        //添加layout大小发生改变监听器
//        activityRootView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
//            @Override
//            public void onLayoutChange(View v, int left, int top, int right,
//                                       int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
//
//                //old是改变前的左上右下坐标点值，没有old的是改变后的左上右下坐标点值
//
////      System.out.println(oldLeft + " " + oldTop +" " + oldRight + " " + oldBottom);
////      System.out.println(left + " " + top +" " + right + " " + bottom);
//
//
//                //现在认为只要控件将Activity向上推的高度超过了1/3屏幕高，就认为软键盘弹起
//                if (oldBottom != 0 && bottom != 0 && (oldBottom - bottom > keyHeight)) {
//
//                    Toast.makeText(MainActivity.this, "监听到软键盘弹起...", Toast.LENGTH_SHORT).show();
//                    metronome.stopPlay();
//                    Log.e("LDQ","111111 监听到软键盘弹起");
//
//                } else if (oldBottom != 0 && bottom != 0 && (bottom - oldBottom > keyHeight)) {
//
//
//                    String dpmStr=beatBpmEt.getText().toString();
//                    if(dpmStr==null||dpmStr.equals("")){
//                        beatBpmEt.setText(mMinValue+"");
//                        metronome.setConfig(mMinValue);
//                    }else {
//                        int dpm= Integer.parseInt(dpmStr);
//                        if(dpm>mMaxValue){
//                            dpm=mMaxValue;
//                        }else if(dpm<mMinValue){
//                            dpm=mMinValue;
//                        }
//                        beatBpmEt.setText(dpm+"");
//                        metronome.setConfig(dpm);
//                    }
//                    Toast.makeText(MainActivity.this, "监听到软件盘关闭...", Toast.LENGTH_SHORT).show();
//                    Log.e("LDQ","22222222 监听到软件盘关闭");
//                }
//            }
//        });
    }

    private Handler mHandler=new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.component(this).inject(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        rotate.setMaxAndMinValue(mMinValue, mMaxValue, mBpm);
        beatTempoTv.setText(mBeatArray[curentBeatDex].getName());
        beatBpmEt.setText(mBpm + "");
        activityRootView = findViewById(R.id.root_layout);
        //获取屏幕高度
        screenHeight = this.getWindowManager().getDefaultDisplay().getHeight();
        //阀值设置为屏幕高度的1/3
        keyHeight = screenHeight/3;

        //添加layout大小发生改变监听器
        activityRootView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, final int left, final int top, final int right,
                                       final  int bottom,final  int oldLeft,final  int oldTop, final int oldRight,final  int oldBottom) {

                //old是改变前的左上右下坐标点值，没有old的是改变后的左上右下坐标点值

//      System.out.println(oldLeft + " " + oldTop +" " + oldRight + " " + oldBottom);
//      System.out.println(left + " " + top +" " + right + " " + bottom);


                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        //现在认为只要控件将Activity向上推的高度超过了1/3屏幕高，就认为软键盘弹起
                        if (oldBottom != 0 && bottom != 0 && (oldBottom - bottom > keyHeight)) {

//                            Toast.makeText(MainActivity.this, "监听到软键盘弹起...", Toast.LENGTH_SHORT).show();
                            metronome.stopPlay();
//                            Log.e("LDQ","111111 监听到软键盘弹起");
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    beatBpmEt.setCursorVisible(true);
                                }
                            });

                        } else if (oldBottom != 0 && bottom != 0 && (bottom - oldBottom > keyHeight)) {
                            String dpmStr=beatBpmEt.getText().toString();
                            if(dpmStr==null||dpmStr.equals("")){
                                mBpm=mMinValue;
//                                beatBpmEt.setText(mMinValue+"");
//                                metronome.setConfig(mMinValue);
                            }else {
                                mBpm= Integer.parseInt(dpmStr);
                                if(mBpm>mMaxValue){
                                    mBpm=mMaxValue;
                                }else if(mBpm<mMinValue){
                                    mBpm=mMinValue;
                                }


                            }

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    beatBpmEt.setText(mBpm+"");

                                    beatBpmEt.setCursorVisible(false);
                                }
                            });

                            metronome.setConfig(mBpm);
                            rotate.setValue(mBpm);
//                            Toast.makeText(MainActivity.this, "监听到软件盘关闭...", Toast.LENGTH_SHORT).show();
//                            Log.e("LDQ","22222222 监听到软件盘关闭");
                        }
                    }
                }).start();

                    }
                });

    }

    @Override
    protected void onStart() {
        super.onStart();

//        setRegion(beatBpmEt);

//        beatBpmEt.addTextChangedListener(new TextWatcher() {
//            private int oldValue=0;
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//                if(editable.toString()==null||editable.toString().equals("")){
//                    return;
//                }
//                int dpm= Integer.parseInt(editable.toString());
//                Log.e("asd","dpm="+dpm+"      editable.toString()="+editable.toString());
//
//
//
//                if(dpm>mMaxValue){
//                    beatBpmEt.setText(mMaxValue+"");
//                    Toast.makeText(MainActivity.this,"最大值为"+mMaxValue,Toast.LENGTH_SHORT).show();
//                    return;
//                }else
////                if(dpm<0){
////                    beatBpmEt.setText(mMinValue+"");
////                    Toast.makeText(MainActivity.this,"最小值为"+mMinValue,Toast.LENGTH_SHORT).show();
////                    return;
////                }
//
//                if(dpm<mMinValue){
//                    return;
//                }
//                metronome.setConfig(dpm);
//            }
//        });


                RxView.clicks(beatLeftIb)
//                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        curentBeatDex--;
                        if (curentBeatDex < 0) {
                            curentBeatDex = (mBeatArray.length - 1);
                        }
                        metronome.setConfig(mBeatArray[curentBeatDex].getmX(), mBeatArray[curentBeatDex].getmY());
                        beatTempoTv.setText(mBeatArray[curentBeatDex].getName());
                    }
                });


        RxView.clicks(beatRightIb)
//                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        curentBeatDex++;
                        if (curentBeatDex > (mBeatArray.length - 1)) {
                            curentBeatDex = 0;
                        }
                        metronome.setConfig(mBeatArray[curentBeatDex].getmX(), mBeatArray[curentBeatDex].getmY());
                        beatTempoTv.setText(mBeatArray[curentBeatDex].getName());
                    }
                });


        RxView.clicks(beatLessIb)
//                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mBpm--;
                        if (mBpm < mMinValue) {
                            mBpm = mMinValue;
                        }
                        metronome.setConfig(mBpm);
                        rotate.setValue(mBpm);
                        beatBpmEt.setText(mBpm + "");
                    }
                });


        RxView.clicks(beatPlusIb)
//                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        mBpm++;
                        if (mBpm > mMaxValue) {
                            mBpm = mMaxValue;
                        }
                        metronome.setConfig(mBpm);
                        rotate.setValue(mBpm);
                        beatBpmEt.setText(mBpm + "");
                    }
                });


        rotate.setOnTempChangeListener(new RotateControlView.OnTempChangeListener() {
            @Override
            public void change(final int temp) {
                metronome.setConfig(temp);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBpm = temp;
                        beatBpmEt.setText(mBpm + "");
                    }
                });

            }
        });

        // Handle click events

        RxView.clicks(beatStartIb)
                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        metronome.togglePlay();
                    }
                });


        // Handle metronome events
//        delaySubscription = metronome.getDelayObservable()
//                .observeOn(mainThreadScheduler)
//                .map(new MapMillisToBpm())
//                .map(new MapRoundFloatToInt())
//                .map(new MapRoundToValue(ROUNDTO_VALUE))
//                .map(new Func1<Integer, String>() {
//                    @Override
//                    public String call(Integer tempo) {
//                        return getApplicationContext().getString(R.string.tempo_disp, tempo);
//                    }
//                })
//                .subscribe(new Action1<String>() {
//                    @Override
//                    public void call(String s) {
//                        tempoView.setText(s);
//                    }
//                });


        // play state display
        playStateSubscription = metronome.getPlayStateObservable()
                .observeOn(mainThreadScheduler)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        beatStartIb.setImageResource(aBoolean ? R.mipmap.pause_icon
                                : R.mipmap.play_icon);
                        if (aBoolean) {
                            startService(new Intent(getApplicationContext(), AudioService.class));
                        } else {
                            stopService(new Intent(getApplicationContext(), AudioService.class));
                        }
                    }
                });

    }

    @Override
    protected void onStop() {
        if (delaySubscription != null && !delaySubscription.isUnsubscribed()) {
            delaySubscription.unsubscribe();
        }
        if (playStateSubscription != null && !playStateSubscription.isUnsubscribed()) {
            playStateSubscription.unsubscribe();
        }
        super.onStop();
    }



//    private int MIN_MARK = mMinValue;
//    private int MAX_MARK = mMaxValue;
//    //private void setRegion(EditText et)
//    private void setRegion( final EditText et)
//    {
//        et.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                if (start > 1)
//                {
//                    if (MIN_MARK != -1 && MAX_MARK != -1)
//                    {
//                        int num = Integer.parseInt(s.toString());
//                        if (num > MAX_MARK)
//                        {
//                            s = String.valueOf(MAX_MARK);
//                            et.setText(s);
//                        }
//                        else if(num < MIN_MARK)
//                            s = String.valueOf(MIN_MARK);
//                        return;
//                    }
//                }
//            }
//
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count,
//                                          int after) {
//            }
//
//            @Override
//            public void afterTextChanged(Editable s)
//            {
//                if (s != null && !s.equals(""))
//                {
//                    if (MIN_MARK != -1 && MAX_MARK != -1)
//                    {
//                        int markVal = 0;
//                        try
//                        {
//                            markVal = Integer.parseInt(s.toString());
//                        }
//                        catch (NumberFormatException e)
//                        {
//                            markVal = 0;
//                        }
//                        if (markVal > MAX_MARK)
//                        {
//                            Toast.makeText(getBaseContext(), "分数不能超过100", Toast.LENGTH_SHORT).show();
//                            et.setText(String.valueOf(MAX_MARK));
//                        }
//                        return;
//                    }
//                }
//            }
//        });
//    }
}







class BeatModel {
    BeatModel(int x, int y) {
        mX = x;
        mY = y;
    }


    int mX;
    int mY;

    public int getmX() {
        return mX;
    }

    public void setmX(int mX) {
        this.mX = mX;
    }

    public int getmY() {
        return mY;
    }

    public void setmY(int mY) {
        this.mY = mY;
    }

    public String getName() {
        return mX + "/" + mY;
    }

}