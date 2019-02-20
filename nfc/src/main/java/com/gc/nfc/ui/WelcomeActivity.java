package com.gc.nfc.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.gc.nfc.R;
import android.view.WindowManager;
import android.view.Window;
import android.os.Handler;
import android.os.Message;

/**
 * Created by gc on 2016/12/8.
 */
public class WelcomeActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (!isTaskRoot()) {
            finish();
            return;
        }
        super.onCreate(savedInstanceState);
        //隐藏标题栏以及状态栏
        /**全屏设置，隐藏窗口所有装饰**/
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 取消标题栏
        setContentView(R.layout.welcome_page);
        handler.sendEmptyMessageDelayed(0,2000);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {



            getHome();
            super.handleMessage(msg);
        }
    };

    public void getHome(){
        Intent intent = new Intent(WelcomeActivity.this, AutoLoginActivity.class);
        startActivity(intent);
        finish();
    }
}
