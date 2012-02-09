
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

package volume;
  
import com.tekle.oss.android.audio.R;
import com.tekle.oss.android.audio.VolumeControl;
import com.tekle.oss.android.audio.VolumeControl.VolumeChangeListener;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer; 
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class VolumeControlActivity extends Activity  implements OnCompletionListener, OnBufferingUpdateListener, OnPreparedListener, OnErrorListener, VolumeChangeListener  {
    /** Called when the activity is first created. */
	
	Button playPauseButton;
	View progressView;
	SeekBar volumeSeekBar;
	TextView progressTextView;
	
	static enum StateEnum {PAUSED, STOPPED};
	
	StateEnum state = StateEnum.STOPPED;
	
	MediaPlayer mediaPlayer;
	
	boolean volumeSeekBarBeingAdjusted = false;
	
	// Let's play the US National Anthem ... or replace by any valid URL to an audio file
	static final String URL = "http://bands.army.mil/music/play.asp?TheStarSpangledBanner_BandAndChorus.mp3";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        volumeSeekBar = (SeekBar)this.findViewById(R.id.volumeSeekBar);
		playPauseButton = (Button)this.findViewById(R.id.playPauseButton); 
		progressView = this.findViewById(R.id.progressLayout);
		progressTextView = (TextView)this.findViewById(R.id.progressTextView);

	    setVolumeControlStream(AudioManager.STREAM_MUSIC);
	    
		mediaPlayer = new MediaPlayer();

		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC); 
		mediaPlayer.setOnBufferingUpdateListener(this);
		
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnErrorListener(this); 

		// The following three lines are all that is needed to use the VolumeControl class
		VolumeControl.sharedVolumeControl().configure(this, this.mediaPlayer);
		VolumeControl.sharedVolumeControl().addVolumeChangeListener(this);
		VolumeControl.sharedVolumeControl().startVolumeMonitor(); // this needed iff a  callback is desired when the system volume is changed (to update a volume level SeekBar for example)
		
		this.playPauseButton.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View v) {  
				if (mediaPlayer.isPlaying()) {
					pause();
				} else {
					play();
				}
			}
		});
		
		this.volumeSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					VolumeControl.sharedVolumeControl().setVolume((float) ((float)progress/100.));
				}
			}

			public void onStartTrackingTouch(SeekBar seekBar) { 
				volumeSeekBarBeingAdjusted = true;
			} 
			public void onStopTrackingTouch(SeekBar seekBar) {
				volumeSeekBarBeingAdjusted = false;
			}
		});
		

		this.setPausedLayout(); 

    }
    
    void setPlayingLayout() {

		this.playPauseButton.setBackgroundResource(R.layout.pause_button);
		this.playPauseButton.setVisibility(View.VISIBLE);
		this.progressView.setVisibility(View.GONE); 
    }
    
    void setPausedLayout() {

		this.playPauseButton.setBackgroundResource(R.layout.play_button);
		this.playPauseButton.setVisibility(View.VISIBLE);
		this.progressView.setVisibility(View.GONE); 
    }
    
    void setLoadingLayout() {

		this.progressTextView.setText("Buffering...");
		this.playPauseButton.setVisibility(View.GONE);
		this.progressView.setVisibility(View.VISIBLE); 
    	
    }
    
    void play() { 
    	
    	if (this.state == StateEnum.PAUSED) {
    		this.mediaPlayer.start(); 
    		this.setPlayingLayout();
    		
    	} else if (this.state == StateEnum.STOPPED) {
    		try {
				mediaPlayer.setDataSource(URL);				
	    		mediaPlayer.prepareAsync();	 

	    		this.setLoadingLayout();
	    		
			} catch (Exception e) {  
				e.printStackTrace();
			}  
    	} 
    }
    
    void pause() {
    	if (this.mediaPlayer.isPlaying()) {
    		
    		this.state = StateEnum.PAUSED;
        	this.mediaPlayer.pause();

    		this.setPausedLayout();
    	}
    }

	@Override
	public void onCompletion(MediaPlayer mp) {
		this.state = StateEnum.STOPPED;

		this.setPausedLayout();
		
		this.play();
	} 

	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.start();

		this.setPlayingLayout();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) { 
		return false; // let's forward this to the completion handler
	}

	@Override
	public void volumeChanged(float volume) {
		if (!volumeSeekBarBeingAdjusted) {
			volumeSeekBar.setProgress((int) (volume*100));
		} 
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		this.progressTextView.setText("Buffering "+percent+"%");
	}
}