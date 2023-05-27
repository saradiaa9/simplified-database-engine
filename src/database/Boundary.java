package database;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Boundary implements Serializable {

	Object xMin, yMin, zMin, xMax, yMax, zMax;

	public Boundary(Object xMin, Object yMin, Object zMin, Object xMax, Object yMax, Object zMax) {
		// super();
		/*
		 * Storing two diagonal points
		 */
		this.xMin = xMin;
		this.yMin = yMin;
		this.zMin = zMin;
		this.xMax = xMax;
		this.yMax = yMax;
		this.zMax = zMax;
	}

	public Object getxMin() {
		return xMin;
	}

	public Object getyMin() {
		return yMin;
	}

	public Object getzMin() {
		return zMin;
	}

	public Object getxMax() {
		return xMax;
	}

	public Object getyMax() {
		return yMax;
	}

	public Object getzMax() {
		return zMax;
	}

	public boolean inRange(Object x, Object y, Object z) {
//		System.out.println(
//				inRangeColumn(x, xMin, xMax) + " " + inRangeColumn(y, yMin, yMax) + " " + inRangeColumn(z, zMin, zMax));
		return (inRangeColumn(x, xMin, xMax) && inRangeColumn(y, yMin, yMax) && inRangeColumn(z, zMin, zMax));
	}

	public boolean inRangeColumn(Object column, Object min, Object max) {

		switch (column.getClass().getTypeName()) {
		case "java.lang.String":
			String columnS = (String) column;
			columnS=columnS.toLowerCase();
			String maxlower=(String) max; 
			String minlower= (String)min;
			
			if ((columnS.compareTo(maxlower.toLowerCase()) <= 0 && (columnS.compareTo(minlower.toLowerCase())) >= 0)) {
				return true;
			} else {
				return false;
			}

		case "java.lang.Integer":

			int columnI = (int) column;
			if (columnI >= (int) min && columnI <= (int) max) {
				return true;
			} else {
				return false;
			}

		case "java.lang.Double":

			Double columnDo = (Double) column;
			Double minD = (Double) min;
			Double maxD = (Double) max;
			if (columnDo >= minD && columnDo <= maxD) {
				return true;
			} else {
				return false;
			}

		case "java.util.Date":
			Date columnD = (Date) column;
			if (((columnD.after((Date) min)) && (columnD.before((Date) max)))
					|| (columnD.equals((Date) min) || columnD.equals((Date) max))) {
				return true;
			} else {
				return false;
			}
		case "database.Null":
			// do later
		}
		return false;
	}

}
