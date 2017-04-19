package com.rm.freedrawsample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hengqian.whiteboard.msg.WhiteBoardManager;
import com.hengqian.whiteboard.msg.Whiteboardmsg;
import com.rm.freedrawview.FreeDrawView;
import com.rm.freedrawview.PathDrawnListener;
import com.rm.freedrawview.PathRedoUndoCountChangeListener;

import java.util.ArrayList;

public class ActivityDraw extends AppCompatActivity
        implements View.OnClickListener, SeekBar.OnSeekBarChangeListener,
        PathRedoUndoCountChangeListener, FreeDrawView.DrawCreatorListener, PathDrawnListener {

    private static final int THICKNESS_STEP = 1;
    private static final int THICKNESS_MAX = 30;
    private static final int THICKNESS_MIN = 1;

    private static final int ALPHA_STEP = 1;
    private static final int ALPHA_MAX = 255;
    private static final int ALPHA_MIN = 0;

    private FreeDrawView mFreeDrawView;
    private View mSideView;
    private Button mBtnRandomColor, mBtnUndo, mBtnClearAll, mBtnSend;
    private SeekBar mThicknessBar, mAlphaBar;
    private TextView mTxtUndoCount;

    private ImageView mImgScreen;
    private Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        mImgScreen = (ImageView) findViewById(R.id.img_screen);

        mTxtUndoCount = (TextView) findViewById(R.id.txt_undo_count);

        mFreeDrawView = (FreeDrawView) findViewById(R.id.free_draw_view);
        mFreeDrawView.setOnPathDrawnListener(this);
        mFreeDrawView.setPathRedoUndoCountChangeListener(this);

        View view = findViewById(R.id.side_view);
        view.setBackgroundColor(Color.DKGRAY);
        mBtnRandomColor = (Button) findViewById(R.id.btn_color);
        mSideView = mBtnRandomColor;
        mBtnUndo = (Button) findViewById(R.id.btn_undo);
        mBtnClearAll = (Button) findViewById(R.id.btn_clear_all);
        mBtnSend = (Button) findViewById(R.id.btn_clear_all);
        mThicknessBar = (SeekBar) findViewById(R.id.slider_thickness);
        mAlphaBar = (SeekBar) findViewById(R.id.slider_alpha);

        mBtnRandomColor.setOnClickListener(this);
        mBtnUndo.setOnClickListener(this);
        mBtnClearAll.setOnClickListener(this);
        mBtnSend.setOnClickListener(this);

        mAlphaBar.setMax((ALPHA_MAX - ALPHA_MIN) / ALPHA_STEP);
        mAlphaBar.setProgress(mFreeDrawView.getPaintAlpha());
        mAlphaBar.setOnSeekBarChangeListener(this);

        mThicknessBar.setMax((THICKNESS_MAX - THICKNESS_MIN) / THICKNESS_STEP);
        mThicknessBar.setOnSeekBarChangeListener(this);
        mThicknessBar.setProgress((int) mFreeDrawView.getPaintWidth());

        Intent intent = getIntent();
        int user = intent.getIntExtra("user", 1);
        WhiteBoardManager.getInst().setUser(user);
        WhiteBoardManager.getInst().setHandler(handler);
        WhiteBoardManager.getInst().start();

        setTitle(WhiteBoardManager.getInst().getUserId());

        changeColor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WhiteBoardManager.getInst().stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);

        mMenu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_screen) {
            takeAndShowScreenshot();
            return true;
        }

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void takeAndShowScreenshot() {

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFreeDrawView.getDrawScreenshot(this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mSideView.setBackgroundColor(mFreeDrawView.getPaintColor());
    }

    private void changeColor() {
        int color = ColorHelper.getRandomMaterialColor(this);

        mFreeDrawView.setPaintColor(color);

        mSideView.setBackgroundColor(mFreeDrawView.getPaintColor());
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == mBtnRandomColor.getId()) {
            changeColor();
        }

        if (id == mBtnUndo.getId()) {
            mFreeDrawView.undoLast();
        }

        if (id == mBtnClearAll.getId()) {
            mFreeDrawView.undoAll();
        }

        if (id == mBtnSend.getId()) {
            send();
        }
    }

    private void send() {
        WhiteBoardManager wbm = WhiteBoardManager.getInst();
        wbm.setHandler(handler);
    }

    // SliderListener
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == mThicknessBar.getId()) {
            mFreeDrawView.setPaintWidthDp(THICKNESS_MIN + (progress * THICKNESS_STEP));
        } else {
            mFreeDrawView.setPaintAlpha(ALPHA_MIN + (progress * ALPHA_STEP));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onBackPressed() {
        if (mImgScreen.getVisibility() == View.VISIBLE) {
            mMenu.findItem(R.id.menu_screen).setVisible(true);
            mImgScreen.setImageBitmap(null);
            mImgScreen.setVisibility(View.GONE);

            mFreeDrawView.setVisibility(View.VISIBLE);
            mSideView.setVisibility(View.VISIBLE);

            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            super.onBackPressed();
        }
    }

    // PathRedoUndoCountChangeListener.
    @Override
    public void onUndoCountChanged(int undoCount) {
        mTxtUndoCount.setText(String.valueOf(undoCount));
    }

    @Override
    public void onRedoCountChanged(int redoCount) {
        // mTxtRedoCount.setText(String.valueOf(redoCount));
    }

    // PathDrawnListener
    @Override
    public void onNewPathDrawn() {
        // The user has finished drawing a path
    }

    @Override
    public void onTouch(int action, ArrayList<Point> points, int paintWidth, int paintColor, int paintAlpha) {
        if (true) {
            WhiteBoardManager wbm = WhiteBoardManager.getInst();
            Whiteboardmsg.TypeCommand type = Whiteboardmsg.TypeCommand.DrawPoint;
            Whiteboardmsg.TouchEvent touchEvent;
            if (action == MotionEvent.ACTION_DOWN) {
                touchEvent = Whiteboardmsg.TouchEvent.DOWN;
            } else if (action == MotionEvent.ACTION_MOVE) {
                touchEvent = Whiteboardmsg.TouchEvent.MOVE;
            } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                touchEvent = Whiteboardmsg.TouchEvent.UP;
            } else {
                return;
            }
            Whiteboardmsg.WhiteBoardMsg wmsg = wbm.newMsg(type, points, touchEvent, paintWidth,
                    paintColor, paintAlpha);

            wbm.addMessage(wmsg);

            if (true) {
                // 发送消息
                Message message = handler.obtainMessage();
                message.what = WhiteBoardManager.MsgWhatRecvWhiteMsg;
                message.arg1 = 1;
                Bundle bundle = new Bundle();
                bundle.putByteArray(WhiteBoardManager.MsgWhatRecvWhiteMsgBundle, wmsg.toByteArray());
                message.setData(bundle);
                handler.sendMessage(message);
            }
        }
    }

    @Override
    public void onUndoLast() {
        WhiteBoardManager wbm = WhiteBoardManager.getInst();
        Whiteboardmsg.TypeCommand type = Whiteboardmsg.TypeCommand.DrawUndo;
        Whiteboardmsg.WhiteBoardMsg wmsg = wbm.newMsg(type);
        wbm.addMessage(wmsg);
    }

    @Override
    public void onUndoAll() {
        WhiteBoardManager wbm = WhiteBoardManager.getInst();
        Whiteboardmsg.TypeCommand type = Whiteboardmsg.TypeCommand.DrawClearAll;
        Whiteboardmsg.WhiteBoardMsg wmsg = wbm.newMsg(type);
        wbm.addMessage(wmsg);
    }

    @Override
    public void onPathStart() {
        // The user has started drawing a path
    }


    // DrawCreatorListener
    @Override
    public void onDrawCreated(Bitmap draw) {
        mSideView.setVisibility(View.GONE);
        mFreeDrawView.setVisibility(View.GONE);

        mMenu.findItem(R.id.menu_screen).setVisible(false);

        mImgScreen.setVisibility(View.VISIBLE);

        mImgScreen.setImageBitmap(draw);
    }

    @Override
    public void onDrawCreationError() {
        Toast.makeText(this, "Error, cannot create bitmap", Toast.LENGTH_SHORT).show();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == WhiteBoardManager.MsgWhatRecvWhiteMsg) {
                Bundle bundle = msg.getData();
                WhiteBoardManager wbm = WhiteBoardManager.getInst();
                Whiteboardmsg.WhiteBoardMsg wmsg = wbm.byte2Msg(bundle.getByteArray(WhiteBoardManager.MsgWhatRecvWhiteMsgBundle));
                Whiteboardmsg.TypeCommand type = wmsg.getType();
                if (type == Whiteboardmsg.TypeCommand.DrawPoint) {
                    int count = wmsg.getPointCount();
                    ArrayList<android.graphics.Point> points = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        Whiteboardmsg.WhiteBoardMsg.Point wbp = wmsg.getPoint(i);
                        points.add(new android.graphics.Point(wbp.getX(), wbp.getY()));
                    }
                    int action;
                    Whiteboardmsg.TouchEvent touchEvent = wmsg.getTouchEvent();
                    if (touchEvent == Whiteboardmsg.TouchEvent.DOWN) {
                        action = MotionEvent.ACTION_DOWN;
                    } else if (touchEvent == Whiteboardmsg.TouchEvent.MOVE) {
                        action = MotionEvent.ACTION_MOVE;
                    } else if (touchEvent == Whiteboardmsg.TouchEvent.UP) {
                        action = MotionEvent.ACTION_UP;
                    } else {
                        return;
                    }
                    mFreeDrawView.onTouch(msg.arg1, action, points, wmsg.getPaintWidth(),
                            wmsg.getPaintColor(), wmsg.getPaintAlpha());
                } else if (type == Whiteboardmsg.TypeCommand.DrawUndo) {
                    mFreeDrawView.undoOtherLast();
                } else if (type == Whiteboardmsg.TypeCommand.DrawClearAll) {
                    mFreeDrawView.undoOtherAll();
                } else if (type == Whiteboardmsg.TypeCommand.DrawQuit) {
                    if (!wmsg.getUid().equals(wbm.getUserId())) {
                        finish();
                    }
                }
            }
        }
    };
}
