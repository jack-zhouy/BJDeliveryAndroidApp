package com.gc.nfc.service;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.RequiresApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.app.Service;

import android.os.Handler;
import android.os.Message;
import android.widget.Toast;
import android.os.Build;
import android.app.ActivityManager;
import android.content.Context;

import com.gc.nfc.ui.AutoLoginActivity;
import com.gc.nfc.ui.BottleRecycleActivity;
import com.gc.nfc.ui.MainlyActivity;
import com.gc.nfc.ui.MybottlesActivity;
import com.gc.nfc.utils.AmapLocationService;

/**JobService，支持5.0以上forcestop依然有效
 *
 * Created by jianddongguo on 2017/7/10.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)//API需要在21及以上
public class MyJobService extends JobService {

    private Handler mJobHandler = new Handler( new Handler.Callback() {

        @Override
        public boolean handleMessage( Message msg ) {
//            Toast.makeText( getApplicationContext(),
//                    "JobService task running", Toast.LENGTH_SHORT )
//                    .show();
//            jobFinished( (JobParameters) msg.obj, false );
            return true;
        }

    } );

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                String servicename = params.getExtras().getString("servicename");
                String userId = params.getExtras().getString("userId");

                Class service = getClassLoader().loadClass(servicename);
                if (service != null) {
                    //判断保活的service是否被杀死
                    if (!isMyServiceRunning(service)) {

                        Intent intentAmap = new Intent(getApplicationContext(), service);
                        Bundle bundleAmap = new Bundle();
                        bundleAmap.putString("userId", userId);
                        intentAmap.putExtras(bundleAmap);
                        //重启service
                        startService(new Intent(intentAmap));
                    }else{
//                        Toast.makeText(this, "alive",
//                                Toast.LENGTH_LONG).show();
                    }
                }
                jobFinished(params, false);
                startJobScheduler(userId);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    @Override
    public boolean onStopJob(JobParameters params) {
        //mJobHandler.removeMessages( 1 );
        return false;
    }

    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @RequiresApi(21)
    public void startJobScheduler(String userId) {
        JobScheduler JobScheduler = (JobScheduler) getSystemService( Context.JOB_SCHEDULER_SERVICE );

        int id = 55;

        JobScheduler.cancel(id);
        JobInfo.Builder builder = new JobInfo.Builder(id, new ComponentName(this, MyJobService.class));
        builder.setMinimumLatency(5000); //执行的最小延迟时间
        //builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS); //执行的最小延迟时间
        builder.setOverrideDeadline(6000);  //执行的最长延时时间
        //builder.setOverrideDeadline(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);  //执行的最长延时时间
        builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR);//线性重试方案
        builder.setPersisted(true);  // 设置设备重启时，执行该任务
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setRequiresCharging(true); // 当插入充电器，执行该任务
        PersistableBundle persiBundle = new PersistableBundle();
        persiBundle.putString("servicename", AmapLocationService.class.getName());
        persiBundle.putString("userId", userId);
        builder.setExtras(persiBundle);
        JobInfo info = builder.build();

        JobScheduler.schedule(info); //开始定时执行该系统任务
    }


}