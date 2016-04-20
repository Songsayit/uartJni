package com.htfyun.uartJni;

import android.util.Log;

public class UartNativeController {
	private final String TAG = "UartNativeController";
	private final String UART_CHANNEL = "/dev/ttyS1";
	private final int UART_BAUDRATE = 9600;//115200;

	private static final UartJni mUartJni = new UartJni();
	private int mUartFd = -1;
	
	private Object mReadLocked = new Object();
	private Object mWriteLocked = new Object();

	private static UartNativeController instance; //虽然TTYS3可以被打开多次, 但是电源控制只有一个. 所以我们还是采用单例模式

	public static UartNativeController getInstance() {
		if (instance == null) {
			instance = new UartNativeController();
		}
		return instance;
	}
	private UartNativeController() {
	}

	private boolean isOpen = false;
	
	public synchronized boolean openUart() {
		boolean ret = false;
		
		Log.e(TAG, "openUart....");
		
		//保证串口只能打开一次. 测试时发现ttyS3居然可以打开很多次. 这与我在PC上打开串口的现象不一样. PC机上打开串口只能被一个应用程序占用
		if (isUartOpen()) {
			Log.w(TAG, UART_CHANNEL + " has been opened. fd = " + mUartFd);
			return ret;
		}
		
		isOpen = true;
		
		int fd = -1;
		try {
			fd = mUartJni.openUartChannel(UART_CHANNEL);
		} catch (Exception e) {
			// TODO: handle exception
			Log.e(TAG, "openUartChannel failed");
		}

		if (fd == -1) {
			Log.e(TAG, "open " + UART_CHANNEL + " failed");
		} else {

			mUartJni.uartSetSpeed(fd, UART_BAUDRATE);
			mUartJni.uartSetParity(fd, 8, 1, 'N');

			mUartFd = fd;

			Log.d(TAG, "open " + UART_CHANNEL + " ok, mUartFd = " + mUartFd);
			ret = true;
		}

		return ret;
	}

	public synchronized void closeUartFd() throws Throwable {
		if (!isUartOpen()) {
			Log.w(TAG, UART_CHANNEL + "do NOT open. So closeUartFd failed");
			return;
		}
		
		Log.d(TAG, "close " + UART_CHANNEL + ", mUartFd = " + mUartFd);
		try {
			mUartJni.closeUartChannel(mUartFd);
			mUartFd = -1;
			isOpen = false;
		} finally {
			super.finalize();
		}
	}

	public int getUartFd() {
		return mUartFd;
	}
	
	public boolean isUartOpen() {
		return (mUartFd > 0 && isOpen == true);
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
				len = mUartJni.uartWriteBytes(mUartFd, buf, length);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return len;
	}

	/**
	 * 
	 * @param buf
	 *            ： 存储读到数据
	 * @param length
	 *            ： 需要读多长的数据
	 * @return 真实读到的数据长度 len
	 */
	public int readUart(byte[] buf, int length) {
		int len = 0;
		if (mUartFd < 0 )
			return len;
		
		try {
			synchronized (mReadLocked) {
				len = mUartJni.uartReadBytes(mUartFd, buf, length);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e(TAG, " uart read data err");
			e.printStackTrace();
		}
		return len;
	}

}