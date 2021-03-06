/*
 * Copyright (c) 2011 Tal Shalif
 * 
 * This file is part of Talos-Rowing.
 * 
 * Talos-Rowing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Talos-Rowing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Talos-Rowing.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.nargila.robostroke.acceleration;

import org.nargila.robostroke.ParamKeys;
import org.nargila.robostroke.RoboStroke;
import org.nargila.robostroke.common.filter.LowpassFilter;
import org.nargila.robostroke.data.DataIdx;
import org.nargila.robostroke.data.SensorDataFilter;
import org.nargila.robostroke.param.Parameter;
import org.nargila.robostroke.param.ParameterChangeListener;
import org.nargila.robostroke.param.ParameterListenerOwner;
import org.nargila.robostroke.param.ParameterListenerRegistration;
import org.nargila.robostroke.param.ParameterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Joins gravity-filtered 3 axis sensor data into acceleration amplitude value
 * @author tshalif
 *
 */
public class AccelerationFilter extends SensorDataFilter implements ParameterListenerOwner {
	
	private final static int ROWER_MODE = 1;
	private final static int COAX_MODE = -1;
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	protected int accelMode = ROWER_MODE;
	
	private final ParameterService params;
	
	private final LowpassFilter zeroY = new LowpassFilter(0.005f);
	private final LowpassFilter zeroZ = new LowpassFilter(0.005f);

	/**
	 * calculate horizontal acceleration amplitude according to device pitch
	 * @param values accelerometer sensor data
	 * @return acceleration amplitude
	 */
	private float calcAcceleration(final float[] values) {
		
		float y = values[DataIdx.ACCEL_Y];
		float z = values[DataIdx.ACCEL_Z];
		
		final double ay = y - zeroY.filter(new float[]{y})[0];
		final double az = z - zeroZ.filter(new float[]{z})[0];
		
		final double accelOrDecelDeterminer = Math.abs(ay) > Math.abs(az) ? -ay : az; // if device is exactly flat or vertical, one axis has to be ignored
		
		final int accelDir = accelMode * accelOrDecelDeterminer < 0 ? -1 : 1;
		
		final float a = (float)(accelDir * Math.sqrt(ay*ay + az*az));
				
		return a;
	}
	public AccelerationFilter(RoboStroke owner) {
		this.params = owner.getParameters();
		
		accelMode = (Boolean)params.getValue(ParamKeys.PARAM_SENSOR_ORIENTATION_REVERSED.getId()) ? COAX_MODE : ROWER_MODE;

		params.addListeners(this);

	}
	
	@Override
	protected final Object filterData(long timestamp, Object value) {
		float[] values = (float[]) value;
		return new float[] {
				calcAcceleration(values)
		};
	}
	

	private final ParameterListenerRegistration[] listenerRegistrations = {
			new ParameterListenerRegistration(ParamKeys.PARAM_SENSOR_ORIENTATION_REVERSED.getId(), new ParameterChangeListener() {
				
				@Override
				public void onParameterChanged(Parameter param) {
					boolean coaxMode = (Boolean)param.getValue();
					logger.info("setting coax mode to {}", coaxMode);
					accelMode = coaxMode ?  COAX_MODE : ROWER_MODE;
				}
			})
	};
	
	@Override
	public ParameterListenerRegistration[] getListenerRegistrations() {
		return listenerRegistrations;
	}

	@Override
	protected void finalize() throws Throwable {
		params.removeListeners(this);
		super.finalize();
	}
}

