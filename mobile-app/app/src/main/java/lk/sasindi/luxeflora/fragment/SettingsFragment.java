package lk.sasindi.luxeflora.fragment;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

import lk.sasindi.luxeflora.R;

public class SettingsFragment extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, light, proximity;

    private TextView textAccelerometer, textLight, textProximity;

    private long lastShakeTime = 0;
    private Vibrator vibrator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);

        textAccelerometer = view.findViewById(R.id.textAccelerometer);
        textLight = view.findViewById(R.id.textLight);
        textProximity = view.findViewById(R.id.textProximity);

        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        if (accelerometer == null) textAccelerometer.setText("Accelerometer not available");
        if (light == null) textLight.setText("Light sensor not available");
        if (proximity == null) textProximity.setText("Proximity sensor not available");

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sensorManager != null) {
            if (accelerometer != null)
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

            if (light != null)
                sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);

            if (proximity != null)
                sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (getView() == null) return;

        switch (event.sensor.getType()) {

            case Sensor.TYPE_ACCELEROMETER:
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                float shake = Math.abs(x + y + z);

                textAccelerometer.setText("Shake intensity: " + shake);

                long currentTime = System.currentTimeMillis();
                if (shake > 25 && (currentTime - lastShakeTime > 1500)) {
                    lastShakeTime = currentTime;

                    // Vibrate the phone
                    long vibrationDuration = 300;
                    if (vibrator != null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(vibrationDuration);
                        }
                    }

                    Toast.makeText(getContext(), "Surprise Flowers", Toast.LENGTH_SHORT).show();
                }
                break;

            case Sensor.TYPE_LIGHT:
                float lightValue = event.values[0];
                textLight.setText("Light: " + lightValue);

                // Optional UI effect
                if (lightValue < 10) {
                    getView().setAlpha(0.8f);
                } else {
                    getView().setAlpha(1f);
                }
                break;

            case Sensor.TYPE_PROXIMITY:
                float proximityValue = event.values[0];
                textProximity.setText("Proximity: " + proximityValue);

                if (proximityValue == 0) {
                    Toast.makeText(getContext(), "Hand detected!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}