package ch.zhaw.bait17.audio_signal_processing_toolbox.visualisation;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ch.zhaw.bait17.audio_signal_processing_toolbox.R;
import ch.zhaw.bait17.audio_signal_processing_toolbox.util.ApplicationContext;

/**
 * @author georgrem, stockan1
 */
public class LineSpectrumView extends FrequencyView {

    private static final String TAG = LineSpectrumView.class.getSimpleName();
    private static final int dB_RANGE = 140;
    private static final int db_PEAK = 50;
    private final Map<VisualisationType, float[]> MAGNITUDES = new HashMap<>();

    private Paint strokePaint;
    private int width, height;
    private float[] spectrumPoints;

    public LineSpectrumView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public LineSpectrumView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public LineSpectrumView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //drawSpectrumLineShape(canvas);
    }

    private void drawSpectrumCurveShape(float[] amplitudes, int ZERO_DEZ_REF) {
        spectrumPoints = new float[amplitudes.length * 4];
        int pointIndex = 0;
        float lastX = -1;
        float lastY = -1;

        /* For efficiency, we don't draw all of the samples in the buffer, but only the ones
           that align with pixel boundaries. */
        for (int x = 0; x < width; x++) {
            int index = (int) (((x * 1.0f) / width) * amplitudes.length);
            float amplitude = amplitudes[index];
            float y = ZERO_DEZ_REF - amplitude;

            if (lastX != -1) {
                spectrumPoints[pointIndex++] = lastX;
                spectrumPoints[pointIndex++] = lastY;
                spectrumPoints[pointIndex++] = x;
                spectrumPoints[pointIndex++] = y;
            }
            lastX = x;
            lastY = y;
        }
    }

    /*
    private void drawSpectrumLineShape(Canvas canvas) {
        float[] mag = new float[magnitudes.length];
        System.arraycopy(magnitudes, 0, mag, 0, magnitudes.length);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);

        float[] dBMag = new float[mag.length];
        float dBMax = Float.MIN_VALUE;
        for (int i = 0; i < mag.length; i++) {
            dBMag[i] = (float) (10 * Math.log10(mag[i]));
            if (dBMag[i] > dBMax) {
                dBMax = dBMag[i];
            }
        }
        float offset = dBMax;
        Log.d(TAG, "max magnitude: " + dBMax);

        for (int x = 0; x < width; x++) {
            int index = (int) (((x * 1.0f) / width) * mag.length / 2);

            //canvas.drawLine(x, downy, strokePaint);

            float y = db_PEAK - dBMag[index];


            c.drawPoint(x, y, strokePaint);
        }

        canvas.drawBitmap(bitmap, 0, 0, null);
    }
    */

    /**
     * Returns the value from its relative position within given boundaries.
     *
     * @param position
     * @param minValue
     * @param maxValue
     * @return
     */
    private float getValueFromRelativePosition(float position, float minValue, float maxValue) {
        return minValue + position * (maxValue - minValue);
    }

    private void drawSpectrumBarShape(float[] amplitudes, int ZERO_DEZ_REF, Canvas canvas) {
        /* For efficiency, we don't draw all of the samples in the buffer, but only the ones
           that align with pixel boundaries. */
        int scaledWidt = width / 5;
        for (int x = 0; x < scaledWidt; x++) {
            int index = (int) (((x * 1.0f) / scaledWidt) * amplitudes.length);
            float sample = amplitudes[index];
            float downy = ZERO_DEZ_REF - (sample);
            int upy = ZERO_DEZ_REF;
            canvas.scale(1, 1);
            canvas.drawRect(x * 5, downy, x * 5 + 5, upy, strokePaint);
        }
    }

    /**
     * Sets the resolution of the FFT. Sometimes called the FFT windows size.
     * The input value is usually a power of 2.
     * For good results the window size should be in the range [2^11, 2^15].
     * The input value should not exceed 2^15.
     *
     * @param fftResolution     power of 2 in the range [2^11, 2^15]
     */
    @Override
    public void setFFTResolution(int fftResolution) {
        //magnitudes = new float[fftResolution];
    }

    /**
     * Sets the spectral density to be displayed in the {@code FrequencyView}.
     * hMag represents the power spectrum of a time series.
     * Use this method if you want to display the power spectrum before and after the effect chain
     * in the same view.
     *
     * @param preFilterMagnitude    array of {@code float} representing magnitudes of unfiltered data
     *                              (power spectrum of a time series)
     * @param postFilterMagnitude   array of {@code float} representing magnitudes of filtered data
     *                              (power spectrum of a time series)
     */
    @Override
    public void setSpectralDensity(@NonNull float[] preFilterMagnitude,
                                   @NonNull float[] postFilterMagnitude) {
        if (preFilterMagnitude.length > 0) {
            MAGNITUDES.put(VisualisationType.PRE_FX,
                    Arrays.copyOf(preFilterMagnitude, preFilterMagnitude.length));
        }
        if (postFilterMagnitude.length > 0) {
            MAGNITUDES.put(VisualisationType.POST_FX,
                    Arrays.copyOf(postFilterMagnitude, postFilterMagnitude.length));
        }
        postInvalidate();
    }

    @Override
    public AudioView getInflatedView() {
        return (AudioView) View.inflate(ApplicationContext.getAppContext(),
                R.layout.line_spectrum_view, null);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        setWillNotDraw(false);
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.LineSpectrumView, defStyle, 0);

        float strokeThickness = a.getFloat(R.styleable.LineSpectrumView_lineSpectrumStrokeThickness, 5f);
        int strokeColor = a.getColor(R.styleable.LineSpectrumView_lineSpectrumColor,
                ContextCompat.getColor(context, R.color.line_spectrum));

        a.recycle();

        strokePaint = new Paint();
        strokePaint.setColor(strokeColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeThickness);
        strokePaint.setAntiAlias(false);
    }
}
