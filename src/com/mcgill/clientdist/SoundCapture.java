package com.mcgill.clientdist;

import java.util.ArrayList;
import java.util.List;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;

import com.mcgill.clientdist.ClientDistActivity;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

public class SoundCapture implements Runnable {
	private Handler handler; //handler to send data to the UI
	private ClientDistActivity activity; //object to connect to the main activity
	private static final int frequencyBar = 24;
	private boolean sentTime = false;
	private boolean listen = false;
	
	 //audio definition
	private static final int sampleRate = 44100,
			  channelConfig = AudioFormat.CHANNEL_IN_MONO, 
			  audioFormat = AudioFormat.ENCODING_PCM_16BIT,
			  fftPoints = 64, //fft points
			  bufferSizeBytes = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
	private int windowSize = 8;
	final int shortBufferSize = ((audioFormat == AudioFormat.ENCODING_PCM_16BIT) ? bufferSizeBytes/2 : bufferSizeBytes);
	final double sampleTimeMs = (1000/sampleRate);
    
	private FloatFFT_1D fft = new FloatFFT_1D(fftPoints); //object to perform the fft
    
    private AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSizeBytes);
    private boolean recording = false;

    private long counter = 0;
    private int bufferCounter = 0;
	
    private int bufferSize = shortBufferSize*10;
	short buffer[] = new short[bufferSize];
	private KryoClient client;
    
    public SoundCapture(ClientDistActivity parent, KryoClient client) {
    	activity = parent;
        handler = new Handler();
        this.client = client;
    }
    
    public void setClient(KryoClient client) {
    	this.client = client;
    }
    
	@Override
	public void run() {
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		start();
		long lastTime = 0;
		
		while (recording && recorder != null) {
			short tempBuffer[] = new short[shortBufferSize];
			System.out.println(System.currentTimeMillis() - lastTime);
			lastTime = System.currentTimeMillis();
			recorder.read(tempBuffer,0,shortBufferSize);
			if (listen) {
				client.shakeHands();
				for (short i: tempBuffer) {
					if (bufferCounter < bufferSize) {
						buffer[bufferCounter] = i;
						bufferCounter++;
					}
					else {
						listen = false;
						new Thread(new Runnable() {
							public void run () {
								fft(buffer);
							}
						}).start();
						break;
					}
				}
			}
		}
	}
	
	public void stop() {
		if (recorder == null)
			return;
		System.out.println("Stopping Capturer");
		recording = false;
		recorder.stop();
		recorder.release();
		recorder = null;
	}
	
	public void pause() {
		if (recorder == null)
			return;
		recorder.stop();
		recording = false;
	}
	
	public void start() {
		System.out.println("Starting Capturer");
		if (recorder == null) {
			recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSizeBytes);
		}
		recording = true;
		recorder.startRecording();
	}
	
	private void postToActivity(final long time) {
		if (!sentTime) {
			System.out.println("Amount of Calculations: " + time);
			handler.post(new Runnable() {
				public void run() {
					activity.receivedSoundSignal(time);
				}
			});
			sentTime = true;
			listen = false;
		}
    }
	
	public void prepareToReceive(long timems) {
		counter = 0;
		bufferCounter = 0;
		listen = true;
		sentTime = false;
	}
	
	public void setWindowSize(int value) {
		this.windowSize = value;
	}
	
	public void fft(short[] buffer) {
		System.out.println("Starting FFT!");
		int j = 0;
		int len = buffer.length;
		for (j = 0; j < len; j += windowSize) {
			int end = j+fftPoints;
			if (end > len)
				break;

		     //buffer to store the audio
			float bufferFloat[] = new float[fftPoints * 2], //buffer to pass the audio for the fft
				  magnitude[] = new float[fftPoints]; //final array, with the magnitudes/frequency
			
			//prepare the float buffer to be passed to the FFT operation
			for (int i = j, k = 0; i < end; i++, k+=2) {
				bufferFloat[k] = (float) buffer[i]; //real part
				bufferFloat[k+1] = 0;				//imaginary part
			}

			//perform the FFT
			fft.complexForward(bufferFloat);

			//convert the FFT values from Im/Real to an absolute value
			for (int i = 0, k = 0; i < fftPoints; i++, k+=2) {
				magnitude[i] = (float) Math.sqrt(bufferFloat[k]*bufferFloat[k] + bufferFloat[k+1]*bufferFloat[k+1]);
			}

			if (
					magnitude[frequencyBar] > 4*magnitude[frequencyBar-3] &&
					magnitude[frequencyBar] > 4*magnitude[frequencyBar-2] &&
					magnitude[frequencyBar] > 4*magnitude[frequencyBar-1] &&
					magnitude[frequencyBar] > 4*magnitude[frequencyBar+1] &&
					magnitude[frequencyBar] > 4*magnitude[frequencyBar+2] &&
					magnitude[frequencyBar] > 4*magnitude[frequencyBar+3]
				)
			{
				System.out.println("Found Signal");
				postToActivity(counter);
				return;
			}
			else {
				counter++;
			}
		}
		System.out.println("Signal not found.");
	}
}
