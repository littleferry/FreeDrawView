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

    private Button mBtnMuiltActive1, mBtnMuiltActive2, mBtnMuiltActive3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chooser);

        mBtnMuiltActive1 = (Button) findViewById(R.id.btn_muilt_active1);
        mBtnMuiltActive2 = (Button) findViewById(R.id.btn_muilt_active2);
        mBtnMuiltActive3 = (Button) findViewById(R.id.btn_muilt_active3);

        mBtnMuiltActive1.setOnClickListener(this);
        mBtnMuiltActive2.setOnClickListener(this);
        mBtnMuiltActive3.setOnClickListener(this);

        WhiteBoardManager wbm = WhiteBoardManager.getInst();
        wbm.initData(this.getApplicationContext());

        mBtnMuiltActive1.setText(wbm.getTitle(1));
        mBtnMuiltActive2.setText(wbm.getTitle(2));
        mBtnMuiltActive3.setText(wbm.getTitle(3));

        mBtnMuiltActive2.setVisibility(View.GONE);
        mBtnMuiltActive3.setVisibility(View.GONE);

        setTitle("当前用户： " + wbm.getDeviceId());
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Intent intent = new Intent(this, ActivityDraw.class);
        if (id == mBtnMuiltActive1.getId()) {
            intent.putExtra(ActivityDraw.UserID, 1);
        } else if (id == mBtnMuiltActive2.getId()) {
            intent.putExtra(ActivityDraw.UserID, 2);
        } else if (id == mBtnMuiltActive3.getId()) {
            intent.putExtra(ActivityDraw.UserID, 3);
        }
        startActivity(intent);
    }
}
