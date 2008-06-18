/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanDataPointsSorterByIntensity;

public class ExactMassDetector implements MassDetector {

	
	// parameter values
	private float noiseLevel;
	private int resolution;
	private String peakModelname;

	private DataPoint scanDataPoints[];
	private Vector<MzPeak> mzPeaks;
	
	private TreeMap<Float, DataPoint[]> dataPointsMap;

	private PeakModel peakModel;

	// Desktop
	private Desktop desktop = MZmineCore.getDesktop();

	public ExactMassDetector(ExactMassDetectorParameters parameters) {
		noiseLevel = (Float) parameters
				.getParameterValue(ExactMassDetectorParameters.noiseLevel);
		resolution = (Integer) parameters
				.getParameterValue(ExactMassDetectorParameters.resolution);
		peakModelname = (String) parameters
				.getParameterValue(ExactMassDetectorParameters.peakModel);
		dataPointsMap = new TreeMap<Float, DataPoint[]>();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector#getMassValues(net.sf.mzmine.data.Scan)
	 */
	public MzPeak[] getMassValues(Scan scan) {
		scanDataPoints = scan.getDataPoints();
		mzPeaks = new Vector<MzPeak>();
		
		Vector<DataPoint> localMaximum = new Vector<DataPoint>();
		Vector<DataPoint> rangeDataPoints = new Vector<DataPoint>();
		boolean ascending = true;

		// Iterate through all data points
		for (int i = 0; i < scanDataPoints.length - 1; i++) {

			boolean nextIsBigger = scanDataPoints[i + 1].getIntensity() > scanDataPoints[i]
					.getIntensity();
			boolean nextIsZero = scanDataPoints[i + 1].getIntensity() == 0;
			boolean currentIsZero = scanDataPoints[i].getIntensity() == 0;

			// Ignore zero intensity regions
			if (currentIsZero){
				continue;
			}

			// Add current (non-zero) data point to the current m/z peak
			rangeDataPoints.add(scanDataPoints[i]);

			// Check for local maximum
			if (ascending && (!nextIsBigger)) {
				localMaximum.add(scanDataPoints[i]);
				rangeDataPoints.remove(scanDataPoints[i]);
				ascending = false;
				continue;
			}

			// Check for the end of the peak
			if ((!ascending) && (nextIsBigger || nextIsZero)) {

				// Add the m/z peak if it is above the noise level
				if (localMaximum.lastElement().getIntensity() > noiseLevel) {
					DataPoint[] rawDataPoints = rangeDataPoints.toArray(new DataPoint[0]);
					dataPointsMap.put(localMaximum.lastElement().getMZ(), rawDataPoints);
					
				}
				else {
					int index = localMaximum.size()-1;
					localMaximum.remove(index);
				}
					

				// Reset and start with new peak
				ascending = true;
				rangeDataPoints.clear();
			}

		}
		
		definePeaks(localMaximum);
		//removeLateralPeaks();
		return mzPeaks.toArray(new MzPeak[0]);
	}

	/**
	 * 
	 * This function calculates the mass (m/z) giving weight to each data point
	 * of the peak using all local maximum and minimum. Also applies a filter
	 * for peaks with intensity below of noise level parameter.
	 * 
	 * @param localMaximum
	 * @param localMinimum
	 * @param start
	 * @param end
	 * @return
	 */
	private void definePeaks(Vector<DataPoint> localMaximum){

		DataPoint[] maximumPeaks = localMaximum.toArray(new DataPoint[0]);
		Arrays.sort(maximumPeaks, new ScanDataPointsSorterByIntensity());
		
		while (maximumPeaks.length > 0){
			
			DataPoint[] rawDataPoints = dataPointsMap.get(maximumPeaks[0].getMZ());
			float exactMz = calculateExactMass(rawDataPoints, maximumPeaks[0].getMZ(), maximumPeaks[0].getIntensity());
			mzPeaks.add(new MzPeak(new SimpleDataPoint(
					exactMz, maximumPeaks[0].getIntensity()), rawDataPoints));
			if (maximumPeaks.length == 1)
				break;
			maximumPeaks = removeLateralPeaks(exactMz, maximumPeaks[0].getIntensity(), maximumPeaks);
			
		}
	}

	private float calculateExactMass(DataPoint[] rangeDataPoints,
			float mz, float intensity) {

		float leftX1 = -1, leftY1 = -1, leftY2 = -1, leftX2 = -1, rightX1 = -1, rightY1 = -1, rightX2 = -1, rightY2 = -1;

		for (int i = 0; i < rangeDataPoints.length; i++) {
			if ((rangeDataPoints[i].getIntensity() <= intensity / 2)
					&& (rangeDataPoints[i].getMZ() < mz)
					&& (i + 1 < rangeDataPoints.length)) {
				leftY1 = rangeDataPoints[i].getIntensity();
				leftX1 = rangeDataPoints[i].getMZ();
				leftY2 = rangeDataPoints[i+1].getIntensity();
				leftX2 = rangeDataPoints[i+1].getMZ();
			}
			if ((rangeDataPoints[i].getIntensity() >= intensity / 2)
					&& (rangeDataPoints[i].getMZ() > mz)
					&& (i + 1 < rangeDataPoints.length)) {
				rightY1 = rangeDataPoints[i].getIntensity();
				rightX1 = rangeDataPoints[i].getMZ();
				rightY2 = rangeDataPoints[i+1].getIntensity();
				rightX2 = rangeDataPoints[i+1].getMZ();
			}
		}

		if ((leftY1 == -1) || (rightY1 == -1))
			return mz;
		
		float mLeft = (leftY1 - leftY2) / (leftX1 - leftX2);
		float mRight = (rightY1 - rightY2) / (rightX1 - rightX2);

		float xLeft = leftX1 + (((intensity / 2) - leftY1) / mLeft);
		float xRight = rightX1 + (((intensity / 2) - rightY1) / mRight);

		float FWHM = xRight - xLeft;

		float exactMass = xLeft + FWHM / 2;

		return exactMass;
	}

	/**
	 * This function calculates the base peak width with a fixed mass resolution
	 * (percentageResolution). After eliminates the encountered lateral peaks in
	 * this range, with a height value less than defined height percentage of
	 * central peak (percentageHeight).
	 * 
	 * @param mzPeaks
	 * @param percentageHeight
	 * @param percentageResolution
	 */
	private DataPoint[] removeLateralPeaks(float mz, float intensity, DataPoint[] maximumPeaks) {

		Constructor peakModelConstruct;
		Class peakModelClass;
		int peakModelindex = 0;
		Vector<DataPoint> newMaximumPeaks = new Vector<DataPoint>();
		for (String model : ExactMassDetectorParameters.peakModelNames) {
			if (model.equals(peakModelname))
				break;
			peakModelindex++;
		}

		String peakModelClassName = ExactMassDetectorParameters.peakModelClasses[peakModelindex];

		try {
			peakModelClass = Class.forName(peakModelClassName);
			peakModelConstruct = peakModelClass.getConstructors()[0];
			peakModel = (PeakModel) peakModelConstruct.newInstance();

		} catch (Exception e) {
			desktop
					.displayErrorMessage("Error trying to make an instance of peak model "
							+ peakModelClassName);
			return null;
		}

		peakModel.setParameters(mz, intensity, resolution);
		Range rangePeak = peakModel.getWidth(intensity * 0.001f);

		// We strat in the second position of array because the first one is our current main peak
		for (int i=1; i<maximumPeaks.length; i++) {

			if ((maximumPeaks[i].getMZ() >= rangePeak.getMin())
					&& (maximumPeaks[i].getMZ() <= rangePeak.getMax())
					&& (maximumPeaks[i].getIntensity() < peakModel
							.getIntensity(maximumPeaks[i].getMZ()))) {
				continue;
			}
			
			newMaximumPeaks.add(maximumPeaks[i]);
		}
		
		DataPoint[] sortedMaximumPeaks = newMaximumPeaks.toArray(new DataPoint[0]);
		Arrays.sort(sortedMaximumPeaks, new ScanDataPointsSorterByIntensity());
		
		return sortedMaximumPeaks;

	}
}
