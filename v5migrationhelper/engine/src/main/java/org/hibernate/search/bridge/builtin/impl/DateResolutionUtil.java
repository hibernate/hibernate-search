/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.impl;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.hibernate.search.exception.AssertionFailure;

public class DateResolutionUtil {

	private DateResolutionUtil() {
	}

	public static Resolution getLuceneResolution(org.hibernate.search.annotations.Resolution hibResolution) {
		final Resolution resolution;
		switch ( hibResolution ) {
			case YEAR:
				resolution = Resolution.YEAR;
				break;
			case MONTH:
				resolution = Resolution.MONTH;
				break;
			case DAY:
				resolution = Resolution.DAY;
				break;
			case HOUR:
				resolution = Resolution.HOUR;
				break;
			case MINUTE:
				resolution = Resolution.MINUTE;
				break;
			case SECOND:
				resolution = Resolution.SECOND;
				break;
			case MILLISECOND:
				resolution = Resolution.MILLISECOND;
				break;
			default:
				throw new AssertionFailure( "Unknown Resolution: " + hibResolution );
		}
		return resolution;
	}
}
