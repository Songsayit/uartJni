
package com.htfyun.uartJni;
public class UartJni {
	
	/*****************************************************************/
	/*****************************************************************/
	/*****                                                       *****/
	/*****       U A R T   C O N F I G E R                       *****/
	/*****                                                       *****/
	/*****************************************************************/
	/*****************************************************************/
	/**
	* 
	* @param fd---openUartChannel的返回值
	* @param speed---必须为(115200, 38400, 19200,  9600,  4800,  2400,  1200,  300)中的一个
	* @return true--ok,false--fail
	* 参考:uartSetSpeed(fd,9600); 
	*/
	public native boolean uartSetSpeed(int fd,int speed);

	/**
	* 
	* @param fd--openUartChannel的返回值
	* @param databits--数据位   取值为 7 或者8
	* @param stopbits--停止位   取值为 1 或者2
	* @param parity--效验类型 取值为N,E,O,S
	* 					N: No parity
	* 					E: even parity
	* 					O: odd parity
	* 					S: as no parity
	* @return true--ok,false--fail
	* 
	* 参考:uartSetParity(fd, 8, 1, 'N')
	*/
	public native boolean uartSetParity(int fd,int databits,int stopbits,int parity);
		
	
	
	/*****************************************************************/
	/*****************************************************************/
	/*****                                                       *****/
	/*****      U A R T   O P E R A T I O N                      *****/
	/*****                                                       *****/
	/*****************************************************************/
	/*****************************************************************/	

	/**
	 * 
	 * @param name (如/dev/ttyS0,/dev/ttyS1)
	 * @return fd---文件句柄
	 * 
	 * 参考:fd = openUartChannel("/dev/ttyS0")
	 */
	public native int openUartChannel(String name);
	
	
	/**
	 * 
	 * @param fd---openUartChannel的返回值
	 */
	public native void closeUartChannel(int fd);

	/**
	 * 
	 * @param fd--openUartChannel的返回值
	 * @param buf--写入串口的数据
	 * @param len--Data Length
	 * @return real write bytes
	 */
	public native int uartWriteBytes(int fd,byte[] buf,int len);
	
	/**
	 * 
	 * @param fd--openUartChannel的返回值
	 * @param buf--从串口读到的数据
	 * @param len--Need Data Length
	 * @return real read bytes
	 */
	public native int uartReadBytes(int fd,byte[] buf,int len);

	static
	{
		try {
			System.loadLibrary("uartJni");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
