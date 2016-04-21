#include <stdio.h> 
#include <stdlib.h> 
#include <unistd.h> 
#include <sys/types.h> 
#include <sys/stat.h> 
#include <fcntl.h> 
#include <termios.h> 
#include <errno.h> 
#include <android/log.h>

#include <string.h>

#include "uartJni.h"

#ifndef TRUE
   #define TRUE  1
   #define true 1
#endif

#ifndef FALSE
   #define FALSE 0
   #define false 0
#endif

#define LOGD(fmt,arg...)   __android_log_print(ANDROID_LOG_INFO, "uartJni", fmt, ##arg)
#if 0
	#define D(fmt,...)  LOGD(fmt,## __VA_ARGS__)
#else
	#define  D(...)   ((void)0)
#endif
/*****************************************************************/
/*****************************************************************/
/*****                                                             						*****/
/*****       U A R T   C O N F I G E R                       						*****/
/*****                                                       							*****/
/*****************************************************************/
/*****************************************************************/
static speed_t getBaudrate(int baudrate)
{
    switch(baudrate) {
    case 0: return B0;
    case 50: return B50;
    case 75: return B75;
    case 110: return B110;
    case 134: return B134;
    case 150: return B150;
    case 200: return B200;
    case 300: return B300;
    case 600: return B600;
    case 1200: return B1200;
    case 1800: return B1800;
    case 2400: return B2400;
    case 4800: return B4800;
    case 9600: return B9600;
    case 19200: return B19200;
    case 38400: return B38400;
    case 57600: return B57600;
    case 115200: return B115200;
    case 230400: return B230400;
    case 460800: return B460800;
    case 500000: return B500000;
    case 576000: return B576000;
    case 921600: return B921600;
    case 1000000: return B1000000;
    case 1152000: return B1152000;
    case 1500000: return B1500000;
    case 2000000: return B2000000;
    case 2500000: return B2500000;
    case 3000000: return B3000000;
    case 3500000: return B3500000;
    case 4000000: return B4000000;
    default: return B9600;
    }
}

/**
*@brief  设置串口通信速率
*@param  fd     类型 int  打开串口的文件句柄
*@param  speed  类型 int  串口速度
*@return  void
*/						
JNIEXPORT jboolean JNICALL Java_com_htfyun_uartJni_UartJni_uartSetSpeed
  (JNIEnv *env, jobject thiz, jint fd, jint baudrate)
  { 
  	int   i; 
	int   status; 
	struct termios   Opt;
	
	tcgetattr(fd, &Opt); 
	
	 //设置 串口的NL-CR 和CR-NL 的映射
	//http://www.dotblogs.com.tw/k/archive/2012/07/24/73572.aspx	
	Opt.c_iflag &= ~ (INLCR | ICRNL | IGNCR);
	Opt.c_oflag &= ~(ONLCR | OCRNL);
	Opt.c_iflag &= ~ (IXON | IXOFF | IXANY);


	speed_t speed = getBaudrate(baudrate);
	
	tcflush(fd, TCIOFLUSH);     
			
	cfsetispeed(&Opt, speed);  
	cfsetospeed(&Opt, speed);   
			
	status = tcsetattr(fd, TCSANOW, &Opt);
	
	return (status == 0);
  }

/**
*@brief   设置串口数据位，停止位和效验位
*@param  fd       类型  int  打开的串口文件句柄
*@param  databits 类型  int  数据位   取值为 7 或者8
*@param  stopbits 类型  int  停止位   取值为 1 或者2
*@param  parity   类型  int  效验类型 取值为N,E,O,S
*/
  JNIEXPORT jboolean JNICALL Java_com_htfyun_uartJni_UartJni_uartSetParity
  (JNIEnv *env, jobject thiz, jint fd, jint databits, jint stopbits, jint parity)
  {
  struct termios options; 
	
	if  (tcgetattr(fd,&options)  !=  0)
	{ 
		perror("SetupSerial 1");     
		return(FALSE);  
	}
	
	options.c_cflag &= ~CSIZE; 
    options.c_lflag &= ~(ICANON | ECHO |ECHOE | ISIG);
    
	switch (databits) /*设置数据位数*/
	{   
    	case 7:		
    		options.c_cflag |= CS7; 
    		break;
    	case 8:     
    		options.c_cflag |= CS8;
    		break;   
    	default:    
    		LOGD("Unsupported data size\n"); 
    		return (FALSE);  
	}
	
	/*设置奇偶校验位*/
    switch (parity) 
    {   
    	case 'n':
    	case 'N':    
    		options.c_cflag &= ~PARENB;     /* Clear parity enable */
    		options.c_iflag &= ~INPCK;      /* Enable parity checking */ 
    		break;  
    	case 'o':   
    	case 'O':     
    		options.c_cflag |= (PARODD | PARENB); /* 设置为奇效验*/  
    		options.c_iflag |= INPCK;             /* Disnable parity checking */ 
    		break;  
    	case 'e':  
    	case 'E':   
    		options.c_cflag |= PARENB;      /* Enable parity */    
    		options.c_cflag &= ~PARODD;     /* 转换为偶效验*/     
    		options.c_iflag |= INPCK;       /* Disnable parity checking */
    		break;
    	case 'S': 
    	case 's':  /*as no parity*/   
    	    options.c_cflag &= ~PARENB;
    		options.c_cflag &= ~CSTOPB;
    		break;  
    	default:   
    		LOGD("Unsupported parity\n");
    		return (FALSE);  
	}  
	
    /* 设置停止位*/  
    switch (stopbits)
    {   
    	case 1:    
    		options.c_cflag &= ~CSTOPB;  
    		break;  
    	case 2:    
    		options.c_cflag |= CSTOPB;  
    	   break;
    	default:    
    		 LOGD("Unsupported stop bits\n");
    		 return (FALSE); 
    } 
    
    /* Set input parity option */ 
    if ((parity != 'n') && (parity != 'N'))  
    { 
    	//options.c_iflag |= INPCK; 
    }
    
    tcflush(fd,TCIFLUSH);
    
    options.c_cflag |= CREAD; 
    options.c_cc[VTIME] = 0;        /* 设置超时0 seconds*/   
    options.c_cc[VMIN]  = 0;        /* Update the options and do it NOW */
    
    if (tcsetattr(fd,TCSANOW,&options) != 0)   
    { 
    	return (FALSE);  
    } 
    return (TRUE);  
  }

/*
  return: uart device handler
*/
  JNIEXPORT jint JNICALL Java_com_htfyun_uartJni_UartJni_openUartChannel
  (JNIEnv *env, jobject thiz, jstring name)
  {
  	const char * name_str;
	int fd = -1;
  	name_str = (*env)->GetStringUTFChars(env,name, false);
  	if(name_str == NULL)
  	{
  		return -1;
  	}

  	LOGD("open the uart -- %s.\n",name_str);
  	fd = open(name_str, O_RDWR | O_NOCTTY | O_NDELAY); 

	if(fd == -1)
	{
		LOGD("open %s failed.\n",name_str);
	}
	(*env)->ReleaseStringUTFChars(env,name, name_str);
  	return fd;
  }

/*
  param: fd = uart device handler
*/
  JNIEXPORT void JNICALL Java_com_htfyun_uartJni_UartJni_closeUartChannel
  (JNIEnv *env, jobject thiz, jint fd)
  {
  	close(fd);
  }

/*
  param: fd = uart device handler
  param:  buf = String Buffer Address
  param:  len = Data Length
  return: real write bytes
*/
  JNIEXPORT jint JNICALL Java_com_htfyun_uartJni_UartJni_uartWriteBytes
  (JNIEnv *env, jobject thiz, jint fd, jbyteArray arr, jint len)
  {
  	int tempLen = -1;
  	unsigned char *buf = (*env)->GetByteArrayElements(env,arr, false);
  	tempLen = write(fd, buf ,len);

  	(*env)->ReleaseByteArrayElements(env,arr, buf, 0);

  	return tempLen;
  }

 JNIEXPORT jint JNICALL Java_com_htfyun_uartJni_UartJni_uartReadBytes
  (JNIEnv *env, jobject thiz, jint fd, jbyteArray arr, jint len)
  {
  	int    tmplen = 0; 
	int    ret;
	int    retval;
	fd_set rfds;	
	struct timeval tv;	
	unsigned char * buf;
	buf = (unsigned char *)calloc(len,sizeof(char));
	
	tv.tv_sec  = 1;		//set the rcv wait time 
	tv.tv_usec = 0;		//1000us = 1ms	
    
   	FD_ZERO(&rfds); 		
	FD_SET(fd, &rfds);		
		
	retval = select(fd + 1,&rfds, NULL, NULL, &tv);	
	if(retval)
	{
		tmplen = 0;
		while(1) {
			ret = read(fd, buf, len);
			if (ret > 0) {
				tmplen += ret;
			} else {
				D("error while reading from gps daemon socket: %s:", strerror(errno)); 
				break;
			}
		}
    }   

   	(*env)->SetByteArrayRegion(env,arr, 0,tmplen,buf);
   	free(buf);
	return tmplen;
  }
