package com.mcgill.clientdist;

import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class ClientDistActivity extends Activity {
	private int state = Network.DISCONNECTED;
	private boolean receivedSound = false;
	private long valueToSend = 0;
	private SeekBar seekBar;
	private TextView seekBarValue;
	
	SoundCapture capturer;
	Thread capturerThread;
	
	EditText mEdit;
	Button bEdit;
	KryoClient client;
	
	TelephonyManager tm;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        configureIPField();
        capturer = new SoundCapture(this, client);
		
		seekBar = (SeekBar) findViewById(R.id.windowSizeBar);
		seekBarValue = (TextView) findViewById(R.id.seekBarValue);
		seekBarValue.setText("Window Size: 1");
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				capturer.setWindowSize(seekBar.getProgress()+1);
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				seekBarValue.setText("Window Size: " + (progress + 1));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
		});
		startCapturer();
    }
    
    public void configureIPField() {
        mEdit = (EditText)findViewById(R.id.ipAddress);
        mEdit.setText(Network.defaultIP);
        bEdit = (Button)findViewById(R.id.connectButton);
        tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
        	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        		if (end > start) {
        			String destText = dest.toString();
        			String resultingText = destText.substring(0, dstart) + source.subSequence(start, end) + destText.substring(dend);
        			if (!resultingText.matches ("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) { 
                        return "";
        			}
                    else {
                    	String[] splits = resultingText.split("\\.");
                    	for (int i  = 0; i < splits.length; i++) {
                    		if (Integer.valueOf(splits[i]) > 255) {
                    			return "";
                    		}
                    	}
                    }
        		}
        		return null;
        	}
        };
        mEdit.setFilters(filters);
    }
    
    private void setEditText(final boolean bool) {
    	if (bool) {
    		mEdit.setEnabled(true);
    	}
    	else {
    		mEdit.setEnabled(false);
    		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    		imm.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);
    	}
    }
    
    private void changeUI(final int value) {
    	switch (value) {
    	case Network.DISCONNECTED:
    		state = Network.DISCONNECTED;
    		setEditText(true);
    		bEdit.setText(R.string.connect);

//    		stopCapturer();
    		break;
    	case Network.CONNECTED: 
    		state = Network.CONNECTED;
    		setEditText(false);
    		bEdit.setText(R.string.disconnect);
    		
//    		startCapturer();
    		break;
    	case Network.CONNECTING: 
    		state = Network.CONNECTING;
    		setEditText(false);
    		bEdit.setText(R.string.connecting);
    		break;
    	}
    }
    
    public void connectButton(View view) {
    	switch (state) {
    	case Network.DISCONNECTED: 
    		changeUI(Network.CONNECTING);
        	
        	client = new KryoClient(mEdit.getText().toString(), this);
        	capturer.setClient(client);
        	new Thread(new Runnable() {
        		public void run() {
                	try {
            			client.connect();
            		} catch (UnknownHostException e) {
            			//Log.e("Error", "Couldn't get a IP Address from the address given.");
            		}
        		}
        	}).start();
        	
    		break;
    	case Network.CONNECTING:
    		break;
    	case Network.CONNECTED:
    		state = Network.DISCONNECTED;
    		stopClient();
    		break;
    	}
    }
    
    public void receiveConnectionStatus(final int status) {
    	changeUI(status);
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		
		stopClient();
		stopCapturer();
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopClient();
		stopCapturer();
	}
	
	protected void stopClient() {
		if (client != null)
			client.close();
	}
	
	public void stopCapturer() {
		if (capturer != null) {
			capturer.stop();
			if (capturerThread != null) {
				capturerThread.interrupt();
				capturerThread = null;
			}
		}
	}
	
	public void startCapturer() {
		capturerThread = new Thread(capturer);
		capturerThread.start();
	}
    
    public void clickButton(View view) {
    	if (capturerThread != null) {
    		System.out.println(capturerThread.getState().toString());
    	}
    	else {
    		System.out.println("null");
    	}
    }

	public void waitForData() {
		receivedSound = false;
    	new Thread(new Runnable() {
    		public void run() {
    			capturer.prepareToReceive(System.currentTimeMillis());
    			try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

    			//client.sendResponse("" + valueToSend, receivedSound);
    		}
    	}).start();
    }
    
    public void receivedSoundSignal(final long time) {
    	receivedSound = true;
    	valueToSend = time;
    }
}
