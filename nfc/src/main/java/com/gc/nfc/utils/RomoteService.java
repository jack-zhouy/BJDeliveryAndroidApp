package com.gc.nfc.utils;

/**
 * Created by Administrator on 2018\7\24 0024.
 */


import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import app.project.IMyAidlInterface;

public class RomoteService extends Service {
    MyConn conn;
    MyBinder binder;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        conn = new MyConn();
        binder = new MyBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //this.bindService(new Intent(this, AmapLocationService.class), conn, Context.BIND_IMPORTANT);
        return START_STICKY;
    }

    class MyBinder extends IMyAidlInterface.Stub {
        @Override
        public String getServiceName() throws RemoteException {
            return RomoteService.class.getSimpleName();
        }
    }

    class MyConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {


            //开启本地服务
            //RomoteService.this.startService(new Intent(RomoteService.this, AmapLocationService.class));
            //绑定本地服务
            //RomoteService.this.bindService(new Intent(RomoteService.this, AmapLocationService.class), conn, Context.BIND_IMPORTANT);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //开启本地服务
       // RomoteService.this.startService(new Intent(RomoteService.this, AmapLocationService.class));
        //绑定本地服务
        //RomoteService.this.bindService(new Intent(RomoteService.this, AmapLocationService.class), conn, Context.BIND_IMPORTANT);

    }
}
