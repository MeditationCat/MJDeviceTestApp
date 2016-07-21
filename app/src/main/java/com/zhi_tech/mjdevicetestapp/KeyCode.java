package com.zhi_tech.mjdevicetestapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zhi_tech.magiceyessdk.MagicEyesActivity;
import com.zhi_tech.magiceyessdk.Utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by taipp on 5/20/2016.
 */
public class KeyCode extends MagicEyesActivity implements OnClickListener {
    SharedPreferences mSp;
    TextView mInfo, mJoyStickInfo;
    Button mBtOk;
    Button mBtFailed;
    private GridView mGrid;
    private MySurfaceView joyStickView;
    private TouchPadView touchPadView;
    private boolean enableJoyStickItem = false;
    private boolean enableTouchPadItem = true;
    private LinearLayout root;
    boolean isMeasured = false;
    public static ArrayList<Integer> itemIds = new ArrayList<Integer>();

    private Handler handler = new Handler();
    private boolean mCheckDataSuccess;
    private byte okFlag = 0x00;

    private final String TAG = "KeyCode";

    @Override
    public void OnServiceConnectedHandler(ComponentName componentName, IBinder iBinder) {
        SendCommand(Utils.CMD_RECV_TP_EVENT);
        SendCommand(Utils.CMD_RECV_TP_EVENT);
    }

    @Override
    protected void OnTouchPadActonEventHandler(final int[] values) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                touchPadView.onTouchEventHandler(values[0], values[1]);
                if (touchPadView.checkResultOk()) {
                    okFlag |= 0x0F;
                    checkDataSuccess();
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keycode);

        mSp = getSharedPreferences("MJDeviceTestApp", Context.MODE_PRIVATE);
        mInfo = (TextView) findViewById(R.id.keycode_info);
        mJoyStickInfo = (TextView) findViewById(R.id.keycode_joystick);
        mBtOk = (Button) findViewById(R.id.keycode_bt_ok);
        assert mBtOk != null;
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.keycode_bt_failed);
        assert mBtFailed != null;
        mBtFailed.setOnClickListener(this);
        mBtOk.setClickable(false);
        mBtFailed.setClickable(false);
        ArrayList<String> mListData = new ArrayList<String>();
        HashMap<String, Integer> keyMap = new HashMap<String, Integer>();
        initTestItems();
        for (int item : itemIds) {
            mListData.add(getString(item));
        }

        mGrid = (GridView) findViewById(R.id.keycode_grid);
        mGrid.setAdapter(new MyAdapter(this, mListData, keyMap));

        //获取布局文件中LinearLayout容器
        root = (LinearLayout) findViewById(R.id.paint_root);
        if (enableJoyStickItem) {
            joyStickView = new MySurfaceView(this, 0, 0, 0 * 5 / 14);
            root.addView(joyStickView);
        } else if (enableTouchPadItem) {
            touchPadView = new TouchPadView(this);
            root.addView(touchPadView);
            mJoyStickInfo.setText(getString(R.string.keycode_touchpad_info));
        }
        assert root != null;
        ViewTreeObserver vto = root.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!isMeasured) {
                    if (enableJoyStickItem) {
                        float circleX = root.getMeasuredWidth() / 2;
                        float circleY = root.getMeasuredHeight() / 2;
                        float circleR = (circleX > circleY ? circleY : circleX) * 5 / 7;
                        joyStickView.setMySurfaceView(circleX, circleY, circleR);
                    } else if (enableTouchPadItem) {
                        touchPadView.setTouchPadView(root.getMeasuredWidth(), root.getMeasuredHeight());
                    }
                    isMeasured = true;
                }
                return true;
            }
        });

        mCheckDataSuccess = false;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mCheckDataSuccess) {
                    mBtFailed.setBackgroundColor(Color.RED);
                    mBtOk.setBackgroundColor(Color.GRAY);
                }
                SaveToReport();
            }
        }, MJDeviceTestApp.ItemTestTimeout * 5 * 1000);
    }

    private void initTestItems() {
        itemIds.clear();
        if (!MJDeviceTestApp.IsFactoryMode) {
            //normal key
            itemIds.add(R.string.keycode_back);
            itemIds.add(R.string.keycode_vol_up);
            itemIds.add(R.string.keycode_vol_down);
            //touch pad
            itemIds.add(R.string.keycode_tp_singleclick);
            itemIds.add(R.string.keycode_tp_doubleclick);
            itemIds.add(R.string.keycode_tp_up);
            itemIds.add(R.string.keycode_tp_down);
            itemIds.add(R.string.keycode_tp_left);
            itemIds.add(R.string.keycode_tp_right);
        }
        //joystick
        if (enableJoyStickItem) {
            itemIds.add(R.string.keycode_button_l2);
            itemIds.add(R.string.keycode_button_l1);
            itemIds.add(R.string.keycode_button_y);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown keyCode->" + String.valueOf(keyCode));
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp keyCode->" + String.valueOf(keyCode));
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyUp(keyCode, event);
        }
        return true; //super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyLongPress keyCode->" + String.valueOf(keyCode));
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyLongPress(keyCode, event);
        }
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEvent keyCode->" + event.getKeyCode() + "-->" + event.toString());
        MyAdapter myAdapter = (MyAdapter) mGrid.getAdapter();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                //normal key
                case KeyEvent.KEYCODE_BUTTON_L1:
                    myAdapter.setKeyMap(getString(R.string.keycode_back), 1);
                    break;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    myAdapter.setKeyMap(getString(R.string.keycode_vol_up), 1);
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    myAdapter.setKeyMap(getString(R.string.keycode_vol_down), 1);
                    break;
                //touch pad
                //case KeyEvent.KEYCODE_ENTER: //single click
                case KeyEvent.KEYCODE_BUTTON_L2:
                    myAdapter.setKeyMap(getString(R.string.keycode_tp_singleclick), 1);
                    break;
                //case KeyEvent.KEYCODE_MENU: //double click menu
                case KeyEvent.KEYCODE_BUTTON_Y:
                    myAdapter.setKeyMap(getString(R.string.keycode_tp_doubleclick), 1);
                    break;
                case KeyEvent.KEYCODE_DPAD_UP: //up -> down
                    myAdapter.setKeyMap(getString(R.string.keycode_tp_up), 1);
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN: //down -> up
                    myAdapter.setKeyMap(getString(R.string.keycode_tp_down), 1);
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT: //right -> left
                    myAdapter.setKeyMap(getString(R.string.keycode_tp_left), 1);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT: // left -> right
                    myAdapter.setKeyMap(getString(R.string.keycode_tp_right), 1);
                    break;
                /*//joystick
                case KeyEvent.KEYCODE_BUTTON_L2:
                    myAdapter.setKeyMap(getString(R.string.keycode_button_l2), 1);
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    myAdapter.setKeyMap(getString(R.string.keycode_button_l1), 1);
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    myAdapter.setKeyMap(getString(R.string.keycode_button_y), 1);
                    break;
                */
                default:
                    break;
            }
            mGrid.setAdapter(myAdapter);
            if (myAdapter.getKeyMaSize() == myAdapter.getCount()) {
                okFlag |= 0x80;
            }
            checkDataSuccess();
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            return super.dispatchKeyEvent(event);
        }
        return true; //super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if ((ev.getDevice().getSources() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0 && enableJoyStickItem) {
            Log.d(TAG, String.format("dispatchGenericMotionEvent ev->(%f, %f)", ev.getX(), ev.getY()));

            if (ev.getX() > 0.5) {
                okFlag |= 0x01;
            }
            if (ev.getX() < -0.5) {
                okFlag |= 0x02;
            }
            if (ev.getY() > 0.5) {
                okFlag |= 0x04;
            }
            if (ev.getY() < -0.5) {
                okFlag |= 0x08;
            }

            checkDataSuccess();

            joyStickView.updateGenericMotionEvent(ev);
        }
        return true; //super.dispatchGenericMotionEvent(ev);
    }

    public class MyAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<String> mDataList;
        private HashMap<String, Integer> keyMap;

        public MyAdapter(Context context, ArrayList<String> mDataList, HashMap<String, Integer> keyMap) {
            this.context = context;
            this.mDataList = mDataList;
            this.keyMap = keyMap;
        }

        @Override
        public int getCount() {
            return (mDataList == null) ? 0 : mDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return (mDataList == null) ? null : mDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(this.context).inflate(R.layout.keycode_grid,parent, false);
            }
            TextView textView = UtilTools.ViewHolder.get(convertView, R.id.factor_button);

            if (keyMap.containsKey(mDataList.get(position)) && keyMap.get(mDataList.get(position)) == 1) {
                textView.setBackgroundResource(R.drawable.btn_default_pressed);
            }
            textView.setText(mDataList.get(position));

            return convertView;
        }

        public void setKeyMap(String key, int value) {
            if (keyMap != null) {
                this.keyMap.put(key, value);
            }
        }

        public int getKeyMaSize() {
            if (keyMap == null) {
                return 0;
            }
            return keyMap.size();
        }

        public boolean keyMapContainsKey(Object key) {
            if (keyMap == null) {
                return false;
            }
            return keyMap.containsKey(key);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onDestroy();
        SendCommand(Utils.CMD_RECV_SENSOR_DATA);
        SendCommand(Utils.CMD_RECV_SENSOR_DATA);
    }

    @Override
    public void onClick(View v) {
        UtilTools.SetPreferences(this, mSp, R.string.KeyCode_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }

    public void SaveToReport() {
        UtilTools.SetPreferences(this, mSp, R.string.KeyCode_name,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, MJDeviceTestApp.ShowItemTestResultTimeout * 1000);
    }

    public void checkDataSuccess() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format("->okFlag = %#x", okFlag));
        if ((okFlag & 0x80) == 0x80 && (okFlag & 0x0F) == 0x0F && !mCheckDataSuccess) {
            mBtFailed.setBackgroundColor(Color.GRAY);
            mBtOk.setBackgroundColor(Color.GREEN);
            mCheckDataSuccess = true;
            SaveToReport();
        }
    }

    //draw view for joystick
    public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
        private Thread th;
        private SurfaceHolder sfh;
        private Canvas canvas;
        private Paint paint;
        private boolean flag;
        //固定摇杆背景圆形的X,Y坐标以及半径
        private float RockerCircleX = 100;
        private float RockerCircleY = 100;
        private float RockerCircleR = 50;
        //摇杆的X,Y坐标以及摇杆的半径
        private float SmallRockerCircleX = 100;
        private float SmallRockerCircleY = 100;
        private float SmallRockerCircleR = 20;
        public MySurfaceView(Context context) {
            super(context);
            Log.v("Himi", "MySurfaceView");
            this.setKeepScreenOn(true);
            sfh = this.getHolder();
            sfh.addCallback(this);
            paint = new Paint();
            paint.setAntiAlias(true);
            setFocusable(true);
            setFocusableInTouchMode(true);
        }
        public MySurfaceView(Context context, float circleX, float circleY, float circleR) {
            super(context);
            Log.v("Himi", "MySurfaceView");
            //this.setKeepScreenOn(true);
            this.RockerCircleX = circleX;
            this.RockerCircleY = circleY;
            this.RockerCircleR = circleR;
            this.SmallRockerCircleX = this.RockerCircleX;
            this.SmallRockerCircleY = this.RockerCircleY;
            this.SmallRockerCircleR = this.RockerCircleR  * 2 / 5;

            sfh = this.getHolder();
            sfh.addCallback(this);
            paint = new Paint();
            paint.setAntiAlias(true);
            setFocusable(true);
            setFocusableInTouchMode(true);
        }

        public void setMySurfaceView(float circleX, float circleY, float circleR) {
            this.RockerCircleX = circleX;
            this.RockerCircleY = circleY;
            this.RockerCircleR = circleR;
            this.SmallRockerCircleX = this.RockerCircleX;
            this.SmallRockerCircleY = this.RockerCircleY;
            this.SmallRockerCircleR = this.RockerCircleR  * 2 / 5;

            this.draw();
        }

        public void surfaceCreated(SurfaceHolder holder) {
            th = new Thread(this);
            flag = true;
            th.start();
        }
        /***
         * 得到两点之间的弧度
         */
        public double getRad(float px1, float py1, float px2, float py2) {
            //得到两点X的距离
            float x = px2 - px1;
            //得到两点Y的距离
            float y = py1 - py2;
            //算出斜边长
            float xie = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
            //得到这个角度的余弦值（通过三角函数中的定理 ：邻边/斜边=角度余弦值）
            float cosAngle = x / xie;
            //通过反余弦定理获取到其角度的弧度
            float rad = (float) Math.acos(cosAngle);
            //注意：当触屏的位置Y坐标<摇杆的Y坐标我们要取反值-0~-180
            if (py2 < py1) {
                rad = -rad;
            }
            return rad;
        }
/*
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN ||
                    event.getAction() == MotionEvent.ACTION_MOVE) {
                // 当触屏区域不在活动范围内
                if (Math.sqrt(Math.pow((RockerCircleX - (int) event.getX()), 2)
                        + Math.pow((RockerCircleY - (int) event.getY()), 2)) >= RockerCircleR) {
                    //得到摇杆与触屏点所形成的角度
                    double tempRad = getRad(RockerCircleX, RockerCircleY, event.getX(), event.getY());
                    //保证内部小圆运动的长度限制
                    getXY(RockerCircleX, RockerCircleY, RockerCircleR, tempRad);
                } else {//如果小球中心点小于活动区域则随着用户触屏点移动即可
                    SmallRockerCircleX = (int) event.getX();
                    SmallRockerCircleY = (int) event.getY();
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                //当释放按键时摇杆要恢复摇杆的位置为初始位置
                SmallRockerCircleX = RockerCircleX;
                SmallRockerCircleY = RockerCircleY;
            }
            return true;
        }
*/

        public boolean updateGenericMotionEvent(MotionEvent event) {

            float CosX = RockerCircleX + event.getX() * RockerCircleR;
            float CosY = RockerCircleY + event.getY() * RockerCircleR;

            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                // 当触屏区域不在活动范围内
                if (Math.sqrt(Math.pow((RockerCircleX - CosX), 2) + Math.pow((RockerCircleY - CosY), 2)) >= RockerCircleR) {
                    //得到摇杆与触屏点所形成的角度
                    double tempRad = getRad(RockerCircleX, RockerCircleY, CosX, CosY);
                    //保证内部小圆运动的长度限制
                    getXY(RockerCircleX, RockerCircleY, RockerCircleR, tempRad);
                } else {//如果小球中心点小于活动区域则随着用户触屏点移动即可
                    SmallRockerCircleX = CosX;
                    SmallRockerCircleY = CosY;
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                //当释放按键时摇杆要恢复摇杆的位置为初始位置
                SmallRockerCircleX = RockerCircleX;
                SmallRockerCircleY = RockerCircleY;
            }

            if (Math.abs(event.getX()) < 0.1 && Math.abs(event.getY()) < 0.1) {
                SmallRockerCircleX = RockerCircleX;
                SmallRockerCircleY = RockerCircleY;
            }
            this.draw();
            return true;//super.dispatchGenericMotionEvent(event);
        }

        /**
         *
         * @param R
         *            圆周运动的旋转点
         * @param centerX
         *            旋转点X
         * @param centerY
         *            旋转点Y
         * @param rad
         *            旋转的弧度
         */
        public void getXY(float centerX, float centerY, float R, double rad) {
            //获取圆周运动的X坐标
            SmallRockerCircleX = (float) (R * Math.cos(rad)) + centerX;
            //获取圆周运动的Y坐标
            SmallRockerCircleY = (float) (R * Math.sin(rad)) + centerY;
        }
        public void draw() {
            try {
                canvas = sfh.lockCanvas();
                canvas.drawColor(Color.WHITE);
                //设置透明度
                paint.setColor(0x70000000);
                //绘制摇杆背景
                canvas.drawCircle(RockerCircleX, RockerCircleY, RockerCircleR, paint);
                paint.setColor(0x70ff0000);
                //绘制摇杆
                canvas.drawCircle(SmallRockerCircleX, SmallRockerCircleY,
                        SmallRockerCircleR, paint);
            } catch (Exception e) {
                // TODO: handle exception
            } finally {
                try {
                    if (canvas != null)
                        sfh.unlockCanvasAndPost(canvas);
                } catch (Exception e2) {
                }
            }
        }
        public void run() {
            // TODO Auto-generated method stub
            while (flag) {
                draw();
                try {
                    Thread.sleep(50);
                } catch (Exception ex) {
                }
            }
        }
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.v("Himi", "surfaceChanged");
        }
        public void surfaceDestroyed(SurfaceHolder holder) {
            flag = false;
            Log.v("Himi", "surfaceDestroyed");
        }
    }

    // touch pad view
    public class TouchPadView extends View {
        //
        private final int mOffsetRangeX = 50;
        private final int mOffsetRangeY = 50;
        //
        private final int mRowCount = 3;
        private final int mColCount = 3;
        //
        private int mPaintX;
        private int mPaintY;
        private int mPaintWidth;
        private int mPaintHeight;
        //
        private int mGridWidth;
        private int mGridHeight;
        //
        private int mRowIndex;
        private int mColIndex;
        //
        private int[][] mTouchFlag;
        private Rect mRect;
        private Paint paint;


        public TouchPadView(Context context) {
            super(context);
            paint = new Paint();
            mRect = new Rect();
        }

        public void setTouchPadView(int width, int height) {
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format(Locale.US, "->width=%d, height=%d", width, height));
            mPaintWidth = height * 4 / 5;
            mPaintHeight = height * 4 / 5;
            mPaintX = (width - mPaintWidth) / 2;
            mPaintY = (height - mPaintHeight) / 2;
            //
            mGridWidth = mPaintWidth / mColCount;
            mGridHeight = mPaintHeight / mRowCount;
            mRowIndex = 0;
            mColIndex = 0;
            //
            mTouchFlag = new int[mRowCount][mColCount];

            for (int row = 0; row < mRowCount; row++) {
                for (int col = 0; col < mColCount; col++) {
                    mTouchFlag[row][col] = 0;
                }
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
            super.onDraw(canvas);
            canvas.drawColor(Color.WHITE);
            //draw grid line
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);
            for (int row = 0; row < mRowCount; row++) {
                for (int col = 0; col < mColCount; col++) {
                    mRect.set(mPaintX + col * mGridWidth,
                            mPaintY + row * mGridHeight,
                            mPaintX + (col + 1) * mGridWidth,
                            mPaintY + (row + 1) * mGridHeight);
                    canvas.drawRect(mRect, paint);
                }
            }
            // draw rect
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.FILL);
            for (int row = 0; row < mRowCount; row++) {
                for (int col = 0; col < mColCount; col++) {
                    if (mTouchFlag[row][col] == 1) {
                        mRect.set(mPaintX + col * mGridWidth,
                                mPaintY + row * mGridHeight,
                                mPaintX + (col + 1) * mGridWidth,
                                mPaintY + (row + 1) * mGridHeight);
                        canvas.drawRect(mRect, paint);
                    }
                }
            }
        }

        public void onTouchEventHandler(int pointX, int pointY) {
            //Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format(Locale.US, "->%d, %d", pointX, pointY));
            if (pointX == 0 && pointY == 0) {
                return;
            }
            mRowIndex = (mOffsetRangeY - pointY) / (mOffsetRangeY / mRowCount + 1);
            mColIndex = pointX / (mOffsetRangeX / mColCount + 1);

            mTouchFlag[mColIndex][mRowIndex] = 1;

            this.invalidate();
        }

        public boolean checkResultOk() {
            for (int row = 0; row < mRowCount; row++) {
                for (int col = 0; col < mColCount; col++) {
                    if (mTouchFlag[row][col] == 0) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
