package de.tubs.ibr.dtn.ruralexplorer.data;

import de.tubs.ibr.dtn.ruralexplorer.backend.NodeAdapter;
import android.content.Context;
import android.database.Cursor;

public class SensorData {
	public static final String PRESSURE = "pressure";
	public static final String TEMPERATURE = "temperature";
	
	private Float mPressure = null;
	private Float mTemperature = null;
	
	public SensorData() {
	}
	
	public SensorData(Context context, Cursor cursor, NodeAdapter.ColumnsMap cmap) {
		mPressure = cursor.getFloat(cmap.mColumnSensorPressure);
		mTemperature = cursor.getFloat(cmap.mColumnSensorTemperature);
	}

	public Float getPressure() {
		return mPressure;
	}

	public void setPressure(Float pressure) {
		mPressure = pressure;
	}
	
	public boolean hasPressure() {
		return mPressure != null;
	}

	public Float getTemperature() {
		return mTemperature;
	}

	public void setTemperature(Float temperature) {
		mTemperature = temperature;
	}
	
	public boolean hasTemperature() {
		return mTemperature != null;
	}
}