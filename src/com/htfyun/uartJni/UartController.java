package com.htfyun.uartJni;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.text.TextUtils;
import android.util.Log;

//ttyS* 可以被多个进程打开多次的.
public class UartController {
	private final String TAG = "UartController";
	private final String UART_DEFAULT_CHANNEL = "/dev/ttyS1";
	private final int UART_DEFAULT_BAUDRATE = 9600;//115200;

	private int mUartFd = -1;
	
	private Object mReadLocked = new Object();
	private Object mWriteLocked = new Object();

	private final String mUartChannel;
	private final int mBaudRate;
	
	public interface OnUartReadListener {
		public int onUartReadListener(byte[] buf, int length);
	}
	
	private List<OnUartReadListener> mUartReadListenerList = new CopyOnWriteArrayList<OnUartReadListener>();
	private Thread mReadThread;
	private boolean mReadEnabled = false;
	
	public UartController(String channel, int baudRate) {
		String chn = channel;
		if (TextUtils.isEmpty(chn)) {
			chn = UART_DEFAULT_CHANNEL;
		}
		
		int rate = baudRate;
		if (rate < 0) {
			rate = UART_DEFAULT_BAUDRATE;
		}
		
		mUartChannel = chn;
		mBaudRate = rate;
	}
	
	
	public boolean registerUartReadListener(OnUartReadListener listener) {
		boolean openOk = openUart();
		if (!openOk) {
			return openOk;
		}
		
		if (!(mUartReadListenerList.contains(listener))) {
			mUartReadListenerList.add(listener);
		}
		
		if (!(mUartReadListenerList.isEmpty())) {
			startReadUart();
		}
		return true;
	}
	
	public boolean unregisterUartReadListener(OnUartReadListener listener) {
		
		mUartReadListenerList.remove(listener);
		
		if (mUartReadListenerList.isEmpty()) {
			stopReadUart();
		}
		return true;
	}
	
	public synchronized boolean openUart() {
		
		//保证串口只能打开一次. 测试时发现ttyS3居然可以打开很多次. 这与我在PC上打开串口的现象不一样. PC机上打开串口只能被一个应用程序占用
		if (isUartOpen()) {
			return true;
		}
		
		try {
			mUartFd = UartJni.openUartChannel(mUartChannel);
		} catch (Exception e) {
			// TODO: handle exception
			Log.e(TAG, "openUartChannel failed, msg = " + e.getMessage());
			return false;
		}
		
		if (mUartFd < 0) {
			Log.e(TAG, "open " + mUartChannel + " failed");
			return false;
		}


		UartJni.uartSetSpeed(mUartFd, mBaudRate);
		UartJni.uartSetParity(mUartFd, 8, 1, 'N');

		Log.d(TAG, "open " + mUartChannel + " ok" + ", baudrate =  " + mBaudRate + ", mUartFd = " + mUartFd);

		return true;
	}

	public synchronized void closeUartFd() {
		if (!isUartOpen()) {
			Log.w(TAG, mUartChannel + "do NOT open. So dont need to  close Uart");
			return;
		}
		
		Log.d(TAG, "close " + mUartChannel + ", mUartFd = " + mUartFd);
		try {
			
			stopReadUart();
			
			UartJni.closeUartChannel(mUartFd);
			mUartFd = -1;
		} finally {
			try {
				super.finalize();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public boolean isUartOpen() {
		return (mUartFd > 0);
	}

	/**
	 * 
	 * @param buf
	 * 			 : 要写入的数据
	 * @param length
	 * 			 : 写入数据长度
	 * @return
	 * 		 真正写的数据长度len
	 */
	public int writeUart(byte[] buf, int length) {
		int len = -1;
		if (mUartFd < 0) {
			return len;
		}
		try {
			synchronized (mWriteLocked) {
				len = UartJni.uartWriteBytes(mUartFd, buf, length);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return len;
	}
	
	
	private void startReadUart() {
	
		if (mReadThread == null) {
			mReadEnabled = true;
			mReadThread = new Thread(mRunnable);
			mReadThread.start();
		}
	}
	
	private void stopReadUart() {
		if (mReadThread != null && mReadThread.isAlive()) {
			try {
				mReadEnabled = false;
				mReadThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
			mReadThread = null;
		}
	}
	

	/**
	 * 
	 * @param buf
	 *            ： 存储读到数据
	 * @param length
	 *            ： 需要读多长的数据
	 * @return 真实读到的数据长度 len
	 */
	private int readUart(byte[] buf, int length) {
		int len = 0;
		if (mUartFd < 0 )
			return len;
		
		try {
			synchronized (mReadLocked) {
				len = UartJni.uartReadBytes(mUartFd, buf, length);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e(TAG, " uart read data err");
			e.printStackTrace();
		}
		return len;
	}
	
	private Runnable mRunnable = new Runnable() {
		private byte[] buffer = new byte[512];
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(mReadEnabled) {
				int len = readUart(buffer, buffer.length);
				if (len > 0 ) {
					for (OnUartReadListener l : mUartReadListenerList) {
						l.onUartReadListener(buffer.clone(), len);
					}
				}
			}
		}
	};

}