package org.hibernate.search.bridge.builtin;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.hibernate.AssertionFailure;

public class DateResolutionUtil {
	
	private DateResolutionUtil() {}
	
	
	public static Resolution getLuceneResolution(org.hibernate.search.annotations.Resolution hibResolution) {
		Resolution resolution = null;
		switch (hibResolution) {
			case YEAR:
				resolution = DateTools.Resolution.YEAR;
				break;
			case MONTH:
				resolution = DateTools.Resolution.MONTH;
				break;
			case DAY:
				resolution = DateTools.Resolution.DAY;
				break;
			case HOUR:
				resolution = DateTools.Resolution.HOUR;
				break;
			case MINUTE:
				resolution = DateTools.Resolution.MINUTE;
				break;
			case SECOND:
				resolution = DateTools.Resolution.SECOND;
				break;
			case MILLISECOND:
				resolution = DateTools.Resolution.MILLISECOND;
				break;
			default:
				throw new AssertionFailure( "Unknown Resolution: " + hibResolution );
		}
		return resolution;
	}
}
