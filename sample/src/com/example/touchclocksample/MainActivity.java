package com.example.touchclocksample;

import de.markusfisch.android.library.touchclock.TouchClockView;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class MainActivity
	extends Activity
	implements TouchClockView.OnTimeUpdateListener
{
	private static final String STATE_HOUR = "hour";
	private static final String STATE_MINUTE = "minute";
	private static final String STATE_DURATION = "duration";

	private TouchClockView touchClockView;
	private TextView timeView;

	@Override
	public void onCreate( Bundle state )
	{
		super.onCreate( state );

		requestWindowFeature( Window.FEATURE_NO_TITLE );
		setContentView( R.layout.main_activity );

		touchClockView = (TouchClockView)findViewById( R.id.touchclock );
		timeView = (TextView)findViewById( R.id.digitalclock );

		touchClockView.setOnTimeUpdateListener( this );

		if( state != null )
		{
			touchClockView.setTime(
				state.getInt( STATE_HOUR, 2 ),
				state.getInt( STATE_MINUTE, 50 ) );

			touchClockView.setDuration(
				state.getInt( STATE_DURATION, 120 ) );
		}
		else
			onTimeUpdate(
				touchClockView.getHour(),
				touchClockView.getMinute(),
				touchClockView.getDuration() );
	}

	@Override
	public void onSaveInstanceState( Bundle state )
	{
		if( state != null )
		{
			state.putInt( STATE_HOUR, touchClockView.getHour() );
			state.putInt( STATE_MINUTE, touchClockView.getMinute() );
			state.putInt( STATE_DURATION, touchClockView.getDuration() );
		}

		super.onSaveInstanceState( state );
	}

	@Override
	public void onTimeUpdate( int hour, int minute, int duration )
	{
		int toHour = hour+duration/60;
		int toMinute = minute+duration%60;

		if( toMinute > 60 )
		{
			toHour += toMinute/60;
			toMinute %= 60;
		}

		toHour %= 24;

		timeView.setText( String.format(
			"%02d:%02d - %02d:%02d",
			hour,
			minute,
			toHour,
			toMinute ) );
	}
}
