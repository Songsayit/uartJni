package com.htfyun.uartJni;

import com.htfyun.uartJni.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class UartJniActivity extends Activity {
    /** Called when the activity is first created. */
	private final String TAG = "uartJniTest";
	 UartJni uartJni = new UartJni();
	 int fd;
	 
	 private TextView txt_show = null;
	 private EditText edt_tty;
	 private EditText edt_command;
	 private Button btn_start_tty = null;
	 private Button btn_send_command = null;
	 private Button btn_send_hex_command = null;
	 private Context mContext;
	 
	 private byte[] hex = {(byte) 0xaa, 0x55, 0x2d, 0x03, (byte) 0xff, 0x10, (byte) 0xc1};
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mContext = this;
        txt_show = (TextView)findViewById(R.id.txt_show);
        edt_tty = (EditText)findViewById(R.id.edt_tty);
        edt_command = (EditText)findViewById(R.id.edt_command);
        btn_start_tty = (Button)findViewById(R.id.btn_start_ttys);
        btn_send_command = (Button)findViewById(R.id.btn_send_command);
        btn_send_hex_command = (Button)findViewById(R.id.btn_send_hex_command);
        
        btn_start_tty.setOnClickListener(mListener);
        btn_send_command.setOnClickListener(mListener);
        btn_send_hex_command.setOnClickListener(mListener);
        btn_send_command.setEnabled(false);
        btn_send_hex_command.setEnabled(false);
        
        
        try{
        	 fd = uartJni.openUartChannel("/dev/ttyUSB0");
        	 
        }catch (Exception e) {
			// TODO: handle exception
        	Log.d(TAG,"openUartChannel failed");
        	e.printStackTrace();
		}  
                 
    }
    
    private View.OnClickListener mListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (v.getId() == btn_start_tty.getId()) {
				
				String tty = edt_tty.getText().toString();
				  try{
			        	 fd = uartJni.openUartChannel("/dev/" + tty);
			        	 
			        }catch (Exception e) {
						// TODO: handle exception
			        	Log.d(TAG,"openUartChannel failed");
			        	
			        	e.printStackTrace();
					}  
				  
				  if(fd == -1){  
			        	Log.d("uartJniTest", "open failed");
			        	Toast.makeText(mContext, "open " + tty + " failed!", Toast.LENGTH_SHORT).show();
			        }else{
			        	Log.d(TAG,"openUartChannel ok");
			        	uartJni.uartSetSpeed(fd, 9600);
			        	uartJni.uartSetParity(fd, 8, 1, 'N');
			        	 
			        	mUartReadThread.start();
			        	
			        	 btn_send_command.setEnabled(true);
			        	  btn_send_hex_command.setEnabled(true);
			        }

				
				
			} else if (v.getId() == btn_send_command.getId()) {
				
				String command = edt_command.getText().toString();
				if (command == "" || command == null) {
					Toast.makeText(mContext, "command null", Toast.LENGTH_SHORT).show();
				}
				
				byte[] buf = command.getBytes();
    			uartJni.uartWriteBytes(fd, buf, command.length());
				
			} else if (v.getId() == btn_send_hex_command.getId()) {
				uartJni.uartWriteBytes(fd, hex, hex.length);
			}
			
		}
	};
	
	 
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
		if (fd != -1) {
			uartJni.closeUartChannel(fd);
			fd = -1;
		}
		finish();
	}

	private final static int MSG_SHOW = 1;
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			int what = msg.what;
			
			if (what == MSG_SHOW) {
				byte[] buf = (byte[]) msg.obj;
				StringBuilder sb = new StringBuilder("show:");
				for (int i = 0; i < buf.length; i++) {
					int hex = (int) buf[i]& 0x00ff;
					
					sb.append(Integer.toHexString(hex) + ", ");
				}
				sb.append("\n");
				txt_show.setText(sb);
			}
			
		};
	};
    
    private Thread mUartReadThread = new Thread(new Runnable(){

		@Override  
		public void run() {  
			// TODO Auto-generated method stub
			while(true)
            {   
                try   
                {   
                    Thread.sleep(500);
                    byte[] buf = new byte[1024];
                    int len;
                	 len = uartJni.uartReadBytes(fd, buf, 1024);
                	 
//        		 	 String string = new String(buf,0,len);
//                	 Log.d("uartJniTest","get data:"+ string);
                	
                	 if (len <= 0)
                		 continue;
                	 
                	 byte[] realBuf = new byte[len];
                	 System.arraycopy(buf, 0, realBuf, 0, len);
                	 
                	 Message msg = mHandler.obtainMessage(MSG_SHOW);
                	 msg.obj = realBuf ;
                	 mHandler.sendMessage(msg);
                	  
                } catch (InterruptedException e)      
                {   
                    // TODO Auto-generated catch block     
                    e.printStackTrace();   
                }                                               
            }   
		}   
    }
    );
    
    static 
	 {
    	try{
    		System.loadLibrary("uartJni");
    		Log.d("uartJniTest","ok to load .so");
    	}catch (Exception e) {
			// TODO: handle exception
    		Log.d("uartJniTest","cant load .so");
    		e.printStackTrace();
		}
	        
	  }
}