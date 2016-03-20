package com.example.activity;


import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ble.BluetoothLeClass;
import com.example.ble.BluetoothLeClass.OnDataAvailableListener;
import com.example.ble.BluetoothLeClass.OnDisconnectListener;
import com.example.ble.BluetoothLeClass.OnServiceDiscoverListener;
import com.example.blewaterdemo.R;
import com.example.view.WaterView;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
@SuppressLint({ "NewApi", "DefaultLocale" })
public class WaterActivity extends Activity implements OnDisconnectListener {
	private final static String TAG = WaterActivity.class.getSimpleName();
	private final static String UUID_KEY_DATA = "00001601-0000-1000-8000-00805f9b34fb";// 与水油外设通讯的UUid

	// private LeDeviceListAdapter mLeDeviceListAdapter;
	/** 搜索BLE终端 */
	private BluetoothAdapter mBluetoothAdapter;
	/** 读写BLE终端 */
	private BluetoothLeClass mBLE;
	private boolean mScanning;
	private Handler mHandler;


	private TextView mBtnBle_textview, textView_jiancejieguo;// 按钮暂时，注意做判断，在连接，检测等过程不可点击
	private TextView mTextView;
	private Button textView_back;

	// 水油
	private WaterView mSinkingView;

	private double percent = 0;
	private float Linepercent = 0;
	// private float waterdata=0;
	private boolean waterflag = true;
	private int str;// 水球代表的最终结果



	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ble_main);

		// getActionBar().setTitle(R.string.title_devices);

		initview();


		mHandler = new Handler() {//注意可能存在的内存泄漏
			@SuppressLint("HandlerLeak")
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 1:
					intBLETimeData++;
					if (intBLETimeData == 10) {
						mScanning = false;
						mBluetoothAdapter.stopLeScan(mLeScanCallback);
						mBtnBle_textview.setText("搜索停止，点击重搜");
						invalidateOptionsMenu();
						intBLETimeData = 0;
						startBLETimeThead = false;
					}
					break;

				case 2:
					if (View.INVISIBLE == textView_jiancejieguo.getVisibility()) {

						textView_jiancejieguo.setVisibility(View.VISIBLE);
					}
					if (str >= 20 && str < 34) {
						textView_jiancejieguo.setText("肌肤水分有点偏低咯");

					} else if (str >= 34 && str < 40) {
						textView_jiancejieguo.setText("肌肤水分在正常范围");

					} else if (str >= 40 && str <= 60) {
						textView_jiancejieguo.setText("肌肤细嫩，水分满满的");
					}

					break;

				default:
					break;
				}
			}
		};

		// Use this check to determine whether BLE is supported on the device.
		// Then you can
		// selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {

			Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT)
					.show();
			finish();
		} 

		// Initializes a Bluetooth adapter. For API level 18 and above, get a
		// reference to
		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Toast.makeText(this,"error_bluetooth_not_supported",
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		// 开启蓝牙
		mBluetoothAdapter.enable();

		mBLE = new BluetoothLeClass(this);
		if (!mBLE.initialize()) {
			// Log.e(TAG, "Unable to initialize Bluetooth");
			finish();
		}
		// 发现BLE终端的Service时回调
		mBLE.setOnServiceDiscoverListener(mOnServiceDiscover);
		// 收到BLE终端数据交互的事件
		mBLE.setOnDataAvailableListener(mOnDataAvailable);

		// mBLE.setOnConnectListener(this);
		mBLE.setOnDisconnectListener(this);

		mSinkingView = (WaterView) findViewById(R.id.sinking);

		initWaterView();

	}

	/**
	 * 初始化控件
	 */
	private void initview() {
		mBtnBle_textview = (TextView) findViewById(R.id.btnBle_Textview);
		mTextView = (TextView) findViewById(R.id.textBle);// 测试用，已隐藏
		textView_jiancejieguo = (TextView) findViewById(R.id.textView_jiancejieguo);
		textView_back = (Button) findViewById(R.id.textView_back);
		textView_back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
		mBtnBle_textview.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mBtnBle_textview.setText("正在连接");
				scanLeDevice(true);

			}
		});



	}

	private void initWaterView() {

		percent = 0.0f;
		Linepercent = 0f;
		mSinkingView.setPercent(percent, Linepercent);
	}

	boolean stopFlag = true;

	/**
	 * 绘制水球UI
	 * 
	 * @param s
	 */
	private void test(final double s) {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				double endS = s;
				// percent=0;
				// Linepercent = 0;
				Log.e(TAG, "最终结果：" + endS);
				while (percent <= s && stopFlag) {
					Log.e(TAG, "percentˮ============" + percent);
					mSinkingView.setPercent(percent, Linepercent);

					if (percent >= 0.21) {
						percent += 0.002f;
						try {
							Thread.sleep(10);// 100
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						percent += 0.001f;
						// Linepercent += 0.001f;
						try {
							Thread.sleep(30);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				}

				str = (int) (percent * 100);
				Log.e(TAG, "结果：：：" + str);

				Message message = Message.obtain();
				message.what = 2;
				message.arg1 = str;
				mHandler.sendMessage(message);

			}
		});
		thread.start();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// scanLeDevice(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		scanLeDevice(false);
		// mLeDeviceListAdapter.clear();
		mBLE.disconnect();
		if (timer != null) {

			timer.cancel();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		mBLE.close();
	}

	private int intBLETimeData = 0;
	boolean startBLETimeThead = true;;// 蓝牙定时开关标志

	/**
	 * 开启蓝牙搜索定时线程
	 */
	private void showBLETime() {

		new Thread() {
			public void run() {
				do {

					try {
						Thread.sleep(1000);
						Message message = Message.obtain();
						message.what = 1;
						mHandler.sendMessage(message);

					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} while (startBLETimeThead);

			};
		}.start();

	}

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			intBLETimeData = 0;
			startBLETimeThead = true;
			showBLETime();

			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
		invalidateOptionsMenu();
	}

	/**
	 * 搜索到BLE终端服务的事件
	 */
	private BluetoothLeClass.OnServiceDiscoverListener mOnServiceDiscover = new OnServiceDiscoverListener() {

		@Override
		public void onServiceDiscover(BluetoothGatt gatt) {
			displayGattServices(mBLE.getSupportedGattServices());
		}

	};

	/**
	 * 收到BLE终端数据交互的事件
	 */
	@SuppressLint("DefaultLocale")
	private BluetoothLeClass.OnDataAvailableListener mOnDataAvailable = new OnDataAvailableListener() {

		/**
		 * BLE终端数据被读的事件
		 */
		@Override
		public void onCharacteristicRead(final BluetoothGatt gatt,
				final BluetoothGattCharacteristic characteristic,
				final int status) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (status == BluetoothGatt.GATT_SUCCESS) {
						//触发外控传输 
						//...
						//假设接收数据为decrypt
						int decrypt = 50000;
						
						switch (decrypt) {
						case 0:
							if (View.VISIBLE == textView_jiancejieguo
									.getVisibility()) {

								textView_jiancejieguo
										.setVisibility(View.INVISIBLE);
							}
							mBtnBle_textview.setText("已经连接");
							mBtnBle_textview.setClickable(false);
							startBLETimeThead = false;
							break;

						case 2:
							if (View.VISIBLE == textView_jiancejieguo
									.getVisibility()) {

								textView_jiancejieguo
										.setVisibility(View.INVISIBLE);
							}
							mBtnBle_textview.setText("正在检测");
							mBtnBle_textview.setClickable(false);
							startBLETimeThead = false;
							stopFlag = true;

							if (waterflag) {
								initWaterView();
								test(0.21);
								waterflag = false;
							}

							break;

						case 3:
							if (View.VISIBLE == textView_jiancejieguo
									.getVisibility()) {

								textView_jiancejieguo
										.setVisibility(View.INVISIBLE);
							}
							mBtnBle_textview.setText("检测时间过短,请再次正确检测自动修正");
							mBtnBle_textview.setClickable(false);
							startBLETimeThead = false;
							// initWaterView();
							initWaterView();

							stopFlag = false;
							// test(0.0);
							break;

						case 4:
							if (View.VISIBLE == textView_jiancejieguo
									.getVisibility()) {

								textView_jiancejieguo
										.setVisibility(View.INVISIBLE);
							}
							mBtnBle_textview.setText("本次检测不稳定，请再次正确检测自动修正");
							mBtnBle_textview.setClickable(false);
							startBLETimeThead = false;
							initWaterView();

							stopFlag = false;
							break;

						}

						if (decrypt > 20000 && decrypt < 60000) {
							if (!waterflag) {

								double s = decrypt * 0.00001f;

								test(s);
								mBtnBle_textview.setText("检测完成");
								mBtnBle_textview.setClickable(false);
								startBLETimeThead = false;

							}
							double d = decrypt * 0.001f;
							mTextView.setText(str + "===" + decrypt * 0.001f
									+ "===" + percent);

							waterflag = true;

						} else if ((decrypt < 20000 || decrypt > 60000)
								&& decrypt != 0 && decrypt != 2 && decrypt != 3
								&& decrypt != 4) {
							mBtnBle_textview.setText(decrypt + "检测异常，请重新检测");
						}

					} else {
						mBtnBle_textview.setText("连接已断开,点击重搜");
						mBtnBle_textview.setClickable(true);
						intBLETimeData = 0;
						startBLETimeThead = false;
					}

				}
			});

		}

		/**
		 * 收到BLE终端写入数据回调
		 */
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			// Log.e(TAG, "onCharWrite " + gatt.getDevice().getName() +
			// " write "
			// + characteristic.getUuid().toString() + " -> "
			// + new String(characteristic.getValue()));
		}
	};

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// Log.e(TAG,
					// "" + device.getName() + "==" + device.getAddress());
					// 08-31 15:32:21.132: E/DeviceScanActivity(24289):
					// MyService==D5:BE:2A:55:B1:E8

					if (!device.getName().equals("MyService")) {
						// Log.e("连接设备：", "连接失败");
						return;
					}
					if (mScanning) {
						mBluetoothAdapter.stopLeScan(mLeScanCallback);
						mScanning = false;
					}

					boolean connect = mBLE.connect(device.getAddress());
					// Log.e("连接设备：", device.getAddress() + "是否true：" +
					// connect);
				}
			});
		}
	};

	private void displayGattServices(
			final List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;

		for (BluetoothGattService gattService : gattServices) {

			// -----Characteristics的字段信息-----//
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService
					.getCharacteristics();
			for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

				// UUID_KEY_DATA是可以跟蓝牙模块通信的Characteristic
				if (gattCharacteristic.getUuid().toString()
						.equals(UUID_KEY_DATA)) {
					// 测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()

					Time(gattCharacteristic);

				}

			}
		}//

	}

	Timer timer;

	private void Time(final BluetoothGattCharacteristic gattCharacteristic) {
		timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				mBLE.readCharacteristic(gattCharacteristic);
				Time(gattCharacteristic);

			}
		}, 200);// // 设定指定的时间time,此处为2000毫秒
	}

	@Override
	public void onDisconnect(BluetoothGatt gatt) {
		mBtnBle_textview.setText("连接已断开,点击重搜");
		mBtnBle_textview.setClickable(true);
		intBLETimeData = 0;
		startBLETimeThead = false;
		// Log.e(TAG, "断开连接了");

	}

}