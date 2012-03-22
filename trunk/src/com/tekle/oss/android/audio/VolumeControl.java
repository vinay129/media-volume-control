/**
 * Copyright (c) 2012 Ephraim Tekle genzeb@gmail.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and 
 * associated documentation files (the "Software"), to deal in the Software without restriction, including 
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the 
 * following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial 
 * portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN 
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 *  @author Ephraim A. Tekle
 *
 */

package com.tekle.oss.android.audio;

import java.util.ArrayList;
import java.util.List;   

import android.app.Activity; 
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler; 

/**
 * This class provides a singleton object for controlling and monitoring the system volume. In particular, 
 * this class provides for the "smooth" control of the volume level (unlike the five stage volume level 
 * provided by the {@link AudioManager} and device buttons).
 * <p /> 
 * To use this class, acquire the singleton instance via the static method {@link #sharedVolumeControl()}. 
 * To receive updates when volume changed (for example, by the user using the volume up/down buttons), 
 * start the volume monitor using {@link #startVolumeMonitor()} then register a listener using 
 * {@link #addVolumeChangeListener(VolumeChangeListener)}. Update callbacks are done on the UI (i.e. main) 
 * thread and therefore are safe to update UI elements within the interface method implementation. 
 * <p />
 * <em>NOTE: the {@link VolumeControl} singleton objects needs to be configured using the 
 * {@link #configure(Activity, MediaPlayer)} method prior to calling any other method on the object. 
 * It is also advised the volume level monitor be started (using {@link #startVolumeMonitor()}) when 
 * the singleton object is configured.</em>
 * 
 * @author Ephraim A. Tekle
 *
 */
public class VolumeControl {
	//private static final String TAG = Constants.TAG + ".VOLUME_CONTROL"; 
	
	private static VolumeControl sharedVolumeControl = null;
	
	/**
	 * Obtain the singleton {@link VolumeControl} object.
	 * @return the singleton {@link VolumeControl} object
	 */
	public static synchronized VolumeControl sharedVolumeControl() {
		if (sharedVolumeControl == null) {
			sharedVolumeControl = new VolumeControl();
		}
		
		return sharedVolumeControl;
	}
	
	/**
	 * The {@code VolumeChangeIndicator} enumerates the volume level change indicators that can be used 
	 * when programmatically changing the volume level (using {@link VolumeControl#setVolume(float)}).
	 * 
	 * @author Ephraim A. Tekle
	 *
	 */
	public static enum VolumeChangeIndicator {
		/**
		 * Play a sound when changing the volume
		 * @see #SHOW_DIALOG
		 */
		PLAY_SOUND,
		/**
		 * Show a (progress bar) dialog when changing the volume
		 * @see #PLAY_SOUND
		 */
		SHOW_DIALOG,
		/**
		 * Play a sound and show a dialog when changing the volume
		 * @see #PLAY_SOUND
		 * @see #SHOW_DIALOG
		 */
		PLAY_SOUND_AND_SHOW_DIALOG,
		/**
		 * Do not show any volume level change indicator
		 */
		NONE;
		
		int getFlag() {
			switch(this) {
			case PLAY_SOUND:
				return AudioManager.FLAG_PLAY_SOUND;
			case SHOW_DIALOG:
				return AudioManager.FLAG_SHOW_UI;
			case PLAY_SOUND_AND_SHOW_DIALOG:
				return PLAY_SOUND.getFlag() | SHOW_DIALOG.getFlag();
			default:
				return AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE;
			}
		}
	}

	private VolumeChangeIndicator volumeChangeIndicator = VolumeChangeIndicator.SHOW_DIALOG;
	private final static float GRANULARITY = 100;  
	static final int VOLUME_MONITOR_RATE_MS = 1000;
	//static final int VOLUME_MONITOR_RATE_HIGH_MS = 100; // sampling rate when volume change is detected
	
	private static float SYSTEM_MAX_VOLUME;
	
	private float playerVolume = 1; 
	
	private AudioManager audioManager = null;
	private MediaPlayer mediaPlayer = null;
	private Activity activity = null;
	
	private boolean inLowVolumeMode = false;
	

	private final Handler handler = new Handler(); 
	private final List<VolumeChangeListener> volumeChangeListeners = new ArrayList<VolumeChangeListener>();
	private volatile float monitoredVolume;
	private volatile boolean stopVolumeMonitor;
	
	private VolumeControl() { 
	}
	
	/**
	 * Configures the {@link VolumeControl} object with the Audio Service system service and {@link AudioManager}.
	 * 
	 * @param activity the Activity that will be used to retrieve the {@link AudioManager} and execute listener call backs on the main thread
	 * @param mediaPlayer the {@link MediaPlayer} being used to play audio/video. While the {@code VolumeControl} will adjust system volumes, it's excepted that this class is being used within the context of a MediaPlayer.
	 * @return returns {@code true} if configuration is successful. Returns {@code false} otherwise.
	 */
	public boolean configure(Activity activity, MediaPlayer mediaPlayer) {
		
		if (activity == null || mediaPlayer == null) {
			return false;
		}
		
		this.audioManager = (AudioManager) activity.getSystemService(Activity.AUDIO_SERVICE);
		this.mediaPlayer = mediaPlayer;
		this.activity = activity;
		
		SYSTEM_MAX_VOLUME = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); 
		
		return true;
	}
	
	/**
	 * Returns {@code true} if the {@code VolumeControl} is configured properly. Otherwise, {@code false} is returned.
	 * @return {@code true} if this {@code VolumeControl} is configured properly and can be used.
	 */
	public boolean isConfigured() {
		return (this.audioManager != null && this.mediaPlayer != null && this.activity != null);
	}
	
	/**
	 * Sets the volume using {@code AudioManager} and the {@code MediaPlayer} (use {@link #setVolumeChangeIndicator(VolumeChangeIndicator)} to change the volume change indicator).
	 * 
	 * @param volume the volume level between 0 (mute) and 1 (maximum volume).
	 * @see #setVolumeChangeIndicator(VolumeChangeIndicator)
	 */
	public void setVolume(float volume) {

		this.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (Math.ceil(SYSTEM_MAX_VOLUME*volume)), volumeChangeIndicator.getFlag());
		
		float systemVolume = this.getSystemVolume();
		
		if (Math.abs(systemVolume-volume)*GRANULARITY >= 1) { 
			
			this.playerVolume = volume/systemVolume;
			
			this.mediaPlayer.setVolume(this.playerVolume,this.playerVolume);  
		}
	} 
	
	/**
	 * Get the current volume level (using {@code AudioManager} and the {@code MediaPlayer})
	 * @return the volume level
	 */
	public float getVolume() {
		return this.getSystemVolume()*this.playerVolume;
	}
	
	/**
	 * Use this method to enter a low-volume mode. This is intended to be used when volume {@link AudioManager#AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK} is detected.
	 */
	public synchronized void enterLowVolumeMode() {
		if (this.playerVolume > 0.1f) {
			this.mediaPlayer.setVolume(0.1f,0.1f);
			this.inLowVolumeMode = true;
		} 
	}
	
	/**
	 * Use this method to exit a low-volume mode and set volume to pre audio-focus loss. This is intended to be used when volume {@link AudioManager#AUDIOFOCUS_GAIN} is detected.
	 */
	public synchronized void exitLowVolumeMode() {
		if (this.inLowVolumeMode) {
			this.mediaPlayer.setVolume(this.playerVolume,this.playerVolume);
			this.inLowVolumeMode = false;
		}
	} 
	
	private float getSystemVolume() {
		return this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)/SYSTEM_MAX_VOLUME; 
	} 
	
	/**
	 * Adds a volume change listener. The listener's {@code VolumeChanged} method is called immediately on the UI thread.
	 * @param l the {@link VolumeChangeListener} to be added
	 */
	public synchronized void addVolumeChangeListener(final VolumeChangeListener l) {
		this.volumeChangeListeners.add(l);
		
		this.activity.runOnUiThread(new Runnable() { 
			@Override
			public void run() { 
				l.volumeChanged(getVolume());
			}
		});
	}
	
	/**
	 * Removes a volume change listener
	 * @param l the volume change listener to remove
	 */
	public synchronized void removeVolumeChangeListener(VolumeChangeListener l) {
		this.volumeChangeListeners.remove(l);
	}
	
	/**
	 * Removes all volume change listeners. This method can be used as a cleanup when the main Activity exits. 
	 */
	public void removeAllVolumeChangeListeners() {
		this.volumeChangeListeners.clear();
	}
	
	/**
	 * Starts the volume monitor so that {@link VolumeChangeListener}s will get notification if the volume is changed (for example, by the user using the volume up/down buttons).
	 */
	public void startVolumeMonitor() { 
		stopVolumeMonitor = false;
		this.monitoredVolume = this.getVolume();
		this.primaryVolumeUpdater() ;
	}
	
	/**
	 * Stops volume monitoring so that no volume change updates are sent to listeners. 
	 */
	public void stopVolumeMonitor() {  
		stopVolumeMonitor = true;
	}
	
	private void notifyVolumeListenersOnMainThread(final float volume) {
		this.activity.runOnUiThread(new Runnable() { 
			@Override
			public void run() {   
				for (VolumeChangeListener l : VolumeControl.this.volumeChangeListeners) {
					l.volumeChanged(volume);
				}
			}
		});
	}
	
	private void primaryVolumeUpdater() {
		if (this.stopVolumeMonitor) {
			return;
		} 
		
		float volumeNow = this.getVolume();
		int samplingRate = VOLUME_MONITOR_RATE_MS;
		
		if (Math.abs(volumeNow-this.monitoredVolume)*GRANULARITY >= 1) {
			this.notifyVolumeListenersOnMainThread(volumeNow);
			//samplingRate = VOLUME_MONITOR_RATE_HIGH_MS;
			// sampling rate made no difference since we are bound by the UI Thread
		}
		
		this.monitoredVolume = volumeNow;
		
		handler.postDelayed(new Runnable() {
			public void run() {
				primaryVolumeUpdater();
			}
		}, samplingRate);
	}
	
	/**
	 * Set the volume change indicator used when volume is changed using  {@link #setVolume(float)}.
	 * @param indicator the desired volume change indicator
	 * @see #getVolumeChangeIndicator()
	 */
	public void setVolumeChangeIndicator(VolumeChangeIndicator indicator) {
		this.volumeChangeIndicator = indicator;
	}
	
	/**
	 * Returns the volume change indicator used when volume is changed using  {@link #setVolume(float)}.
	 * @return the volume change indicator
	 * @see #setVolumeChangeIndicator(VolumeChangeIndicator)
	 */
	public VolumeChangeIndicator getVolumeChangeIndicator() {
		return this.volumeChangeIndicator;
	}
	 
	/**
	 * Interface for receiving notification when the system volume has changed (eg when user changes volume using the device volume buttons). Update calls are done on the UI (i.e. main) thread and therefore are safe to update UI elements within the interface method implementation.
	 * 
	 * @author Ephraim A. Tekle
	 *
	 */
	public static interface VolumeChangeListener {
		public void volumeChanged(float volume);
	}
}
