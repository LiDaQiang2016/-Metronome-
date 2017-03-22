package com.cainwong.metronome.core;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

/**
 * Created by cwong on 10/14/15.
 */
@Singleton
public class Metronome {

    public static final int DEFAULT_DELAY = 500;
//    public static final int DEFAULT_BEATS = 4;
    private int mBpm =100;
    private int mX=4;
    private int mY=4;
    private int delay;
    private int numBeats = mX;
    private int beat = 0;
    private int firstDelay=0;

    private BehaviorSubject<Integer> delayObservable = BehaviorSubject.create();
    private BehaviorSubject<Integer> beatObservable = BehaviorSubject.create();
    private BehaviorSubject<Boolean> playStateObservable = BehaviorSubject.create();
    private PublishSubject<Object> stopTrigger= PublishSubject.create();

    @Inject
    @Named("newThread")
    Scheduler scheduler;

    @Inject
    public Metronome() {
//        setDelay(DEFAULT_DELAY);
        setConfig(mBpm,mX,mY);
    }

    private void setDelay(int delay) {
        this.delay = delay;
        delayObservable.onNext(delay);
        restartIfPlaying();
    }



    private boolean delayIsChange=false;
    public void setConfig(int pdm,int x,int y){
        if(mBpm==0||x==0||y==0){
         return;
        }
        mBpm =pdm;
        mX=x;
        mY=y;
//        int  newDelay=(int)(((1000*60.0)/ mBpm)*(1.0*mX/mY));
//        if(newDelay==delay){
//            return;
//        }
        delay=(int)(((1000*60.0)/ mBpm)*(1.0*mX/mY));
        delayIsChange=true;
//        setDelay(delay);
    }


    public void setConfig(int x,int y){
        setConfig(mBpm,x,y);
    }

    public void setConfig(int pdm){
        setConfig(pdm,mX,mY);
    }


    public void setNumBeats(int numBeats) {
        this.numBeats = numBeats;
        restartIfPlaying();
    }

    public Observable<Integer> getDelayObservable() {
        return delayObservable;
    }

    public Observable<Integer> getBeatObservable(){
        return beatObservable;
    }

    public Observable<Boolean> getPlayStateObservable() {
        return playStateObservable;
    }


    public void togglePlay(){
        if(isPlaying()){
            stop();
        } else {
            play();
        }
    }

    private boolean isPlaying(){
        return Boolean.TRUE.equals(playStateObservable.getValue());
    }

//    Handler handler = new Handler();
//
//
//    private void  postDelayed(int time){
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                beat++;
//                beatObservable.onNext(beat);
//                if(beat==numBeats){
//                    beat = 0;
//                }
//                postDelayed(delay);
//            }
//        },time);
//
//    }
    private void play(){
        playStateObservable.onNext(true);
        Observable.interval(delay, TimeUnit.MILLISECONDS, scheduler)
                .takeUntil(stopTrigger)
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long o) {
//                        Log.e("LDQ","111111111111     beat="+beat);
//                        courentTime= SystemClock.currentThreadTimeMillis();
                        beat++;
                        beatObservable.onNext(beat);
                        if(beat==numBeats){
                            beat = 0;
                        }
                        if(delayIsChange){
                            delayIsChange=false;
                            setDelay(delay);
                        }

                    }
                });



    }
//    Timer timer = new Timer();
//    TimerTask task = new TimerTask() {
//
//        @Override
//        public void run() {
//            // 需要做的事:发送消息
//            beat++;
//            beatObservable.onNext(beat);
//            if(beat==numBeats){
//                beat = 0;
//            }
//        }
//    };
    private void stop(){
        stopTrigger.onNext(null);
        beat=0;
        playStateObservable.onNext(false);
    }

    public void stopPlay(){
        if(isPlaying()){
            stop();
        }
    }

    private void restartIfPlaying(){
        if(isPlaying()) {
            stopTrigger.onNext(null);
            play();
        }
    }

}
