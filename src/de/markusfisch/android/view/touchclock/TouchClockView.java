package de.markusfisch.android.view.touchclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;

public class TouchClockView extends View
{
	public interface OnTimeUpdateListener
	{
		public void onTimeUpdate( int hour, int minute, int duration );
	}

	static private enum HandType
	{
		NOHAND,
		HOUR,
		MINUTE,
		STOP
	}

	private static final float TAU = (float)Math.PI*2f;
	private static final float HALF_PI = (float)Math.PI/2f;
	private static final float RAD_MINUTE = TAU/60f;
	private static final float MINUTE_RAD = 60f/TAU;
	private static final float HOUR_RAD = 12f/TAU;
	private static final float RAD_TO_DEGREE = 180f/(float)Math.PI;
	private static final float MPH_PER_RAD = 720f/TAU;
	private static final float RAD_PER_MPH = TAU/720f;
	private static final float RAD_PER_5MPH = TAU/144f;

	public boolean useDuration = true;
	public int markColor = 0xff0099cc;
	public int hourColor = 0xff0099cc;
	public int minuteColor = 0xff33b5e5;
	public int stopColor = 0xffaa66cc;

	protected Paint handPaint = new Paint( Paint.ANTI_ALIAS_FLAG );
	protected Paint durationPaint = new Paint( Paint.ANTI_ALIAS_FLAG );
	protected Hand hour = new Hand();
	protected Hand minute = new Hand();
	protected Hand stop = new Hand();

	private final RectF stopRect = new RectF();
	private HandType handType = HandType.NOHAND;
	private Bitmap clockFace = null;
	private float touchRadius;
	private float centerX;
	private float centerY;
	private float hourLastAngle;
	private float minuteLastAngle;
	private int hourValue;
	private int minuteValue;
	private boolean am = true;
	private OnTimeUpdateListener onTimeUpdateListener;

	public TouchClockView( Context context )
	{
		super( context );
		init();
	}

	public TouchClockView( Context context, AttributeSet attrs )
	{
		super( context, attrs );
		init();
	}

	public TouchClockView( Context context, AttributeSet attrs, int defStyle )
	{
		super( context, attrs, defStyle );
		init();
	}

	public void setOnTimeUpdateListener( OnTimeUpdateListener listener )
	{
		onTimeUpdateListener = listener;
	}

	public void setTime( int h, int m )
	{
		if( h > 12 )
			am = false;

		hourValue = h;
		minuteValue = m;

		setAm( hourValue );

		hour.angle = getHourAngle( hourValue, minuteValue );
		hourLastAngle = getClockAngle( hour.angle );
		minute.angle = getMinuteAngle( minuteValue );
		minuteLastAngle = getClockAngle( minute.angle );

		update();
	}

	public void setDuration( int minutes )
	{
		int h = hourValue;
		int m = minuteValue;

		h += minutes/60;
		m += minutes%60;

		stop.angle = getHourAngle( h, m );

		update();
	}

	public int getHour()
	{
		return hourValue;
	}

	public int getMinute()
	{
		return minuteValue;
	}

	public int getDuration()
	{
		return Math.round(
			MPH_PER_RAD*
			getAngleDifference( stop.angle-hour.angle ) );
	}

	@Override
	public void onSizeChanged( int w, int h, int oldw, int oldh )
	{
		super.onSizeChanged( w, h, oldw, oldh );

		final float radius = Math.min( w, h )*.5f;
		final float dp = getContext()
			.getResources()
			.getDisplayMetrics()
			.density;

		final float cell = radius*.6f/3f;
		touchRadius = dp*24f;
		hour.length = radius*.4f;

		if( cell < touchRadius*2f )
		{
			stop.length = hour.length+cell;
			minute.length = stop.length+cell;
		}
		else
		{
			stop.length = radius*.6f;
			minute.length = radius*.8f;
		}

		centerX = w/2;
		centerY = h/2;

		clockFace = getClockFace(
			w,
			h,
			centerX,
			centerY,
			radius,
			dp*2f,
			markColor );

		hour.color = hourColor;
		minute.color = minuteColor;
		stop.color = stopColor;
		stop.handleOnly = true;

		handPaint.setStyle( Paint.Style.STROKE );
		handPaint.setStrokeWidth( dp*2f );

		durationPaint.setStyle( Paint.Style.FILL );
		durationPaint.setColor( 0x44000000 | (stop.color & 0xffffff) );

		stopRect.left = centerX-stop.length;
		stopRect.top = centerY-stop.length;
		stopRect.right = centerX+stop.length;
		stopRect.bottom = centerY+stop.length;
	}

	@Override
	public void onDraw( Canvas c )
	{
		super.onDraw( c );

		if( clockFace == null )
			return;

		c.drawBitmap( clockFace, 0, 0, null );

		if( useDuration )
		{
			final float a = hour.angle*RAD_TO_DEGREE;
			float d = getAngleDifference(
				stop.angle-hour.angle );

			d *= RAD_TO_DEGREE;

			c.drawArc(
				stopRect,
				a,
				d,
				true,
				durationPaint );

			handPaint.setStyle( Paint.Style.STROKE );
			handPaint.setColor( stop.color );
			c.drawArc(
				stopRect,
				a,
				d,
				false,
				handPaint );
		}

		hour.draw( c );
		minute.draw( c );

		if( useDuration )
			stop.draw( c );

		handPaint.setStyle( Paint.Style.FILL );
		handPaint.setColor( markColor );
		c.drawCircle(
			centerX,
			centerY,
			touchRadius*.3f,
			handPaint );
	}

	@Override
	public boolean onTouchEvent( MotionEvent motionEvent )
	{
		final float x = motionEvent.getX();
		final float y = motionEvent.getY();
		float angle = 0;
		float duration = 0;

		if( handType != HandType.NOHAND )
		{
			angle = (float)Math.atan2( y-centerY, x-centerX );
			duration = getAngleDifference( stop.angle-hour.angle );
		}

		switch( (motionEvent.getAction() & MotionEvent.ACTION_MASK) )
		{
			case MotionEvent.ACTION_DOWN:
				if( inRadius( x, y, hour.x, hour.y, touchRadius ) )
					handType = HandType.HOUR;
				else if( inRadius( x, y, minute.x, minute.y, touchRadius ) )
					handType = HandType.MINUTE;
				else if( inRadius( x, y, stop.x, stop.y, touchRadius ) )
					handType = HandType.STOP;
				break;
			case MotionEvent.ACTION_MOVE:
				switch( handType )
				{
					case HOUR:
						hour.angle = angle;
						stop.angle = getAngleDifference(
							hour.angle+duration );

						// determine if am or pm
						{
							final float c = getClockAngle( hour.angle );
							float a = c-hourLastAngle;
							final float r = Math.abs( a );

							if( TAU-r < r )
								a = -a;

							if( (a > 0 && hourLastAngle > c) ||
								(a < 0 && hourLastAngle < c) )
								am ^= true;

							hourLastAngle = c;
						}

						hourValue = (Math.round(
								HOUR_RAD*
								getClockAngle( hour.angle ) )+
								(am ? 0 : 12)
							)%24;

						update();
						break;
					case MINUTE:
						minute.angle = angle;

						// determine if hour needs modification
						{
							final float c = getClockAngle( minute.angle );
							float a = c-minuteLastAngle;
							final float r = Math.abs( a );

							if( TAU-r < r )
								a = -a;

							if( a > 0 && minuteLastAngle > c )
								setAm( (hourValue = getWrappedHour(
									++hourValue )) );
							else if( a < 0 && minuteLastAngle < c )
								setAm( (hourValue = getWrappedHour(
									--hourValue )) );

							minuteLastAngle = c;
						}

						minuteValue = (int)Math.floor(
								MINUTE_RAD*
								getClockAngle( minute.angle )
							)%60;

						hour.angle = getHourAngle(
							hourValue,
							minuteValue );
						stop.angle = getAngleDifference(
							hour.angle+duration );

						update();
						break;
					case STOP:
						stop.angle = Math.round(
							angle/RAD_PER_5MPH )*RAD_PER_5MPH;

						update();
						break;
				}
				break;
			case MotionEvent.ACTION_UP:
				switch( handType )
				{
					case HOUR:
					case MINUTE:
						hour.angle = getHourAngle(
							hourValue,
							minuteValue );
						minute.angle = getMinuteAngle(
							minuteValue );
						stop.angle = getAngleDifference(
							hour.angle+duration );

						update();
						break;
				}

				handType = HandType.NOHAND;
				break;
		}

		return true;
	}

	protected static Bitmap getClockFace(
		int width,
		int height,
		float cx,
		float cy,
		float radius,
		float strokeWidth,
		int color )
	{
		final Bitmap bitmap;

		java.lang.System.gc();

		if( (bitmap = Bitmap.createBitmap(
			width,
			height,
			Bitmap.Config.ARGB_8888 )) == null )
			return null;

		final Canvas c = new Canvas( bitmap );
		final Paint p = new Paint( Paint.ANTI_ALIAS_FLAG );

		p.setStyle( Paint.Style.STROKE );
		p.setStrokeWidth( strokeWidth );
		p.setColor( color );

		float r1 = radius*.9f;
		float r2 = radius*.95f;
		float a = 0;

		for( int n = 0;
			a < TAU;
			a += RAD_MINUTE, ++n )
		{
			final float cos = (float)Math.cos( a );
			final float sin = (float)Math.sin( a );
			final float r;

			if( n % 5 > 0 )
				r = r2;
			else
				r = r1;

			c.drawLine(
				cx+r*cos,
				cy+r*sin,
				cx+radius*cos,
				cy+radius*sin,
				p );
		}

		return bitmap;
	}

	protected void init()
	{
		Calendar c = Calendar.getInstance();

		setTime(
			c.get( Calendar.HOUR_OF_DAY ),
			c.get( Calendar.MINUTE ) );

		setDuration( 120 );
	}

	protected void update()
	{
		if( onTimeUpdateListener != null )
			onTimeUpdateListener.onTimeUpdate(
				hourValue,
				minuteValue,
				getDuration() );

		invalidate();
	}

	private static boolean inRadius(
		final float tx,
		final float ty,
		final float cx,
		final float cy,
		final float r )
	{
		final float dx = cx-tx;
		final float dy = cy-ty;

		return (Math.sqrt( dx*dx+dy*dy ) < r);
	}

	private void setAm( final int h )
	{
		am = (h <= 11);
	}

	private static float getHourAngle( final int h, final int m )
	{
		return -HALF_PI+RAD_PER_MPH*((h > 12 ? h-12 : h)*60+m);
	}

	private static float getMinuteAngle( final int m )
	{
		return -HALF_PI+RAD_MINUTE*m;
	}

	private static float getClockAngle( final float a )
	{
		return getAngleDifference( a+HALF_PI );
	}

	private static float getAngleDifference( float a )
	{
		while( a < 0 )
			a += TAU;

		while( a > TAU )
			a -= TAU;

		return a;
	}

	private static int getWrappedHour( int h )
	{
		while( h < 0 )
			h += 24;

		while( h > 24 )
			h -= 24;

		return h%24;
	}

	protected class Hand
	{
		public float x;
		public float y;
		public float angle;
		public float length;
		public int color;
		public boolean handleOnly = false;

		public void draw( Canvas c )
		{
			x = centerX+length*(float)Math.cos( angle );
			y = centerY+length*(float)Math.sin( angle );

			handPaint.setColor( color );

			if( !handleOnly )
			{
				handPaint.setStyle( Paint.Style.STROKE );
				c.drawLine(
					centerX,
					centerY,
					x,
					y,
					handPaint );
			}

			handPaint.setStyle( Paint.Style.FILL );
			c.drawCircle(
				x,
				y,
				touchRadius*.3f,
				handPaint );

			handPaint.setColor( 0x88000000 | (color & 0xffffff) );
			c.drawCircle(
				x,
				y,
				touchRadius,
				handPaint );
		}
	}
}
