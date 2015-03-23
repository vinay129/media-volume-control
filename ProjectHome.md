## Android Volume Control and Monitor ##
### Effortless continuous audio level control and monitor for your Android app ###
Android's `AudioManager` allows incremental volume up/down control (typically 5 stages!). Whereas the `MediaPlayer` allows for smooth volume control, but maxed at the current system volume setting (so if the system volume is set to zero, `MediaPlayer`'s `setVolume(float,float)` is basically useless.

Furthermore, `AudioManager` does not provide functionality for registering a delegate to be notified if/when the system volume changes.

Media-Volume-Control supports:
  1. Smooth adjustment of the system volume for a media from minium system volume (0) to the maximum system volume continuously.
  1. Methods to adjust volume level or mute during an interrupt -- such as when audio focus is lost (eg. incoming call while playing music)
  1. System volume monitor for registering a listener that will be notified if the volume changes (for example by the user using the volume button on the device)

| <img src='http://media-volume-control.googlecode.com/files/volume.png' width='300px' /> | <img src='http://media-volume-control.googlecode.com/files/com.arifsoft.arifzefen.volume_control.png' width='300px' /> |
|:----------------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------|
| [Sample app](https://code.google.com/p/media-volume-control/source/checkout) using the [media-volume-control](#.md) library | [ArifZefen App](https://market.android.com/details?id=com.arifsoft.arifzefen) using the [media-volume-control](#.md) library|

## How to Install ##
Get a copy of the source [here](https://code.google.com/p/media-volume-control/source/checkout). Copy-paste the /src/com folder to your project's source folder.
In your main `Activity` (or any `Activity`), import the classes and configure the `VolumeControl` as follows:
```

import com.tekle.oss.android.audio.VolumeControl;
import com.tekle.oss.android.audio.VolumeControl.VolumeChangeListener;
...
VolumeControl.sharedVolumeControl().configure(this, this.mediaPlayer);
```
Now that the `VolumeControl` singleton instance is configured, feel free to explore around. To change the volume, do:
```
VolumeControl.sharedVolumeControl().setVolume(newVolumeLevelInFloats);
```
More importantly, to get notification when the volume level changes, add a listener and start volume monitor as follows:
```
VolumeControl.sharedVolumeControl().addVolumeChangeListener(new VolumeChangeListener() {
	@Override
	public void volumeChanged(float volume) {
		// It's safe to update the UI here
	}
});
VolumeControl.sharedVolumeControl().startVolumeMonitor();
```

Optionally, you can set what volume level change indicator you want to use by calling `VolumeControl.setVolumeChangeIndicator`.
## Disclaimer ##
You may use, modify, etc. this code. However, give credit per the license agreement.