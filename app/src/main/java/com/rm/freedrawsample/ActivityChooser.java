package com.rm.freedrawsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.hengqian.whiteboard.msg.WhiteBoardManager;

/**
 * Created by Riccardo on 01/12/16.
 */

public class ActivityChooser extends AppCompatActivity implements View.OnClickListener {

    private Button mBtnMutilActive1, mBtnMutilActive2, mBtnMutilActive3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chooser);

        mBtnMutilActive1 = (Button) findViewById(R.id.btn_multi_active1);
        mBtnMutilActive2 = (Button) findViewById(R.id.btn_multi_active2);
        mBtnMutilActive3 = (Button) findViewById(R.id.btn_multi_active3);

        mBtnMutilActive1.setOnClickListener(this);
        mBtnMutilActive2.setOnClickListener(this);
        mBtnMutilActive3.setOnClickListener(this);

        WhiteBoardManager wbm = WhiteBoardManager.getInst();
        wbm.initData(this.getApplicationContext());

        mBtnMutilActive1.setText(wbm.getTitle(1));
        mBtnMutilActive2.setText(wbm.getTitle(2));
        mBtnMutilActive3.setText(wbm.getTitle(3));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Intent intent = new Intent(this, ActivityDraw.class);
        if (id == mBtnMutilActive1.getId()) {
            intent.putExtra("user", 1);
        } else if (id == mBtnMutilActive2.getId()) {
            intent.putExtra("user", 2);
        } else if (id == mBtnMutilActive3.getId()) {
            intent.putExtra("user", 3);
        }
        startActivity(intent);
    }
}
