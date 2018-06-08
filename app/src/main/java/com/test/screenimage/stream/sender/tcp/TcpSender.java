package com.test.screenimage.stream.sender.tcp;

import com.test.screenimage.configuration.VideoConfiguration;
import com.test.screenimage.entity.Frame;
import com.test.screenimage.stream.packer.TcpPacker;
import com.test.screenimage.stream.sender.OnSenderListener;
import com.test.screenimage.stream.sender.Sender;
import com.test.screenimage.stream.sender.sendqueue.ISendQueue;
import com.test.screenimage.stream.sender.sendqueue.NormalSendQueue;
import com.test.screenimage.stream.sender.sendqueue.SendQueueListener;
import com.test.screenimage.stream.sender.tcp.interf.TcpConnectListener;
import com.test.screenimage.utils.WeakHandler;


/**
 * Created by wt
 * tcp发送
 */

public class TcpSender implements Sender, SendQueueListener {
    private ISendQueue mSendQueue = new NormalSendQueue();
    private static final String TAG = "TcpSender";
    private OnSenderListener sendListener;
    private TcpConnection mTcpConnection;
    private WeakHandler weakHandler = new WeakHandler();
    private String ip;
    private int port;


    public TcpSender(String ip, int port) {
        mTcpConnection = new TcpConnection();
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void start() {
        mSendQueue.setSendQueueListener(this);
        mSendQueue.start();
    }

    public void setVideoParams(VideoConfiguration videoConfiguration) {
        mTcpConnection.setVideoParams(videoConfiguration);
    }

    // TODO: 2018/5/29 wt修改
    @Override
    public void onData(byte[] data, int type) {
        Frame frame = null;
        if (type == TcpPacker.FIRST_VIDEO) {
            frame = new Frame(data, type, Frame.FRAME_TYPE_CONFIGURATION);
        } else if (type == TcpPacker.KEY_FRAME) {
            frame = new Frame(data, type, Frame.FRAME_TYPE_KEY_FRAME);
        } else if (type == TcpPacker.INTER_FRAME) {
            frame = new Frame(data, type, Frame.FRAME_TYPE_INTER_FRAME);
        } else if (type == TcpPacker.AUDIO) {
            frame = new Frame(data, type, Frame.FRAME_TYPE_AUDIO);
        }
        if (frame == null) {
            return;
        }
        mSendQueue.putFrame(frame);
    }


    @Override
    public void stop() {
        mTcpConnection.stop();
        mSendQueue.stop();
    }




    @Override
    public void good() {
        weakHandler.post(new Runnable() {
            @Override
            public void run() {
                //网络好
                sendListener.onNetGood();
            }
        });
    }

    @Override
    public void bad() {
        weakHandler.post(new Runnable() {
            @Override
            public void run() {
                //网络差
                sendListener.onNetBad();
            }
        });
    }

    // TODO: 2018/6/4 监听回调
    private TcpConnectListener mTcpListener = new TcpConnectListener() {
        @Override
        public void onSocketConnectSuccess() {
//            connected();
        }

        @Override
        public void onSocketConnectFail() {
            disConnected();
        }

        @Override
        public void onTcpConnectSuccess() {
//           connect();
        }

        @Override
        public void onTcpConnectFail() {
          disConnected();
        }

        @Override
        public void onPublishSuccess() {
         connect();
        }

        @Override
        public void onPublishFail() {
            //数据发送失败
            weakHandler.post(new Runnable() {
                @Override
                public void run() {
                    sendListener.onPublishFail();
                }
            });
        }

        @Override
        public void onSocketDisconnect() {
            disConnected();
        }

    };

    public void connect() {
        //设置缓存队列
        mTcpConnection.setSendQueue(mSendQueue);
        new Thread(new Runnable() {
            @Override
            public void run() {
                connectNotInUi();
            }
        }).start();

    }

    private synchronized void connectNotInUi() {
        //设置连接回调
        mTcpConnection.setConnectListener(mTcpListener);
        //开始连接服务器
        mTcpConnection.connect(ip, port);
    }
    // TODO: 2018/6/6 连接成功
    private void connected() {
        weakHandler.post(new Runnable() {
            @Override
            public void run() {
                sendListener.onConnected();
            }
        });
    }


    // TODO: 2018/6/6 连接失败
    private void disConnected() {
        weakHandler.post(new Runnable() {
            @Override
            public void run() {
                sendListener.onDisConnected();
            }
        });
    }
    public void setSenderListener(OnSenderListener listener) {
        this.sendListener = listener;
    }

    /**
     * add by xu.wang 为解决首次黑屏而加
     */
    public void setSpsPps(byte[] spsPps) {
        if (mTcpConnection != null) mTcpConnection.setSpsPps(spsPps);
    }

}