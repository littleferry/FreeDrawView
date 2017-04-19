package com.hengqian.whiteboard.msg;

import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Created by Administrator on 2017/4/17.
 */

public class WhiteBoardManager {
    private static final String TAG = WhiteBoardManager.class.getSimpleName();
    public static final String MsgWhatRecvWhiteMsgBundle = "msg";
    public static final int MsgWhatRecvWhiteMsg = 10000;

    private static WhiteBoardManager gInst;
    private Handler handler;
    private boolean isStoped = false;
    private Object lock = new Object();
    private String send_mqname;
    private String recv_mqname;
    private boolean durable = false;
    private String userId;
    private String send_user;
    private String send_pass;
    private String recv_user;
    private String recv_pass;

    public static WhiteBoardManager getInst() {
        if (gInst == null) {
            gInst = new WhiteBoardManager();
        }
        return gInst;
    }

    private Thread recvThread;
    private Thread sendThread;

    private ArrayList<Whiteboardmsg.WhiteBoardMsg> msgArrayList = new ArrayList<>();

    private WhiteBoardManager() {
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void start() {
        isStoped = false;
        //开启发送者线程
        startSendThread();
        //开启消费者线程
        startRecvThread();
    }

    public void stop() {
        handler = null;
        isStoped = true;
        if (sendThread != null) {
            sendThread.interrupt();
            sendThread = null;
        }
        if (recvThread != null) {
            recvThread.interrupt();
            recvThread = null;
        }
    }

    /**
     * 连接设置
     */
    private void setupConnectionFactory(ConnectionFactory factory, String user, String pass) {
        //factory.setHost("192.168.1.19");
        factory.setHost("180.150.186.42");
        factory.setPort(5672);
        factory.setUsername(user);
        factory.setPassword(pass);
    }

    /**
     * 消费者线程
     */
    private void startRecvThread() {
        if (recvThread != null) {
            return;
        }
        recvThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "recv thread start. " + this);
                while (true) {
                    if (isStoped) {
                        break;
                    }
                    Connection connection = null;
                    Channel channel = null;
                    try {
                        ConnectionFactory recvFactory = new ConnectionFactory();
                        setupConnectionFactory(recvFactory, recv_user, recv_pass);
                        //使用之前的设置，建立连接
                        connection = recvFactory.newConnection();
                        //创建一个通道
                        channel = connection.createChannel();
                        //一次只发送一个，处理完成一个再获取下一个
                        channel.basicQos(1);
                        // AMQP.Queue.DeclareOk q = channel.queueDeclare();
                        //将队列绑定到消息交换机exchange上
                        //                  queue         exchange              routingKey路由关键字，exchange根据这个关键字进行消息投递。
                        //channel.queueBind(q.getQueue(), "", "");
                        channel.queueDeclare(recv_mqname, durable, false, false, null);

                        //创建消费者
                        QueueingConsumer consumer = new QueueingConsumer(channel);
                        channel.basicConsume(recv_mqname, true, consumer);

                        while (true) {
                            if (isStoped) {
                                break;
                            }
                            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                            Whiteboardmsg.WhiteBoardMsg wmsg = byte2Msg(delivery.getBody());
                            Log.i(TAG, "recv thread get delivery. msg: " + wmsg);
                            if (handler != null) {
                                //从message池中获取msg对象更高效
                                Message msg = handler.obtainMessage();
                                msg.what = MsgWhatRecvWhiteMsg;
                                msg.arg1 = 0;
                                Bundle bundle = new Bundle();
                                bundle.putByteArray(MsgWhatRecvWhiteMsgBundle, delivery.getBody());
                                msg.setData(bundle);
                                handler.sendMessage(msg);
                            }

                            try {
                                Thread.sleep(10); //sleep and then try again
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        channel.close();
                        connection.close();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        if (channel!=null) {
                            try {
                                channel.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } catch (TimeoutException e1) {
                                e1.printStackTrace();
                            }
                        }
                        if (connection!=null) {
                            try {
                                connection.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        break;
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        try {
                            Thread.sleep(5000); //sleep and then try again
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }
                recvThread = null;
                Log.i(TAG, "recv thread end. " + this);
            }
        });
        recvThread.start();
    }

    /**
     * 消费者线程
     */
    public void startSendThread() {
        if (sendThread != null) {
            return;
        }
        sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "send thread start. " + this);
                while (true) {
                    if (isStoped) {
                        break;
                    }
                    Connection connection = null;
                    Channel channel = null;
                    try {
                        ConnectionFactory factory = new ConnectionFactory();
                        setupConnectionFactory(factory, send_user, send_pass);

                        connection = factory.newConnection();
                        channel = connection.createChannel();

                        channel.queueDeclare(send_mqname, durable, false, false, null);

                        while (true) {
                            synchronized (lock) {
                                int size = msgArrayList.size();
                                if (size > 0) {
                                    Whiteboardmsg.WhiteBoardMsg msg = msgArrayList.get(0);
                                    msgArrayList.remove(0);
                                    Log.i(TAG, "send thread send message. msg: " + msg.toString());
                                    channel.basicPublish("", send_mqname, null, msg.toByteArray());

                                    // 退出白板
                                    if (msg.getType() == Whiteboardmsg.TypeCommand.DrawQuit) {
                                        break;
                                    }
                                } else {
                                    // 等待新的消息加入
                                    lock.wait();
                                }
                            }
                        }
                        channel.close();
                        connection.close();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        if (channel!=null) {
                            try {
                                channel.close();
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                        if (connection!=null) {
                            try {
                                connection.close();
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (isStoped) {
                            break;
                        }
                        try {
                            Thread.sleep(5000); //sleep and then try again
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                            break;
                        }
                    }
                }
                sendThread = null;
                Log.i(TAG, "send thread end. " + this);
            }
        });
        sendThread.start();
    }

    public void addMessage(Whiteboardmsg.WhiteBoardMsg msg) {
        synchronized (lock) {
            msgArrayList.add(msg);
            lock.notifyAll();
        }
    }

    /**
     * 场景1 一个P向queue发送一个message，一个C从该queue接收message
     */
    public Whiteboardmsg.WhiteBoardMsg newMsg(Whiteboardmsg.TypeCommand type,
                                              List<Point> points, Whiteboardmsg.TouchEvent touchEvent,
                                              int paintWidth, int paintColor, int paintAlpha) {
        Whiteboardmsg.WhiteBoardMsg.Builder b = Whiteboardmsg.WhiteBoardMsg.newBuilder();
        b.setType(type);
        b.setUid(getUserId());

        b.setPaintAlpha(paintAlpha);
        b.setPaintColor(paintColor);
        b.setPaintWidth(paintWidth);

        // 可选参数
        b.setTouchEvent(touchEvent);
        for (int j = 0; j < points.size(); j++) {
            Whiteboardmsg.WhiteBoardMsg.Point.Builder wbpb = Whiteboardmsg.WhiteBoardMsg.Point.newBuilder();
            wbpb.setX(points.get(j).x);
            wbpb.setY(points.get(j).y);
            b.addPoint(wbpb.build());
        }
        return b.build();
    }

    /**
     * 场景1 一个P向queue发送一个message，一个C从该queue接收message
     */
    public Whiteboardmsg.WhiteBoardMsg newMsg(Whiteboardmsg.TypeCommand type) {
        Whiteboardmsg.WhiteBoardMsg.Builder b = Whiteboardmsg.WhiteBoardMsg.newBuilder();
        b.setType(type);
        b.setUid(getUserId());
        return b.build();
    }

    public Whiteboardmsg.WhiteBoardMsg byte2Msg(byte[] data) {
        Whiteboardmsg.WhiteBoardMsg msg =
                null;
        try {
            msg = Whiteboardmsg.WhiteBoardMsg.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return msg;
    }

    public String getUserId() {
        return userId;
    }

    public void setUser(int user) {
        userId = "用户" + user;
        if (user == 1) {
            send_mqname = "Queue1";
            recv_mqname = "Queue2";
            send_user = "send";
            send_pass = "123";
            recv_user = "recv";
            recv_pass = "123";
        } else {
            send_mqname = "Queue2";
            recv_mqname = "Queue1";
            send_user = "recv";
            send_pass = "123";
            recv_user = "send";
            recv_pass = "123";
        }
    }
}