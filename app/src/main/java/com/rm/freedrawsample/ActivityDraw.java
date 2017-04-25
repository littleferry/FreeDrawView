package com.rm.freedrawsample;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Gallery;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class ActivityDraw extends AppCompatActivity
        implements View.OnClickListener, SeekBar.OnSeekBarChangeListener,
        PathRedoUndoCountChangeListener, PathDrawnListener, ViewPager.OnPageChangeListener, AdapterView.OnItemClickListener {

    private static final int MsgWhatExitWhiteBoard = 0;
    private static final int MsgWhatHeartbeat = 1;
    public static final String UserID = "user";

    private static final int THICKNESS_STEP = 1;
    private static final int THICKNESS_MAX = 30;
    private static final int THICKNESS_MIN = 1;

    private static final int ALPHA_STEP = 1;
    private static final int ALPHA_MAX = 255;
    private static final int ALPHA_MIN = 0;

    private FreeDrawView mFreeDrawView;
    private View mSideView;
    private Button mBtnRandomColor, mBtnUndo, mBtnClearAll, mBtnUserList;
    private SeekBar mThicknessBar, mAlphaBar;
    private TextView mTxtUndoCount;
    private Gallery gallery;
    private ColorsAdapter adapter;

    private ImageView mImgScreen;
    private Menu mMenu;
    private int user;

    private HashMap<String, User> mUserList = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
        mBtnUserList = (Button) findViewById(R.id.btn_user_list);
        mThicknessBar = (SeekBar) findViewById(R.id.slider_thickness);
        mAlphaBar = (SeekBar) findViewById(R.id.slider_alpha);

        mBtnRandomColor.setOnClickListener(this);
        mBtnUndo.setOnClickListener(this);
        mBtnClearAll.setOnClickListener(this);
        mBtnUserList.setOnClickListener(this);

        mAlphaBar.setMax((ALPHA_MAX - ALPHA_MIN) / ALPHA_STEP);
        mAlphaBar.setProgress(mFreeDrawView.getPaintAlpha());
        mAlphaBar.setOnSeekBarChangeListener(this);

        mThicknessBar.setMax((THICKNESS_MAX - THICKNESS_MIN) / THICKNESS_STEP);
        mThicknessBar.setOnSeekBarChangeListener(this);
        mThicknessBar.setProgress((int) mFreeDrawView.getPaintWidth());

        Intent intent = getIntent();
        user = intent.getIntExtra(UserID, 1);
        WhiteBoardManager wbm = WhiteBoardManager.getInst();
        mFreeDrawView.setDeviceId(wbm.getDeviceId());
        wbm.setUser(user);
        wbm.setHandler(handler);
        wbm.start();

        wbm.addMessage(Whiteboardmsg.TypeCommand.Join,
                mFreeDrawView.getMeasuredWidth(), mFreeDrawView.getMeasuredHeight());

        mUserList.put(wbm.getDeviceId(), new User(System.currentTimeMillis(), wbm.getUserInfo()));
        handler.sendEmptyMessage(MsgWhatHeartbeat);

        gallery = (Gallery) findViewById(R.id.gly_container);
        adapter = new ColorsAdapter(this);
        gallery.setAdapter(adapter); // 为viewpager设置adapter

        gallery.setOnItemClickListener(this);

        int index = 0;
        int color = ColorHelper.getColor(index);
        mFreeDrawView.setPaintColor(color);
        mSideView.setBackgroundColor(mFreeDrawView.getPaintColor());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WhiteBoardManager.getInst().addMessage(Whiteboardmsg.TypeCommand.Exit,
                mFreeDrawView.getMeasuredWidth(), mFreeDrawView.getMeasuredHeight());
        mFreeDrawView.stopPlayback();
        handler.sendEmptyMessageDelayed(MsgWhatExitWhiteBoard, 500);
        ColorHelper.recycleColors();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);

        mMenu = menu;

        updateMenuItem();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_playback) {
            playback();
            return true;
        }

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void playback() {
        if (mFreeDrawView.isPlaybacking()) {
            mFreeDrawView.stopPlayback();
        } else {
            mFreeDrawView.startPlayback();
        }
        updateMenuItem();
    }

    private void updateMenuItem() {
        MenuItem playback = mMenu.findItem(R.id.menu_playback);
        if (mFreeDrawView.isPlaybacking()) {
            playback.setTitle("停止回放");
            setTitle("正在回放");
        } else {
            playback.setTitle("开始回放");
            setTitle("用户:" + WhiteBoardManager.getInst().getDeviceId());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mSideView.setBackgroundColor(mFreeDrawView.getPaintColor());
    }

    private void changeColor() {
        int index = ColorHelper.getRandomColorIndex();
        int color = ColorHelper.getColor(index);
        mFreeDrawView.setPaintColor(color);
        mSideView.setBackgroundColor(mFreeDrawView.getPaintColor());
        adapter.setSelIndex(index);
        gallery.setSelection(index, true);
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

        if (id == mBtnUserList.getId()) {
            showUserListDialog();
        }
    }

    private void showUserListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("当前用户列表");
        StringBuffer sb = new StringBuffer();
        Set<String> keys = mUserList.keySet();
        Iterator<String> iterator = keys.iterator();
        int i = 1;
        while (iterator.hasNext()) {
            String str = iterator.next();
            sb.append(i);
            sb.append(" ");
            sb.append(str);
            sb.append(" ");
            sb.append(mUserList.get(str).getInfo());
            sb.append("\n");
            i++;
        }
        builder.setMessage(sb.toString());
        AlertDialog alert = builder.create();
        alert.show();
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

    // PathRedoUndoCountChangeListener.
    @Override
    public void onUndoCountChanged(int undoCount) {
        mTxtUndoCount.setText(String.valueOf(undoCount));
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
                    paintColor, paintAlpha, mFreeDrawView.getWidth(), mFreeDrawView.getHeight());

            wbm.addMessage(wmsg);

            if (true) {
                // 发送消息
                Message message = handler.obtainMessage();
                message.what = WhiteBoardManager.MsgWhatRecvWhiteMsg;
                Bundle bundle = new Bundle();
                bundle.putByteArray(WhiteBoardManager.MsgWhatRecvWhiteMsgBundle, wmsg.toByteArray());
                message.setData(bundle);
                handler.sendMessage(message);
            }
        }
    }

    @Override
    public void onUndoLast() {
        WhiteBoardManager.getInst().addMessage(Whiteboardmsg.TypeCommand.DrawUndo,
                mFreeDrawView.getWidth(), mFreeDrawView.getHeight());
    }

    @Override
    public void onUndoAll() {
        WhiteBoardManager.getInst().addMessage(Whiteboardmsg.TypeCommand.DrawClearAll,
                mFreeDrawView.getWidth(), mFreeDrawView.getHeight());
    }

    @Override
    public void onPathStart() {
        // The user has started drawing a path
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            WhiteBoardManager wbm = WhiteBoardManager.getInst();
            switch (msg.what) {
                case WhiteBoardManager.MsgWhatRecvWhiteMsg: {
                    Bundle bundle = msg.getData();
                    Whiteboardmsg.WhiteBoardMsg wmsg = wbm.byte2Msg(bundle.getByteArray(WhiteBoardManager.MsgWhatRecvWhiteMsgBundle));
                    Whiteboardmsg.TypeCommand type = wmsg.getType();
                    switch (type.getNumber()) {
                        case Whiteboardmsg.TypeCommand.DrawPoint_VALUE: {
                            int count = wmsg.getDrawPoint().getPointCount();
                            ArrayList<android.graphics.Point> points = new ArrayList<>();
                            for (int i = 0; i < count; i++) {
                                Whiteboardmsg.WhiteBoardMsg.DrawPoint.Point wbp = wmsg.getDrawPoint().getPoint(i);
                                points.add(new android.graphics.Point(wbp.getX(), wbp.getY()));
                            }
                            int action;
                            Whiteboardmsg.TouchEvent touchEvent = wmsg.getDrawPoint().getTouchEvent();
                            if (touchEvent == Whiteboardmsg.TouchEvent.DOWN) {
                                action = MotionEvent.ACTION_DOWN;
                            } else if (touchEvent == Whiteboardmsg.TouchEvent.MOVE) {
                                action = MotionEvent.ACTION_MOVE;
                            } else if (touchEvent == Whiteboardmsg.TouchEvent.UP) {
                                action = MotionEvent.ACTION_UP;
                            } else {
                                return;
                            }
                            mFreeDrawView.onTouch(wmsg.getUid(), action, points, wmsg.getDrawPoint().getPaint().getWidth(),
                                    wmsg.getDrawPoint().getPaint().getColor(), wmsg.getDrawPoint().getPaint().getAlpha(),
                                    wmsg.getSize().getW(), wmsg.getSize().getH());
                            mUserList.put(wmsg.getUid(), new User(System.currentTimeMillis(), wmsg.getUserInfo()));
                            notifyUserListChanged();
                            break;
                        }
                        case Whiteboardmsg.TypeCommand.DrawUndo_VALUE: {
                            mFreeDrawView.undoLast(wmsg.getUid());
                            mUserList.put(wmsg.getUid(), new User(System.currentTimeMillis(), wmsg.getUserInfo()));
                            notifyUserListChanged();
                            break;
                        }
                        case Whiteboardmsg.TypeCommand.DrawClearAll_VALUE: {
                            mFreeDrawView.undoAll(wmsg.getUid());
                            mUserList.put(wmsg.getUid(), new User(System.currentTimeMillis(), wmsg.getUserInfo()));
                            notifyUserListChanged();
                            break;
                        }
                        case Whiteboardmsg.TypeCommand.Join_VALUE: {
                            Toast.makeText(ActivityDraw.this, wmsg.getUid() + "\n" + wmsg.getUserInfo() +
                                    " 已加入", Toast.LENGTH_LONG).show();
                            mUserList.put(wmsg.getUid(), new User(System.currentTimeMillis(), wmsg.getUserInfo()));
                            notifyUserListChanged();
                            break;
                        }
                        case Whiteboardmsg.TypeCommand.Exit_VALUE: {
                            Toast.makeText(ActivityDraw.this, wmsg.getUid() + "\n" + wmsg.getUserInfo() +
                                    " 已退出", Toast.LENGTH_LONG).show();
                            mUserList.remove(wmsg.getUid());
                            notifyUserListChanged();
                            break;
                        }
                        case Whiteboardmsg.TypeCommand.Heartbeat_VALUE: {
                            // 心跳协议
                            mUserList.put(wmsg.getUid(), new User(System.currentTimeMillis(), wmsg.getUserInfo()));
                            notifyUserListChanged();
                            break;
                        }
                    }
                    break;
                }

                case MsgWhatExitWhiteBoard: {
                    WhiteBoardManager.getInst().stop();
                    break;
                }
                case MsgWhatHeartbeat: {
                    handler.sendEmptyMessageDelayed(MsgWhatHeartbeat, 5000);
                    mUserList.put(wbm.getDeviceId(), new User(System.currentTimeMillis(), wbm.getUserInfo()));
                    wbm.addMessage(Whiteboardmsg.TypeCommand.Heartbeat, 0, 0);
                    notifyUserListChanged();
                    break;
                }
            }
        }
    };

    private void notifyUserListChanged() {
        // 检查是否有超过10秒没有心跳活动的用户，如果存在，移除
        Set<String> keys = mUserList.keySet();
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            User user = mUserList.get(key);
            if (System.currentTimeMillis() - user.getTime() > 10000) {
                mUserList.remove(key);
                iterator = keys.iterator();
            }
        }
        mBtnUserList.setText("用户(" + mUserList.size() + ")");
    }


    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (gallery != null) {
            gallery.invalidate();
        }
    }

    // 一个新页被调用时执行,仍为原来的page时，该方法不被调用
    public void onPageSelected(int position) {
        // tvTitle.setText(getFile(position));
    }

    /*
     * SCROLL_STATE_IDLE: pager处于空闲状态 SCROLL_STATE_DRAGGING： pager处于正在拖拽中
     * SCROLL_STATE_SETTLING： pager正在自动沉降，相当于松手后，pager恢复到一个完整pager的过程
     */
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mFreeDrawView.setPaintColor(ColorHelper.getColor(position));
        mSideView.setBackgroundColor(mFreeDrawView.getPaintColor());
        adapter.setSelIndex(position);
    }
}
