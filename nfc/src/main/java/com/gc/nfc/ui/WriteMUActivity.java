package com.gc.nfc.ui;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.Arrays;
import com.gc.nfc.R;
import com.gc.nfc.base.BaseNfcActivity;

import java.nio.charset.Charset;

/**
 * Created by gc on 2016/12/8.
 */
public class WriteMUActivity extends BaseNfcActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_mu);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String[] techList = tag.getTechList();
        boolean haveMifareUltralight = false;
        for (String tech : techList) {
            if (tech.indexOf("MifareUltralight") >= 0) {
                haveMifareUltralight = true;
                break;
            }
        }
        if (!haveMifareUltralight) {
            Toast.makeText(this, "不支持MifareUltralight数据格式", Toast.LENGTH_SHORT).show();
            return;
        }
        writeTag(tag);
    }

    public void writeTag(Tag tag) {
        EditText textWrite=(EditText)findViewById(R.id.textWrite);
        MifareUltralight ultralight = MifareUltralight.get(tag);
        try {
            ultralight.connect();
            byte[] writeBytes= textWrite.getText().toString().getBytes();
            //写入八个汉字，从第五页开始写，中文需要转换成GB2312格式
            //Toast.makeText(this, textWrite.getText().toString(), Toast.LENGTH_SHORT).show();
            byte[][] bytes = split_bytes(writeBytes, 4);
            for (int i=0;i<bytes.length;i++){
                ultralight.writePage(4+i, bytes[i]);
            }
            Toast.makeText(this, "写入成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "写入失败", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                ultralight.close();
            } catch (Exception e) {
            }
        }
    }


        public byte[][] split_bytes(byte[] bytes, int copies) {

        double split_length = Double.parseDouble(copies + "");

        int array_length = (int) Math.ceil(bytes.length / split_length);
        byte[][] result = new byte[array_length][];

        int from, to;

        for (int i = 0; i < array_length; i++) {

            from = (int) (i * split_length);
            to = (int) (from + split_length);

            if (to > bytes.length)
                to = bytes.length;

            result[i] = Arrays.copyOfRange(bytes, from, to);
        }

        return result;
    }
}
