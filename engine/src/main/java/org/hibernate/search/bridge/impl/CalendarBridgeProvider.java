/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.lang.reflect.AnnotatedElement;
import java.util.Calendar;

import org.hibernate.search.annotations.CalendarBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.NumericEncodingCalendarBridge;
import org.hibernate.search.bridge.builtin.StringEncodingCalendarBridge;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Built-in {@link org.hibernate.search.bridge.spi.BridgeProvider} handling calendar bridging when
 * {@code @CalendarBridge} is involved.
 * As built-in provider, no Service Loader file is used: the {@code BridgeFactory} does access it
 * after the custom bridge providers found.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
class CalendarBridgeProvider extends ExtendedBridgeProvider {
	private static final Log LOG = LoggerFactory.make();

	@Override
	public FieldBridge provideFieldBridge(ExtendedBridgeProviderContext context) {
		AnnotatedElement annotatedElement = context.getAnnotatedElement();
		if ( Calendar.class.isAssignableFrom( context.getReturnType() ) ) {
			if ( annotatedElement.isAnnotationPresent( org.hibernate.search.annotations.CalendarBridge.class ) ) {
				CalendarBridge dateBridgeAnnotation = annotatedElement.getAnnotation(
						org.hibernate.search.annotations.CalendarBridge.class
				);

				Resolution resolution = dateBridgeAnnotation.resolution();
				EncodingType encodingType = dateBridgeAnnotation.encoding();
				return getDateFieldBridge( resolution, encodingType );
			}
			else {
				return getDateFieldBridge( Resolution.MILLISECOND, EncodingType.NUMERIC );
			}
		}
		return null;
	}

	private FieldBridge getDateFieldBridge(Resolution resolution, EncodingType encodingType) {
		switch ( encodingType ) {
			case NUMERIC: {
				switch ( resolution ) {
					case YEAR:
						return NumericEncodingCalendarBridge.DATE_YEAR;
					case MONTH:
						return NumericEncodingCalendarBridge.DATE_MONTH;
					case DAY:
						return NumericEncodingCalendarBridge.DATE_DAY;
					case HOUR:
						return NumericEncodingCalendarBridge.DATE_HOUR;
					case MINUTE:
						return NumericEncodingCalendarBridge.DATE_MINUTE;
					case SECOND:
						return NumericEncodingCalendarBridge.DATE_SECOND;
					case MILLISECOND:
						return NumericEncodingCalendarBridge.DATE_MILLISECOND;
					default:
						throw LOG.unknownResolution( resolution.toString() );
				}
			}
			case STRING: {
				switch ( resolution ) {
					case YEAR:
						return StringEncodingCalendarBridge.DATE_YEAR;
					case MONTH:
						return StringEncodingCalendarBridge.DATE_MONTH;
					case DAY:
						return StringEncodingCalendarBridge.DATE_DAY;
					case HOUR:
						return StringEncodingCalendarBridge.DATE_HOUR;
					case MINUTE:
						return StringEncodingCalendarBridge.DATE_MINUTE;
					case SECOND:
						return StringEncodingCalendarBridge.DATE_SECOND;
					case MILLISECOND:
						return StringEncodingCalendarBridge.DATE_MILLISECOND;
					default:
						throw LOG.unknownResolution( resolution.toString() );
				}
			}
			default: {
				throw LOG.unknownEncodingType( encodingType.name() );
			}
		}
	}
}
